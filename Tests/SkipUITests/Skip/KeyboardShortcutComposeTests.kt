// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class KeyboardShortcutComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun commandUsesCtrlAndMetaWhileFocusedEditingKeepsPlainTyping() {
        install()
        rule.onNodeWithTag("shortcut-field").performClick()
        rule.onRoot().performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.F); keyUp(Key.CtrlLeft) }
        rule.onNodeWithText("Shortcut hits 1").assertIsDisplayed()
        rule.onRoot().performKeyInput { keyDown(Key.MetaLeft); pressKey(Key.F); keyUp(Key.MetaLeft) }
        rule.onNodeWithText("Shortcut hits 2").assertIsDisplayed()
        rule.onRoot().performKeyInput { pressKey(Key.F) }
        rule.onNodeWithTag("shortcut-field").assertTextContains("f")
        rule.onNodeWithText("Shortcut hits 2").assertIsDisplayed()
        rule.onRoot().performKeyInput { keyDown(Key.CtrlLeft); keyDown(Key.ShiftLeft); pressKey(Key.F); keyUp(Key.ShiftLeft); keyUp(Key.CtrlLeft) }
        rule.onNodeWithText("Shortcut hits 2").assertIsDisplayed()
    }

    @Test fun shortcutReadsFreshActionAndEnabledStateAndDisposesRemovedControl() {
        install()
        ctrlF()
        rule.onNodeWithText("Shortcut hits 1").assertIsDisplayed()
        rule.onNodeWithText("Change handler version").performClick()
        ctrlF()
        rule.onNodeWithText("Shortcut hits 4").assertIsDisplayed()
        rule.onNodeWithText("Toggle shortcut enabled").performClick()
        ctrlF()
        rule.onNodeWithText("Shortcut hits 4").assertIsDisplayed()
        rule.onNodeWithText("Toggle shortcut enabled").performClick()
        rule.onNodeWithText("Toggle shortcut presence").performClick()
        ctrlF()
        rule.onNodeWithText("Shortcut hits 4").assertIsDisplayed()
        rule.onNodeWithText("Toggle shortcut presence").performClick()
        ctrlF()
        rule.onNodeWithText("Shortcut hits 7").assertIsDisplayed()
    }

    @Test fun defaultCancelAndToggleInvokeExistingControlActions() {
        install()
        rule.onRoot().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithText("Default hits 1").assertIsDisplayed()
        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithText("Cancel hits 1").assertIsDisplayed()
        rule.onRoot().performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.T); keyUp(Key.CtrlLeft) }
        rule.onNodeWithText("Toggle on").assertIsDisplayed()
    }

    @Test fun automaticArrowsMirrorInRTL() = checkArrow(false, Key.DirectionRight)
    @Test fun withoutMirroringKeepsThePhysicalArrowInRTL() = checkArrow(true, Key.DirectionLeft)

    private fun checkArrow(withoutMirroring: Boolean, physical: Key) {
        rule.setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            KeyboardShortcutRoot(Modifier.fillMaxSize()) { KeyboardArrowShortcutProbe(withoutMirroring).Compose(ComposeContext()) }
        } }
        rule.onRoot().performKeyInput { pressKey(physical) }
        rule.onNodeWithText("Arrow hits 1").assertIsDisplayed()
    }
    private fun install() { rule.setContent {
        KeyboardShortcutRoot(Modifier.fillMaxSize()) { KeyboardShortcutRuntimeProbe().Compose(ComposeContext()) }
    } }
    private fun ctrlF() { rule.onRoot().performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.F); keyUp(Key.CtrlLeft) } }
}
