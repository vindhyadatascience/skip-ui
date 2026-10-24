// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/** Measures content produced by the public Swift probe, including lazy List plumbing. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ListRowInsetsComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun explicitInsetsReplaceDefaultsAndOuterNilRestoresThem() {
        checkInsets(LayoutDirection.Ltr)
    }

    @Test fun leadingAndTrailingFollowRightToLeftLayout() {
        checkInsets(LayoutDirection.Rtl)
    }

    private fun checkInsets(direction: LayoutDirection) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalLayoutDirection provides direction) {
                ListRowInsetsProbe().Compose(ComposeContext())
            }
        }
        rule.waitForIdle()
        val zero = rule.onNodeWithTag("insets-zero", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val custom = rule.onNodeWithTag("insets-custom", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val zeroBackground = rule.onNodeWithTag("insets-zero-background", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val customBackground = rule.onNodeWithTag("insets-custom-background", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val defaults = rule.onNodeWithTag("insets-default", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val reset = rule.onNodeWithTag("insets-reset", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val outer = rule.onNodeWithTag("insets-outer", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val left = if (direction == LayoutDirection.Ltr) 12f else 24f
        val right = if (direction == LayoutDirection.Ltr) 24f else 12f
        assertEquals(left, custom.left - zero.left, 0.1f)
        assertEquals(right, zero.right - custom.right, 0.1f)
        assertEquals(36f, zero.width - custom.width, 0.1f)
        assertEquals(defaults.left, reset.left, 0.1f)
        assertEquals(defaults.right, reset.right, 0.1f)
        assertEquals(custom.left, outer.left, 0.1f)
        assertEquals(custom.right, outer.right, 0.1f)
        assertEquals(20f, custom.height, 0.1f)
        // List keeps a minimum content height, which centers this 20-point probe even with
        // zero insets. Subtract that measured common offset to isolate the added top inset.
        val zeroContentOffset = zero.top - zeroBackground.top
        val customContentOffset = custom.top - customBackground.top
        assertEquals(6f, customContentOffset - zeroContentOffset, 0.1f)
        assertEquals(14f, customBackground.height - zeroBackground.height, 0.1f)
    }
}
