// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State as ComposeState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Test

/** Guards the actual self-write/read cycle so an original-source control fails without hanging. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class GestureInvalidationComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val stoppedLoop = mutableStateOf(false)
    private val handlerWrites = AtomicInteger()
    private var observer: ObserverHandle? = null

    @After fun releaseObserver() { observer?.dispose() }

    private fun guardHandlerFeedback() {
        observer = Snapshot.registerApplyObserver { changes, _ ->
            val changedHandlers = changes.any { state ->
                val value = (state as? ComposeState<*>)?.value
                value is kotlin.collections.List<*> && value.firstOrNull() is ModifiedGesture<*>
            }
            if (changedHandlers && handlerWrites.incrementAndGet() > 32 && !stoppedLoop.value) {
                // Test-only circuit breaker: dispose the defect's tree, then fail the assertion.
                // It observes runtime state objects rather than adding a production test hook.
                Snapshot.withMutableSnapshot { stoppedLoop.value = true }
            }
        }
    }

    @Test fun stackedGesturesSettleAndRetainedDetectorsUseCurrentCallbacks() {
        guardHandlerFeedback()
        rule.setContent {
            if (!stoppedLoop.value) StackedGestureRuntimeProbe().Compose(ComposeContext())
        }
        rule.waitForIdle()
        assertFalse("Gesture composition subscribed to its own handler writes", stoppedLoop.value)
        rule.onNodeWithTag("stacked-gesture-target").performTouchInput { click() }
        rule.onNodeWithText("Handled 0").assertIsDisplayed()
        rule.onNodeWithText("Simultaneous 1").assertIsDisplayed()
        rule.onNodeWithText("Next handler").performClick()
        rule.onNodeWithTag("stacked-gesture-target").performTouchInput { click() }
        rule.onNodeWithText("Handled 1").assertIsDisplayed()
        rule.onNodeWithText("Enable double tap").performClick()
        rule.onNodeWithTag("stacked-gesture-target").performTouchInput { doubleClick() }
        rule.onNodeWithText("Double 1").assertIsDisplayed()
        assertFalse("Updating handlers restarted the feedback loop", stoppedLoop.value)
    }

    @Test fun changingMinimumDistanceRekeysTheDragDetector() {
        guardHandlerFeedback()
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                if (!stoppedLoop.value) ChangingDragRuntimeProbe().Compose(ComposeContext())
            }
        }
        fun tinyDrag() {
            rule.onNodeWithTag("changing-drag-target").performTouchInput {
                down(Offset(50f, 50f)); moveBy(Offset(2f, 0f)); up()
            }
        }
        tinyDrag()
        rule.onNodeWithText("Changes 0").assertIsDisplayed()
        rule.onNodeWithText("Ends 0").assertIsDisplayed()
        rule.onNodeWithText("Zero distance").performClick()
        tinyDrag()
        rule.onNodeWithText("Ends 1").assertIsDisplayed()
        rule.onNodeWithText("Positive distance").performClick()
        tinyDrag()
        rule.onNodeWithText("Ends 1").assertIsDisplayed()
        assertFalse("Changing drag capability restarted the feedback loop", stoppedLoop.value)
    }
}
