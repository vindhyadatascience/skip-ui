// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ContextMenuPressAnchorComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    /** The anchor box sits at the press point, so Material opens the menu there. */
    @Test fun theAnchorFollowsEachPressWithoutResizingItsParent() {
        val anchor = ContextMenuPressAnchor()
        rule.setContent {
            Box(Modifier.size(300.dp, 80.dp).testTag("pressed-row")) {
                ContextMenuAnchorBox(anchor) { Box(Modifier.size(0.dp).testTag("menu-anchor")) }
            }
        }
        val row = rule.onNodeWithTag("pressed-row").getBoundsInRoot()
        rule.onNodeWithTag("menu-anchor").assertPositionInRootIsEqualTo(row.left, row.top)
        val density = rule.density.density
        rule.runOnIdle { anchor.set(Offset(250f * density, 30f * density)) }
        rule.onNodeWithTag("menu-anchor").assertPositionInRootIsEqualTo(row.left + 250.dp, row.top + 30.dp)
        rule.onNodeWithTag("pressed-row").assertWidthIsEqualTo(300.dp).assertHeightIsEqualTo(80.dp)
    }

    /** A right-click far from the row's leading edge opens the menu at the click, not at the edge. */
    @Test fun aSecondaryClickOpensTheMenuAtThePointer() {
        rule.setContent {
            Text(verbatim = "A wide row").frame(width = 300.0, height = 60.0)
                .contextMenu(menuItems = { Button(action = {}, label = { Text(verbatim = "Pin cohort") }) }).Compose()
        }
        val rowBounds = rule.onNodeWithText("A wide row").getBoundsInRoot()
        rule.onNodeWithText("A wide row").performMouseInput {
            moveTo(Offset(width - 20f, 10f)); press(MouseButton.Secondary); release(MouseButton.Secondary)
        }
        rule.onNodeWithText("Pin cohort").assertExists()
        // The menu item's window position is relative to the popup; compare its popup anchor through the row.
        val menu = rule.onNodeWithText("Pin cohort").fetchSemanticsNode()
        val row = rule.onNodeWithText("A wide row").fetchSemanticsNode()
        val menuLeft = menu.positionOnScreen.x
        val rowLeft = row.positionOnScreen.x
        val density = rule.density.density
        val pressX = rowLeft + (rowBounds.right - rowBounds.left).value * density - 20f
        assertEquals("the menu opens at the pointer, not the row's leading edge", pressX, menuLeft, 48f * density)
    }
}
