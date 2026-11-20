// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class RowGestureComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var drawingView: android.view.View

    /**
     * A deletable row paints its red delete background only while it is swiped, as iOS reveals
     * its delete action and Material's own SwipeToDismissBox sample does. At rest the red was
     * drawn behind every row and showed through the rounded corners of an inset-grouped section
     * (the Report Builder's Figures card, RNABrowser-1eso).
     */
    @Test fun aDeletableRowAtRestPaintsNoRed() {
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            drawingView = LocalView.current
            DeletableRowProbe().Compose(ComposeContext())
        } }
        val bounds = rule.onNodeWithTag("deletable-One", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        var colour = 0
        rule.runOnIdle {
            assertTrue(drawingView.width > 0 && drawingView.height > 0)
            val bitmap = android.graphics.Bitmap.createBitmap(drawingView.width, drawingView.height, android.graphics.Bitmap.Config.ARGB_8888)
            drawingView.draw(android.graphics.Canvas(bitmap))
            colour = bitmap.getPixel(bounds.center.x.roundToInt(), bounds.center.y.roundToInt())
            bitmap.recycle()
        }
        assertNotEquals("red behind a row at rest", android.graphics.Color.RED, colour)
    }

    /** A long press alone opens the row's menu and leaves it open, as on iOS. */
    @Test fun aLongPressOnADraggableRowOpensItsMenu() {
        val requests = mutableListOf<Offset>()
        urlDragTransferRequestObserver = { requests.add(it) }
        try {
            rule.setContent { DraggableContextMenuRowProbe().Compose(ComposeContext()) }
            rule.onNodeWithText("Figure row").performTouchInput { down(center); advanceEventTime(700); up() }
            rule.onNodeWithText("Move Down").assertExists()
            rule.runOnIdle { assertEquals("no drag without movement", 0, requests.size) }
        } finally {
            urlDragTransferRequestObserver = null
        }
    }

    /**
     * Dragging after the long press turns the press into a drag: the menu closes and the drag
     * starts, as iOS lifts a context-menu preview into a drag. Both used to start at once, and
     * the menu stayed open over the dragged row (RNABrowser-1obr).
     */
    @Test fun draggingAfterTheLongPressClosesTheMenuAndStartsTheDrag() {
        val requests = mutableListOf<Offset>()
        urlDragTransferRequestObserver = { requests.add(it) }
        try {
            rule.setContent { DraggableContextMenuRowProbe().Compose(ComposeContext()) }
            rule.onNodeWithText("Figure row").performTouchInput { down(center) }
            rule.mainClock.advanceTimeBy(700)
            rule.onNodeWithText("Move Down").assertExists()
            rule.runOnIdle { assertEquals("no drag before the finger moves", 0, requests.size) }
            rule.onNodeWithText("Figure row").performTouchInput {
                moveBy(Offset(0f, 40f)); advanceEventTime(50); moveBy(Offset(0f, 40f)); up()
            }
            rule.onNodeWithText("Move Down").assertDoesNotExist()
            rule.runOnIdle { assertEquals("one drag", 1, requests.size) }
        } finally {
            urlDragTransferRequestObserver = null
        }
    }
}
