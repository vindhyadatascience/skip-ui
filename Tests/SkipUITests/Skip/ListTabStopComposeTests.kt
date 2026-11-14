// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Test

/** RNABrowser-gd1n.3.9: a list is one Tab stop with arrows inside; a form is not. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ListTabStopComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun tabLeavesTheListFromAnyRowAndArrowsMoveWithinIt() {
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ListTabStopProbe(asForm = false).Compose(ComposeContext()) }
        keyboardMode()
        rule.onNodeWithText("Before").requestFocus()
        tab()
        rule.onNode(hasSetTextAction()).assertIsFocused() // the heading field keeps its own stop
        tab()
        rule.onNodeWithText("Row 0").assertIsFocused()
        rule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithText("Row 1").assertIsFocused()
        tab()
        rule.onNodeWithText("After").assertIsFocused() // not Row 2 … Row 29
        shiftTab()
        rule.onNodeWithText("After").assertIsNotFocused()
        rule.onNodeWithText("Before").assertIsNotFocused()
        shiftTab()
        rule.onNodeWithText("Before").assertIsFocused()
    }

    @Test fun aFormKeepsTabThroughEachControl() {
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ListTabStopProbe(asForm = true).Compose(ComposeContext()) }
        keyboardMode()
        rule.onNodeWithText("Before").requestFocus()
        tab()
        rule.onNodeWithText("Form row 0").assertIsFocused()
        tab()
        rule.onNodeWithText("Form row 1").assertIsFocused()
    }

    /** A hardware keyboard puts Compose in keyboard input mode, where buttons take focus. */
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    private fun keyboardMode() = rule.runOnIdle { checkNotNull(inputMode).requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
    private var inputMode: androidx.compose.ui.input.InputModeManager? = null
    private fun tab() = rule.onRoot().performKeyInput { pressKey(Key.Tab) }
    private fun shiftTab() = rule.onRoot().performKeyInput { keyDown(Key.ShiftLeft); pressKey(Key.Tab); keyUp(Key.ShiftLeft) }
}
