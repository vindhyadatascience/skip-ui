// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies the public style reaches real Text and keeps following the current theme. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class TertiaryStyleComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun textUsesTertiaryAlphaAndTracksThemeChanges() {
        val foreground = mutableStateOf(ComposeColor(0.2f, 0.4f, 0.6f))
        rule.setContent {
            MaterialTheme(colorScheme = lightColorScheme(onBackground = foreground.value)) {
                TertiaryStyleProbe().Compose(ComposeContext())
            }
        }
        fun assertTextColor(expected: ComposeColor) {
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithTag("tertiary-content").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.size)
            assertEquals(expected, layouts.single().layoutInput.style.color)
        }
        assertTextColor(foreground.value.copy(alpha = 0.38f))
        rule.runOnIdle { foreground.value = ComposeColor(0.8f, 0.3f, 0.1f) }
        assertTextColor(foreground.value.copy(alpha = 0.38f))
    }
}
