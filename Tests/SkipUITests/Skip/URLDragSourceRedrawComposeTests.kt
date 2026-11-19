// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import skip.foundation.URL

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class URLDragSourceRedrawComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    /**
     * A drag source without a preview shape must not use Compose's default-shadow source. That
     * source draws its content from a recording cached until a resize, so on a device a change
     * inside it (a light/dark switch recolouring a report row's text) stayed in the old colours
     * (RNABrowser-idsl). The stale picture shows only with hardware rendering: Robolectric draws
     * to a software canvas, which replays the live content and cannot see it, and
     * `captureToImage` does not redraw under Robolectric. So this pins the modifier chain: the
     * source records its content on every draw, the way the preview-shape path always has.
     */
    @Test fun aDragSourceWithoutAPreviewShapeDrawsItsContentLive() {
        var modifier: Modifier? = null
        rule.setContent {
            modifier = Modifier.skipURLDragSource(LocalContext.current, URL(string = "https://example.com/figure.png")!!)
        }
        rule.waitForIdle()
        val elements = modifier!!.foldIn(listOf<String>()) { names, element -> names + (element::class.java.simpleName ?: "") }
        assertFalse("default-shadow source in $elements", elements.any { it.contains("WithDefaultShadow") })
        assertEquals("one drag source in $elements", 1, elements.count { it.startsWith("DragAndDropSource") })
    }
}
