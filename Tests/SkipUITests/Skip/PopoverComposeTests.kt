// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class PopoverComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    /** Exercises the public adapter, including compact-window sheet adaptation. */
    @Test fun publicBooleanAndItemBindingsDismissAndReplaceTheirValues() {
        rule.setContent { PopoverRuntimeProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Open boolean").performClick()
        // Compact adaptation enters an Android dialog window after the presenting click.
        try {
            rule.waitUntil(5000) {
                kotlin.runCatching { rule.onNodeWithText("Boolean popover").assertIsDisplayed() }.isSuccess
            }
        } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
            val roots = rule.onAllNodes(isRoot(), useUnmergedTree = true)
            val trees = (0 until roots.fetchSemanticsNodes().size).joinToString("\n") { roots[it].printToString() }
            throw AssertionError("Compact popover failed actual placement:\n$trees", failure)
        }
        rule.onNodeWithText("Boolean popover").assertIsDisplayed()
        rule.onNodeWithText("Close popover").performClick()
        rule.onNodeWithText("Boolean false").assertIsDisplayed()
        rule.onNodeWithText("Open item").performClick()
        rule.onNodeWithText("Selected first").assertIsDisplayed()
        rule.onNodeWithText("Change item").performClick()
        rule.onNodeWithText("Selected second").assertIsDisplayed()
        rule.onNodeWithText("Close popover").performClick()
        rule.onNodeWithText("Item none").assertIsDisplayed()
    }

    /** Both adaptations share this presentation function; force the wide-window path. */
    @Test fun regularPopupDismissesThroughEnvironmentAndCanReopen() {
        val presented = mutableStateOf(true)
        rule.setContent {
            Box(Modifier.size(120.dp, 40.dp)) {
                PopoverPresentation(Binding(get = { presented.value }, set = { presented.value = it }),
                    false, Edge.top, boundsPopoverTestAnchor(), ComposeContext(), { PopoverDismissProbe("Regular popover") })
            }
        }
        rule.onNode(isPopup()).assertExists()
        rule.onNodeWithText("Regular popover").assertIsDisplayed()
        rule.onNodeWithText("Close popover").performClick()
        rule.runOnIdle { assertFalse(presented.value); presented.value = true }
        rule.onNodeWithText("Regular popover").assertIsDisplayed()
        rule.onNodeWithText("Close popover").performClick()
        rule.runOnIdle { assertFalse(presented.value) }
    }

    @Test fun positionHonorsArrowEdgeRightToLeftAndAvailableSpace() {
        val anchor = IntRect(200, 200, 240, 240)
        val window = IntSize(1000, 1000)
        val content = IntSize(100, 80)
        fun position(edge: Edge, direction: LayoutDirection = LayoutDirection.Ltr,
            bounds: IntRect = anchor): IntOffset =
            PopoverPositionProvider(edge, 8f).calculatePosition(bounds, window, direction, content)
        assertEquals(IntOffset(170, 248), position(Edge.top))
        assertEquals(IntOffset(170, 112), position(Edge.bottom))
        assertEquals(IntOffset(248, 180), position(Edge.leading))
        assertEquals(IntOffset(92, 180), position(Edge.trailing))
        assertEquals(IntOffset(92, 180), position(Edge.leading, LayoutDirection.Rtl))
        assertEquals(IntOffset(248, 180), position(Edge.trailing, LayoutDirection.Rtl))
        assertEquals(IntOffset(170, 862), position(Edge.top, bounds = IntRect(200, 950, 240, 990)))
        assertEquals(IntOffset(0, 18), position(Edge.bottom, bounds = IntRect(0, 0, 10, 10)))
        val custom = PopoverPositionProvider(Edge.top, 8f) { bounds, _ ->
            IntRect(bounds.left + 8, bounds.top + 10, bounds.left + 48, bounds.top + 30)
        }
        assertEquals(IntOffset(178, 238), custom.calculatePosition(anchor, window, LayoutDirection.Ltr, content))
    }
}
