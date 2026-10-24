// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class SupportedModifierComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun bindableTextFieldWritesThroughToObservableModel() {
        rule.setContent { BindableModifierProbe(ModifierBindingModel()).Compose(ComposeContext()) }
        rule.onNodeWithText("Model initial").assertIsDisplayed()
        rule.onNodeWithTag("bindable-field").performTextReplacement("edited")
        rule.onNodeWithText("Model edited").assertIsDisplayed()
        rule.onNodeWithTag("bindable-field").performTextReplacement("again")
        rule.onNodeWithText("Model again").assertIsDisplayed()
    }

    @Test fun destructiveRoleTintsRenderedLabelAndPreservesAction() {
        val error = ComposeColor(0.7f, 0.1f, 0.2f)
        rule.setContent {
            MaterialTheme(colorScheme = lightColorScheme(error = error)) {
                DestructiveButtonProbe().Compose(ComposeContext())
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag("destructive-label", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.size)
        assertEquals(error, layouts.single().layoutInput.style.color)
        rule.onNodeWithText("Delete item").performClick()
        rule.onNodeWithText("Clicks 1").assertIsDisplayed()
    }

    @Test fun swipeDeleteReportsIndexAndHonorsDeleteDisabled() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                DeleteModifierProbe().Compose(ComposeContext())
            }
        }
        rule.onNodeWithTag("delete-row-20").performTouchInput { swipeLeft() }
        rule.onNodeWithTag("delete-row-20").assertIsDisplayed()
        rule.onNodeWithText("Deleted -1").assertIsDisplayed()
        rule.onNodeWithTag("delete-row-10").performTouchInput { swipeLeft() }
        rule.onNodeWithTag("delete-row-10").assertDoesNotExist()
        rule.onNodeWithText("Deleted 0").assertIsDisplayed()
        rule.onNodeWithTag("delete-row-20").assertIsDisplayed()
        rule.onNodeWithTag("delete-row-30").assertIsDisplayed()
    }
}
