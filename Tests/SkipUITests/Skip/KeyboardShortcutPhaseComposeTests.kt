// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import android.view.KeyEvent as AndroidKeyEvent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** SDK press-phase dispatch must coexist with the app's inherited release-phase fallback. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class KeyboardShortcutPhaseComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private var exits by mutableStateOf(0)
    private var registered by mutableStateOf(true)
    private val unhandled = mutableListOf<Pair<Key, KeyEventType>>()

    @Test fun cancelConsumesThePhysicalReleaseBeforeTheScreenFallback() {
        install(fallbackOutsideRoot = true)
        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals("one physical Escape must leave once", 1, exits) }
    }

    @Test fun aHeldShortcutPreservesRegisteredRepeatActionsWithoutLeakingItsRelease() {
        install(fallbackOutsideRoot = true)
        val downTime = android.os.SystemClock.uptimeMillis()
        send(downTime, 0, AndroidKeyEvent.ACTION_DOWN, 0)
        send(downTime, 40, AndroidKeyEvent.ACTION_DOWN, 1)
        send(downTime, 80, AndroidKeyEvent.ACTION_DOWN, 2)
        send(downTime, 100, AndroidKeyEvent.ACTION_UP, 0)
        rule.runOnIdle { assertEquals("all three registered KeyDown actions remain; release adds none", 3, exits) }
    }

    @Test fun releaseOwnershipOutlivesTheDisposedControlAndThenStops() {
        install(removeOnShortcut = true, fallbackOutsideRoot = true)
        val downTime = android.os.SystemClock.uptimeMillis()
        send(downTime, 0, AndroidKeyEvent.ACTION_DOWN, 0)
        rule.runOnIdle { assertFalse("shortcut control must actually be disposed", registered) }
        send(downTime, 40, AndroidKeyEvent.ACTION_DOWN, 1)
        send(downTime, 80, AndroidKeyEvent.ACTION_UP, 0)
        rule.runOnIdle { assertEquals("disposal before release must not expose the fallback", 1, exits) }
        // A subsequent, unregistered press is a real screen-boundary exit, not a stale claim.
        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals("unregistered next press must reach the fallback", 2, exits) }
    }

    @Test fun unregisteredEscapeAndUnrelatedKeysKeepTheirNormalDispatch() {
        registered = false
        install()
        rule.onRoot().performKeyInput { pressKey(Key.Escape); pressKey(Key.A) }
        rule.runOnIdle {
            assertEquals(1, exits)
            assertEquals(listOf(Key.A to KeyEventType.KeyDown, Key.A to KeyEventType.KeyUp), unhandled)
        }
    }

    @Test fun childPresentationRemovalDoesNotLeakReleaseIntoTheParentBoundary() {
        var presented by mutableStateOf(true)
        rule.setContent {
            KeyboardShortcutRoot(Modifier.fillMaxSize()) {
                val parentFocus = remember { FocusRequester() }
                Box(Modifier.fillMaxSize().testTag("parent-release-fallback")
                    .onKeyEvent { event ->
                        if (event.key == Key.Escape) {
                            if (event.type == KeyEventType.KeyUp) exits += 1
                            true
                        } else false
                    }.focusRequester(parentFocus).focusable()) {
                    if (presented) {
                        KeyboardShortcutRoot(Modifier.fillMaxSize()) {
                            Button("Dismiss child") { exits += 1; presented = false }
                                .keyboardShortcut(KeyboardShortcut.cancelAction).Compose(ComposeContext())
                        }
                    }
                }
                LaunchedEffect(presented) { if (!presented) parentFocus.requestFocus() }
            }
        }
        val downTime = android.os.SystemClock.uptimeMillis()
        send(downTime, 0, AndroidKeyEvent.ACTION_DOWN, 0)
        rule.runOnIdle { assertFalse("the nested shortcut root must actually disappear", presented) }
        rule.onNodeWithTag("parent-release-fallback").assertIsFocused()
        send(downTime, 40, AndroidKeyEvent.ACTION_DOWN, 1)
        send(downTime, 80, AndroidKeyEvent.ACTION_UP, 0)
        rule.runOnIdle { assertEquals("child dismissal owns the release even after its root disappears", 1, exits) }
        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals("the next unregistered press still reaches the parent", 2, exits) }
    }

    @Test fun ownedRepeatsFollowTheirMountedControlWhenFocusMovesOutsideItsRoot() {
        val ancestorFocus = FocusRequester()
        var rawDowns = 0
        rule.setContent {
            KeyboardShortcutRoot(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().testTag("ancestor-repeat-fallback")
                    .onKeyEvent { event ->
                        if (event.key == Key.Escape) {
                            if (event.type == KeyEventType.KeyDown) rawDowns += 1
                            true
                        } else false
                    }.focusRequester(ancestorFocus).focusable()) {
                    KeyboardShortcutRoot(Modifier.fillMaxSize()) {
                        Button("Mounted child") { exits += 1 }
                            .keyboardShortcut(KeyboardShortcut.cancelAction).Compose(ComposeContext())
                    }
                }
            }
        }
        val downTime = android.os.SystemClock.uptimeMillis()
        send(downTime, 0, AndroidKeyEvent.ACTION_DOWN, 0)
        rule.runOnIdle { assertEquals(1, exits); ancestorFocus.requestFocus() }
        rule.onNodeWithTag("ancestor-repeat-fallback").assertIsFocused()
        rule.onNodeWithText("Mounted child").assertIsDisplayed()
        send(downTime, 40, AndroidKeyEvent.ACTION_DOWN, 1)
        send(downTime, 80, AndroidKeyEvent.ACTION_UP, 0)
        rule.runOnIdle {
            assertEquals("the original live registration retains its repeat action", 2, exits)
            assertEquals("owned repeats must not leak to a newly focused raw boundary", 0, rawDowns)
        }
    }

    @Test fun aFreshUnmatchedDownClearsOwnershipOfAnInterruptedPress() {
        install(removeOnShortcut = true, fallbackOutsideRoot = true)
        val interruptedDownTime = android.os.SystemClock.uptimeMillis()
        send(interruptedDownTime, 0, AndroidKeyEvent.ACTION_DOWN, 0)
        rule.runOnIdle { assertFalse(registered); assertEquals(1, exits) }
        // Deliberately omit the old release, as when focus/window dispatch is interrupted.
        val freshDownTime = interruptedDownTime + 100
        send(freshDownTime, 0, AndroidKeyEvent.ACTION_DOWN, 0)
        send(freshDownTime, 20, AndroidKeyEvent.ACTION_UP, 0)
        rule.runOnIdle {
            assertEquals("a fresh unregistered press must retain its raw release action", 2, exits)
        }
    }

    @Test fun configurationResizePreservesChildFocusAndRecoversALostShortcutRoot() {
        var configuration by mutableStateOf(Configuration(rule.activity.resources.configuration))
        var focusManager: androidx.compose.ui.focus.FocusManager? = null
        var text by mutableStateOf("typed before resize")
        var findHits = 0
        rule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                focusManager = LocalFocusManager.current
                KeyboardShortcutRoot(Modifier.fillMaxSize().testTag("resize-shortcut-root")) {
                    val childFocus = remember { FocusRequester() }
                    Column {
                        BasicTextField(value = text, onValueChange = { text = it },
                            modifier = Modifier.testTag("resize-shortcut-field").focusRequester(childFocus))
                        Button("Find resize field") { findHits += 1; childFocus.requestFocus() }
                            .keyboardShortcut(KeyboardShortcut(KeyEquivalent('f'), modifiers = EventModifiers.command))
                            .Compose(ComposeContext())
                        Button("Cancel after resize") { exits += 1 }
                            .keyboardShortcut(KeyboardShortcut.cancelAction).Compose(ComposeContext())
                    }
                    // Initial focus only: neither resize nor recovery manually requests child focus.
                    LaunchedEffect(Unit) { childFocus.requestFocus() }
                }
            }
        }
        rule.onNodeWithTag("resize-shortcut-field").assertIsFocused()
        rule.runOnIdle {
            configuration = Configuration(configuration).apply {
                screenWidthDp += 64
                screenHeightDp += 48
            }
        }
        rule.onNodeWithTag("resize-shortcut-field").assertIsFocused()
        rule.onNodeWithTag("resize-shortcut-field").assertTextContains("typed before resize")
        rule.runOnIdle { checkNotNull(focusManager).clearFocus(force = true) }
        rule.onAllNodes(isFocused()).assertCountEquals(0)
        rule.runOnIdle {
            configuration = Configuration(configuration).apply {
                screenWidthDp += 64
                screenHeightDp += 48
            }
        }
        // Recovery must be caused by the changed configuration, not a test focus request.
        rule.onNodeWithTag("resize-shortcut-root").assertIsFocused()
        rule.onRoot().performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.F); keyUp(Key.CtrlLeft) }
        rule.onNodeWithTag("resize-shortcut-field").assertIsFocused()
        rule.onNodeWithTag("resize-shortcut-field").assertTextContains("typed before resize")
        rule.runOnIdle { assertEquals(1, findHits) }
        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals("cancel remains dispatched from a focused child after recovery", 1, exits) }
    }

    /** A focused view that clears its own selection on Escape keeps the key from the page's Cancel. */
    @Test fun aFocusedViewThatHandlesEscapeKeepsItFromAPlainShortcut() {
        var cleared = 0
        var commands = 0
        rule.setContent {
            KeyboardShortcutRoot(Modifier.fillMaxSize()) {
                val requester = remember { FocusRequester() }
                Column {
                    Button("Leave page") { exits += 1 }.keyboardShortcut(KeyboardShortcut.cancelAction).Compose(ComposeContext())
                    Button("Command") { commands += 1 }
                        .keyboardShortcut(KeyboardShortcut(KeyEquivalent('k'), modifiers = EventModifiers.command)).Compose(ComposeContext())
                    Box(Modifier.testTag("selection-owner").onKeyEvent { event ->
                        if (event.key == Key.Escape && event.type == KeyEventType.KeyDown && cleared == 0) { cleared += 1; true } else false
                    }.focusRequester(requester).focusable())
                }
                LaunchedEffect(Unit) { requester.requestFocus() }
            }
        }
        rule.onNodeWithTag("selection-owner").assertIsFocused()
        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals("the focused view clears first", 1, cleared); assertEquals("and the page stays", 0, exits) }
        // Nothing left to clear: the view declines, and the same key now leaves.
        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals(1, cleared); assertEquals(1, exits) }
        // Command shortcuts still preempt the focused view, as menu key equivalents do.
        rule.onRoot().performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.K); keyUp(Key.CtrlLeft) }
        rule.runOnIdle { assertEquals(1, commands) }
    }

    /** A sheet's window takes focus and the presenting window loses it; on return the button comes back. */
    @Test fun focusReturnsToItsButtonWhenTheWindowRegainsFocus() {
        val window = TestWindowInfo()
        var focusManager: androidx.compose.ui.focus.FocusManager? = null
        var inputMode: androidx.compose.ui.input.InputModeManager? = null
        rule.setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalWindowInfo provides window) {
                focusManager = LocalFocusManager.current
                inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
                KeyboardShortcutRoot(Modifier.fillMaxSize()) {
                    Column {
                        Button("First stop") {}.Compose(ComposeContext())
                        Button("Options") {}.Compose(ComposeContext())
                    }
                }
            }
        }
        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
        rule.runOnIdle { checkNotNull(inputMode).requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        rule.onNodeWithText("Options").requestFocus()
        rule.onNodeWithText("Options").assertIsFocused()
        // The sheet's window takes focus, and the presenting window's focus is lost meanwhile.
        rule.runOnIdle { window.focused = false }
        rule.runOnIdle { checkNotNull(focusManager).clearFocus(force = true) }
        rule.onNodeWithText("Options").assertIsNotFocused()
        // The sheet closes: the window is back, and so is the button, not the first stop.
        rule.runOnIdle { window.focused = true }
        rule.onNodeWithText("Options").assertIsFocused()
    }

    private class TestWindowInfo : androidx.compose.ui.platform.WindowInfo {
        var focused by mutableStateOf(true)
        override val isWindowFocused: Boolean get() = focused
    }

    /**
     * The screen's release-phase fallback consumes Escape in both phases and leaves on release.
     * Inside the root it is a focused view that keeps the key; `fallbackOutsideRoot` puts it
     * around the root instead, so a press reaches the registered shortcut first and the
     * fallback sees only what the root declines (a leaked release would count an extra exit).
     */
    private fun install(removeOnShortcut: Boolean = false, fallbackOutsideRoot: Boolean = false) {
        val fallback: Modifier = Modifier.onKeyEvent { event ->
            if (event.key == Key.Escape) {
                if (event.type == KeyEventType.KeyUp) exits += 1
                true
            } else { unhandled += event.key to event.type; false }
        }
        rule.setContent {
            Box(if (fallbackOutsideRoot) fallback else Modifier) {
                KeyboardShortcutRoot(Modifier.fillMaxSize()) {
                    val requester = remember { FocusRequester() }
                    Box(Modifier.fillMaxSize().testTag("screen-release-fallback")
                        .then(if (fallbackOutsideRoot) Modifier else fallback)
                        .focusRequester(requester).focusable()) {
                        if (registered) {
                            Button("Cancel") {
                                exits += 1
                                if (removeOnShortcut) registered = false
                            }.keyboardShortcut(KeyboardShortcut.cancelAction).Compose(ComposeContext())
                        }
                    }
                    LaunchedEffect(Unit) { requester.requestFocus() }
                }
            }
        }
        rule.onNodeWithTag("screen-release-fallback").assertIsFocused()
    }

    private fun send(downTime: Long, delta: Long, action: Int, repeat: Int) {
        rule.runOnIdle {
            rule.activity.dispatchKeyEvent(AndroidKeyEvent(downTime, downTime + delta,
                action, AndroidKeyEvent.KEYCODE_ESCAPE, repeat))
        }
    }
}
