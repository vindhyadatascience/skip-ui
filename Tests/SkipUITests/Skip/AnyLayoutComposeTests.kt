// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class AnyLayoutComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    /** RNABrowser-gd1n.3.7: a column folding into a row keeps the children's state and open sheet. */
    @Test fun changingAxisKeepsChildStateAndAnOpenSheet() {
        rule.setContent { AnyLayoutIdentityProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Count 0").performClick()
        rule.onNodeWithText("Count 1").assertIsDisplayed()
        val leadingColumn = rule.onNodeWithText("Leading child").getBoundsInRoot()
        val childColumn = rule.onNodeWithText("Count 1").getBoundsInRoot()
        assertTrue("a column stacks the child below", childColumn.top >= leadingColumn.bottom)

        rule.onNodeWithText("Show sheet").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText("Sheet body").fetchSemanticsNodes().isNotEmpty() }
        // The window resize that folds the page happens while the sheet is up.
        rule.onNodeWithText("Switch layout", useUnmergedTree = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onAllNodesWithText("Sheet body").assertCountEquals(1)
        rule.onNodeWithText("Count 1").assertExists()
        val leadingRow = rule.onNodeWithText("Leading child").getBoundsInRoot()
        val childRow = rule.onNodeWithText("Count 1").getBoundsInRoot()
        assertTrue("a row places the child beside", childRow.left >= leadingRow.right)
        assertEquals("the row's top alignment applies", leadingRow.top.value, childRow.top.value, 1f)
    }

    /** In a vertical scroll, the shorter column of a row still starts at the row's top. */
    @Test fun aRowInAScrollKeepsTopAlignment() {
        rule.setContent { AnyLayoutScrollProbe().Compose(ComposeContext()) }
        val header = rule.onNodeWithText("Short header").getBoundsInRoot()
        val inspector = rule.onNodeWithText("Inspector top").getBoundsInRoot()
        assertEquals("both columns start at the row's top", inspector.top.value, header.top.value, 1f)
    }

    /** RNABrowser-gd1n.3.8: the first frame at a new width already has the arrangement for it. */
    @Test fun theProposedWidthChoosesTheAxisInTheSameFrame() {
        assertTrue("the reader has no stale frame", !showsAStaleFrame { ProposalAxisProbe(width = it).Compose(ComposeContext()) })
    }

    /** Negative control: the measured-and-fed-back width shows the old arrangement for a frame. */
    @Test fun aMeasuredWidthShowsTheOldArrangementForAFrame() {
        assertTrue("the measured width lags", showsAStaleFrame { MeasuredAxisProbe(width = it).Compose(ComposeContext()) })
    }

    /** Shrinks 400 -> 200; true when a frame lays the row out at the new width. */
    private fun showsAStaleFrame(probe: @androidx.compose.runtime.Composable (Double) -> Unit): Boolean {
        var width by androidx.compose.runtime.mutableStateOf(400.0)
        rule.setContent { probe(width) }
        val first = rule.onNodeWithText("First").getBoundsInRoot()
        assertTrue("wide: beside", rule.onNodeWithText("Count 0").getBoundsInRoot().left >= first.right)
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { width = 200.0 }
        var stale = false
        for (i in 1..10) {
            rule.mainClock.advanceTimeByFrame()
            val now = rule.onNodeWithText("First").getBoundsInRoot()
            val child = rule.onNodeWithText("Count 0").getBoundsInRoot()
            if (now != first && child.top < now.bottom) stale = true
        }
        val final = rule.onNodeWithText("Count 0").getBoundsInRoot()
        assertTrue("settles below", final.top >= rule.onNodeWithText("First").getBoundsInRoot().bottom)
        rule.mainClock.autoAdvance = true
        return stale
    }
}
