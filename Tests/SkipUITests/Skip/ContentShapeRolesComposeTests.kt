// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ContentShapeRolesComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var drawingView: android.view.View

    // PixelCopy/captureToImage waits for a real window redraw under Robolectric. Draw the
    // actual Compose host onto a bitmap, matching XCSnapshotTestCase's host rendering path.
    private fun pixel(tag: String, x: Int, y: Int): Int {
        val bounds = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        var value = 0
        rule.runOnIdle {
            assertTrue(drawingView.width > 0 && drawingView.height > 0)
            val bitmap = android.graphics.Bitmap.createBitmap(drawingView.width, drawingView.height, android.graphics.Bitmap.Config.ARGB_8888)
            drawingView.draw(android.graphics.Canvas(bitmap))
            value = bitmap.getPixel(bounds.left.roundToInt() + x, bounds.top.roundToInt() + y)
            bitmap.recycle()
        }
        return value
    }

    @Test fun accessibilityExportsItsActualOutlineWithoutChangingPaint() {
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            drawingView = LocalView.current
            AccessibleContentShapeProbe().Compose(ComposeContext())
        } }
        val node = rule.onNodeWithTag("accessible-circle", useUnmergedTree = true)
        val outline = node.fetchSemanticsNode().config[SemanticsProperties.Shape]
            .createOutline(androidx.compose.ui.geometry.Size(100f, 100f), LayoutDirection.Ltr, Density(1f))
        assertTrue(outline is Outline.Generic)
        assertEquals(100f, (outline as Outline.Generic).path.getBounds().width, 0.1f)
        assertEquals(android.graphics.Color.RED, pixel("accessible-circle", 0, 0))
        assertEquals(android.graphics.Color.RED, pixel("accessible-circle", 50, 50))
    }

    @Test fun onlyTheRecordedOSDragPreviewIsClippedAndTheActualSourceKeepsItsCorners() {
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            drawingView = LocalView.current
            Column {
                URLDragPreviewShapeProbe().Compose(ComposeContext())
                val layer = rememberGraphicsLayer()
                Canvas(Modifier.size(100.dp).background(ComposeColor.White).testTag("os-drag-decoration")) {
                    layer.record { drawRect(ComposeColor.Red) }
                    drawURLDragPreview(layer, ContentShapeDescriptor(Circle(), false), Density(1f))
                }
            }
        } }
        assertEquals(android.graphics.Color.RED, pixel("unclipped-drag-source", 0, 0))
        assertEquals(android.graphics.Color.WHITE, pixel("os-drag-decoration", 0, 0))
        assertEquals(android.graphics.Color.RED, pixel("os-drag-decoration", 50, 50))
    }
}
