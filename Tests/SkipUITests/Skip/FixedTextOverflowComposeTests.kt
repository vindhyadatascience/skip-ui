// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class FixedTextOverflowComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun unlimitedTextCanPaintOutsideItsFrameWithoutChangingLayoutOrLineLimits() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                FixedTextLayoutProbe().Compose(ComposeContext())
            }
        }
        rule.onNodeWithTag("tiny-text-frame", useUnmergedTree = true).assertHeightIsEqualTo(4.dp)
        fun layout(tag: String): TextLayoutResult {
            val results = mutableListOf<TextLayoutResult>()
            rule.onNodeWithTag(tag, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertEquals(1, results.size)
            return results.single()
        }
        val unlimited = layout("unlimited-fixed-text")
        assertEquals(TextOverflow.Visible, unlimited.layoutInput.overflow)
        assertEquals(Int.MAX_VALUE, unlimited.layoutInput.maxLines)
        assertTrue(unlimited.multiParagraph.height > 4f)
        val limited = layout("limited-fixed-text")
        assertEquals(TextOverflow.Ellipsis, limited.layoutInput.overflow)
        assertEquals(1, limited.layoutInput.maxLines)
    }
}
