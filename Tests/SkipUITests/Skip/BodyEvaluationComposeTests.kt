// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import skip.model.StateTracker
import skip.model.StateTracking
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class BodyEvaluationComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun unchangedParentInputsSkipWhileAnExternalSnapshotStillProducesFreshRenderables() {
        val parent = mutableStateOf(0)
        val snapshot = mutableStateOf(0)
        val saver = ComposeStateSaver()
        var factories = 0
        var evaluations = 0
        rule.setContent {
            Column {
                androidx.compose.material3.Text("Parent ${parent.value}")
                val context = ComposeContext(modifier = Modifier.testTag("ephemeral-${parent.value}"),
                    composer = Composer { _, _ -> error("Incoming collector must not escape normalization") },
                    stateSaver = saver, scope = Any())
                val renderables = evaluateImmutableViewBody(context, 0, kotlin.arrayOf(42)) {
                    factories += 1
                    ComposeBuilder { child ->
                        evaluations += 1
                        Text("Snapshot ${snapshot.value}").Compose(child)
                    }
                }
                for (renderable in renderables) renderable.Render(ComposeContext(stateSaver = saver))
            }
        }
        rule.onNodeWithText("Snapshot 0").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, factories); assertEquals(1, evaluations); parent.value += 1 }
        rule.onNodeWithText("Parent 1").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, factories); assertEquals(1, evaluations); snapshot.value = 7 }
        rule.onNodeWithText("Snapshot 7").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, factories); assertEquals(2, evaluations); parent.value += 1 }
        rule.onNodeWithText("Parent 2").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, factories); assertEquals(2, evaluations) }
    }

    @Test fun changedInputsOptionsAndStateSaverEachReplaceTheBuilder() {
        val input = mutableStateOf(1)
        val options = mutableStateOf(0)
        val saver = mutableStateOf(ComposeStateSaver(), referentialEqualityPolicy())
        var factories = 0
        rule.setContent {
            val selected = input.value
            val selectedOptions = options.value
            val context = ComposeContext(stateSaver = saver.value)
            val renderables = evaluateImmutableViewBody(context, selectedOptions, kotlin.arrayOf(selected)) {
                factories += 1
                ComposeBuilder { child -> Text("Input $selected options $selectedOptions").Compose(child) }
            }
            for (renderable in renderables) renderable.Render(context)
        }
        rule.onNodeWithText("Input 1 options 0").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, factories); input.value = 2 }
        rule.onNodeWithText("Input 2 options 0").assertIsDisplayed()
        rule.runOnIdle { assertEquals(2, factories); options.value = 1 }
        rule.onNodeWithText("Input 2 options 1").assertIsDisplayed()
        rule.runOnIdle { assertEquals(3, factories); saver.value = ComposeStateSaver() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(4, factories) }
    }

    @Test fun nestedInitialCompositionFailureBalancesItsBodyTracking() = assertAbandonedTracking(false)
    @Test fun nestedLaterRecompositionFailureBalancesFreshBodyTrackingCandidates() = assertAbandonedTracking(true)

    @OptIn(InternalComposeApi::class)
    private fun assertAbandonedTracking(afterSuccess: Boolean) {
        rule.runOnUiThread {
            assertTrue("Body tracking proof must execute on the actual main looper", android.os.Looper.myLooper() === android.os.Looper.getMainLooper())
            val recomposer = Recomposer(Dispatchers.Main.immediate)
            val applier = object : AbstractApplier<Unit>(Unit) {
                override fun insertTopDown(index: Int, instance: Unit) { error("No UI nodes expected") }
                override fun insertBottomUp(index: Int, instance: Unit) { error("No UI nodes expected") }
                override fun remove(index: Int, count: Int) { }
                override fun move(from: Int, to: Int, count: Int) { }
                override fun onClear() { }
            }
            val composition = ControlledComposition(applier, recomposer)
            val shouldThrow = mutableStateOf(!afterSuccess)
            var factories = 0
            val saver = ComposeStateSaver()
            val content: @Composable () -> Unit = {
                evaluateImmutableViewBody(ComposeContext(stateSaver = saver), 0, kotlin.arrayOf(1)) {
                    factories += 1
                    ComposeBuilder { outer ->
                        val outerFailure = shouldThrow.value
                        evaluateImmutableViewBody(outer, 0, kotlin.arrayOf(2)) {
                            factories += 1
                            ComposeBuilder {
                                if (shouldThrow.value) throw IllegalStateException("Abandoned nested body probe")
                                ComposeResult.ok
                            }
                        }
                        check(!outerFailure) { "Nested body must throw before the outer completes" }
                        ComposeResult.ok
                    }
                }
            }
            try {
                if (afterSuccess) {
                    // Recomposer performs the real initial snapshot/read-observer setup.
                    composition.setContent(content)
                    assertEquals(2, factories)
                    shouldThrow.value = true
                    composition.recordModificationsOf(setOf(shouldThrow))
                }
                var failure: IllegalStateException? = null
                try {
                    if (afterSuccess) {
                        Snapshot.observe(composition::recordReadOf, composition::recordWriteOf) { composition.recompose() }
                    } else composition.setContent(content)
                } catch (caught: IllegalStateException) { failure = caught }
                assertTrue("The actual nested composition must throw", failure?.message?.contains("Abandoned nested body probe") == true)
                assertEquals("Recomposition must reuse both existing holders/builders", 2, factories)
                assertTrue("StateTracking.register must run on the main looper", android.os.Looper.myLooper() === android.os.Looper.getMainLooper())
                var tracked = 0
                StateTracking.register(object : StateTracker { override fun trackState() { tracked += 1 } })
                assertEquals("Both nested pushes must be gone before disposal", 1, tracked)
            } finally {
                composition.dispose()
                recomposer.close()
                // Keep this test isolated even if a defect-control leaks either nested push.
                repeat(2) { StateTracking.popBody() }
            }
        }
    }

    @Test fun unprovenClosureBindingAndCustomValueInputsStayFresh() {
        rule.setContent { BodyEvaluationFallbackProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Invoke current action").performClick()
        rule.onNodeWithText("Observed 0").assertIsDisplayed()
        rule.onNodeWithText("Increment current binding").performClick()
        rule.onNodeWithText("Left 1 Right 0").assertIsDisplayed()
        rule.onNodeWithText("Change fallback inputs").performClick()
        rule.onNodeWithText("Custom input 1").assertIsDisplayed()
        rule.onNodeWithText("Invoke current action").performClick()
        rule.onNodeWithText("Observed 1").assertIsDisplayed()
        rule.onNodeWithText("Increment current binding").performClick()
        rule.onNodeWithText("Left 1 Right 1").assertIsDisplayed()
    }

    @Test fun referenceIdentityAndRawFloatingBitsRemainDistinctInputKeys() {
        val reference = mutableStateOf(EqualBodyReference("first"), referentialEqualityPolicy())
        val doubleBits = mutableStateOf(0L)
        val floatBits = mutableStateOf(0)
        val parent = mutableStateOf(0)
        val saver = ComposeStateSaver()
        var factories = 0
        rule.setContent {
            Column {
                androidx.compose.material3.Text("Parent ${parent.value}")
                val current = reference.value
                val double = Double.fromBits(doubleBits.value)
                val float = Float.fromBits(floatBits.value)
                val renderables = evaluateImmutableViewBody(ComposeContext(stateSaver = saver), 0, kotlin.arrayOf(
                    bodyInputReference(current), java.lang.Double.doubleToRawLongBits(double), java.lang.Float.floatToRawIntBits(float))) {
                    factories += 1
                    ComposeBuilder { child -> Text("Reference ${current.label}").Compose(child) }
                }
                for (renderable in renderables) renderable.Render(ComposeContext(stateSaver = saver))
            }
        }
        rule.onNodeWithText("Reference first").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, factories); parent.value += 1 }
        rule.onNodeWithText("Parent 1").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, factories); reference.value = EqualBodyReference("second") }
        rule.onNodeWithText("Reference second").assertIsDisplayed()
        rule.runOnIdle { assertEquals(2, factories); doubleBits.value = Long.MIN_VALUE }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(3, factories); doubleBits.value = 0x7ff8000000000001L }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(4, factories); doubleBits.value = 0x7ff8000000000002L }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(5, factories); floatBits.value = Int.MIN_VALUE }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(6, factories); floatBits.value = 0x7fc00001 }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(7, factories); floatBits.value = 0x7fc00002 }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(8, factories); parent.value += 1 }
        rule.onNodeWithText("Parent 2").assertIsDisplayed()
        rule.runOnIdle { assertEquals(8, factories) }
    }
}

private class EqualBodyReference(val label: String) {
    override fun equals(other: Any?): Boolean = other is EqualBodyReference
    override fun hashCode(): Int = 0
}
