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
        install()
        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals("one physical Escape must leave once", 1, exits) }
    }

    @Test fun aHeldShortcutPreservesRegisteredRepeatActionsWithoutLeakingItsRelease() {
        install()
        val downTime = android.os.SystemClock.uptimeMillis()
        send(downTime, 0, AndroidKeyEvent.ACTION_DOWN, 0)
        send(downTime, 40, AndroidKeyEvent.ACTION_DOWN, 1)
        send(downTime, 80, AndroidKeyEvent.ACTION_DOWN, 2)
        send(downTime, 100, AndroidKeyEvent.ACTION_UP, 0)
        rule.runOnIdle { assertEquals("all three registered KeyDown actions remain; release adds none", 3, exits) }
    }

    @Test fun releaseOwnershipOutlivesTheDisposedControlAndThenStops() {
        install(removeOnShortcut = true)
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
        install(removeOnShortcut = true)
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

    private fun install(removeOnShortcut: Boolean = false) {
        rule.setContent {
            KeyboardShortcutRoot(Modifier.fillMaxSize()) {
                val requester = remember { FocusRequester() }
                Box(Modifier.fillMaxSize().testTag("screen-release-fallback")
                    .onKeyEvent { event ->
                        if (event.key == Key.Escape) {
                            if (event.type == KeyEventType.KeyUp) exits += 1
                            true
                        } else { unhandled += event.key to event.type; false }
                    }.focusRequester(requester).focusable()) {
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
        rule.onNodeWithTag("screen-release-fallback").assertIsFocused()
    }

    private fun send(downTime: Long, delta: Long, action: Int, repeat: Int) {
        rule.runOnIdle {
            rule.activity.dispatchKeyEvent(AndroidKeyEvent(downTime, downTime + delta,
                action, AndroidKeyEvent.KEYCODE_ESCAPE, repeat))
        }
    }
}
