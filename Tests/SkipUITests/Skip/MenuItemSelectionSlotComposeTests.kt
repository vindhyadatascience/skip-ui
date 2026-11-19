// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class MenuItemSelectionSlotComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun waitForText(text: String) {
        rule.waitUntil(5000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    /**
     * A ForEach-built menu item is not a selection: it gets no empty check-mark slot, so its
     * title starts where a written-out item's does (it was indented and truncated).
     */
    @Test fun forEachMenuItemsHaveNoSelectionSlot() {
        rule.setContent { ForEachMenuProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Row actions").performClick()
        waitForText("Export Project")
        val forEachLeft = rule.onNodeWithText("Export Project", useUnmergedTree = true).fetchSemanticsNode().positionOnScreen.x
        val writtenLeft = rule.onNodeWithText("Delete Project", useUnmergedTree = true).fetchSemanticsNode().positionOnScreen.x
        assertEquals(writtenLeft, forEachLeft, 1f)
    }

    /** A Picker's menu keeps its check mark on the selected item. */
    @Test fun pickerMenuStillMarksTheSelection() {
        rule.setContent { ForEachPickerProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Alpha").performClick()
        waitForText("Beta")
        rule.onAllNodesWithContentDescription("selected").assertCountEquals(1)
    }
}
