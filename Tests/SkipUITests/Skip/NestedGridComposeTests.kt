// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.layout.positionInRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class NestedGridComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun nestedGridOwnsAllRowsAndFooterAfterInsertionAndColumnChanges() = checkNested(LayoutDirection.Ltr)
    @Test fun nestedGridPreservesRightToLeftGeometry() = checkNested(LayoutDirection.Rtl)

    private fun checkNested(direction: LayoutDirection) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalLayoutDirection provides direction) {
                NestedGridRuntimeProbe().Compose(ComposeContext())
            }
        }
        checkHeightAndFooter(302)
        rule.onNodeWithText("Add grid items").performClick()
        checkHeightAndFooter(404)
        rule.onNodeWithText("Change grid columns").performClick()
        checkHeightAndFooter(268)
        rule.onNodeWithTag("grid-footer").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Grid item 23").assertIsDisplayed()
    }

    private fun checkHeightAndFooter(expectedHeight: Int) {
        rule.waitForIdle()
        val grid = rule.onNodeWithTag("nested-grid", useUnmergedTree = true).fetchSemanticsNode().layoutInfo.coordinates
        val footer = rule.onNodeWithTag("grid-footer", useUnmergedTree = true).fetchSemanticsNode().layoutInfo.coordinates
        // Coordinates retain the complete measured size even when an outer viewport clips paint.
        assertEquals(expectedHeight, grid.size.height)
        assertEquals(10f, footer.positionInRoot().y - grid.positionInRoot().y - grid.size.height, 0.1f)
    }

    @Test fun intrinsicGridKeepsTheForEachImplicitIDScrollToRoute() {
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            NestedGridImplicitIDScrollProbe().Compose(ComposeContext())
        } }
        rule.onNodeWithText("Implicit grid item 39").assertExists().assertIsNotDisplayed()
        rule.onNodeWithText("Scroll to last implicit grid ID").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Implicit grid item 39").assertIsDisplayed()
    }

    @Test fun soleDirectGridRetainsLazyCompositionAndCanReachTheLastItem() {
        val counter = GridBodyCounter()
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            SoleLazyGridRuntimeProbe(counter).Compose(ComposeContext())
        } }
        rule.waitForIdle()
        assertTrue("A sole grid must not compose all 500 cells", counter.bodies in 1 until 500)
        rule.onNodeWithText("Lazy item 499").assertDoesNotExist()
        rule.onNode(
            hasScrollToIndexAction() and
                (hasTestTag("sole-lazy-grid") or hasAnyAncestor(hasTestTag("sole-lazy-grid"))),
            useUnmergedTree = true
        ).performScrollToIndex(499)
        rule.onNodeWithText("Lazy item 499").assertIsDisplayed()
    }
}
