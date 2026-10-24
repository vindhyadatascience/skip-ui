// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Key phases share ownership across nested presentation roots, not their registrations. */
private class ShortcutPressOwner {
    data class PhysicalKey(val device: Int, val code: Int)
    class Press(val downTime: Long, val registry: ShortcutRegistry, val registration: Any)
    private val presses = HashMap<PhysicalKey, Press>()
    private fun key(event: AndroidKeyEvent) = PhysicalKey(event.deviceId, event.keyCode)
    fun current(event: AndroidKeyEvent): Press? = presses[key(event)]?.takeIf { it.downTime == event.downTime }
    fun release(event: AndroidKeyEvent): Boolean {
        if (current(event) == null) return false
        presses.remove(key(event))
        return true
    }
    fun clearStaleDown(event: AndroidKeyEvent) { presses.remove(key(event)) }
    fun claim(event: AndroidKeyEvent, registry: ShortcutRegistry, registration: Any) {
        presses[key(event)] = Press(event.downTime, registry, registration)
    }
}

private class ShortcutRegistry(private val pressOwner: ShortcutPressOwner) {
    class Entry(var shortcut: KeyboardShortcut?, var enabled: Boolean, var direction: LayoutDirection, var action: () -> Unit)
    val entries = LinkedHashMap<Any, Entry>()
    var isMounted = true
    fun dispatch(event: KeyEvent): Boolean {
        val native = event.nativeKeyEvent
        if (event.type == KeyEventType.KeyUp) return pressOwner.release(native)
        if (event.type != KeyEventType.KeyDown) return false
        val owned = pressOwner.current(native)
        if (owned != null) {
            // Focus may have moved outside the original root. Dispatch its current entry
            // directly instead of leaking this held press to a new scope/raw-key boundary.
            val original = owned.registry
            val entry = if (original.isMounted) original.entries[owned.registration] else null
            if (entry?.enabled == true && entry.shortcut?.let {
                keyboardShortcutMatches(it, native, entry.direction) } == true) entry.action()
            return true
        }
        // New native downTime means an interrupted old press no longer owns this key.
        pressOwner.clearStaleDown(native)
        val match = entries.entries.firstOrNull { (_, entry) -> entry.enabled && entry.shortcut?.let {
            keyboardShortcutMatches(it, native, entry.direction) } == true } ?: return false
        // Claim before invoking: the action can synchronously remove its control/root.
        pressOwner.claim(native, this, match.key)
        match.value.action()
        return true
    }
}
private val LocalShortcutRegistry = staticCompositionLocalOf<ShortcutRegistry?> { null }
private val LocalShortcutPressOwner = staticCompositionLocalOf<ShortcutPressOwner?> { null }

/** Each window/presentation owns its visible shortcuts and removes registrations on disposal. */
@Composable internal fun KeyboardShortcutRoot(modifier: Modifier, content: @Composable () -> Unit) {
    val pressOwner = LocalShortcutPressOwner.current ?: remember { ShortcutPressOwner() }
    val registry = remember(pressOwner) { ShortcutRegistry(pressOwner) }
    DisposableEffect(registry) {
        registry.isMounted = true
        onDispose { registry.isMounted = false }
    }
    val requester = remember { FocusRequester() }
    val hasFocus = remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalShortcutRegistry provides registry,
        LocalShortcutPressOwner provides pressOwner) {
        Box(modifier.onPreviewKeyEvent { registry.dispatch(it) }.focusRequester(requester)
            .onFocusChanged { hasFocus.value = it.hasFocus }.focusable()) { content() }
    }
    // Resizing can clear the whole focus tree. Recover only when no child kept focus.
    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration.screenWidthDp, configuration.screenHeightDp) {
        if (!hasFocus.value) requester.requestFocus()
    }
}

@Composable internal fun RegisterKeyboardShortcut(shortcut: KeyboardShortcut?, enabled: Boolean, action: () -> Unit) {
    val registry = LocalShortcutRegistry.current ?: return
    val identity = remember { Any() }
    val direction = LocalLayoutDirection.current
    // No observable state is written: callbacks always see this composition's actual action/enabled state.
    SideEffect { registry.entries[identity] = ShortcutRegistry.Entry(shortcut, enabled, direction, action) }
    DisposableEffect(registry, identity) { onDispose { registry.entries.remove(identity) } }
}

/** Android treats Ctrl as the platform Command equivalent, and accepts physical Meta keyboards too. */
internal fun keyboardShortcutMatches(shortcut: KeyboardShortcut, event: AndroidKeyEvent, direction: LayoutDirection): Boolean {
    val modifiers = shortcut.modifiers.rawValue
    val command = modifiers and 16 != 0
    val control = modifiers and 4 != 0
    val ctrl = event.isCtrlPressed
    val meta = event.isMetaPressed
    if (command) {
        if (control) { if (!ctrl || !meta) return false }
        else if (ctrl == meta) return false
    } else if (ctrl != control || meta) return false
    if (event.isShiftPressed != (modifiers and 2 != 0) || event.isAltPressed != (modifiers and 8 != 0)) return false
    if (modifiers and 1 != 0 && !event.isCapsLockOn) return false
    if (modifiers and 32 != 0 && event.keyCode !in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN) return false
    var character = shortcut.key.character
    if (direction == LayoutDirection.Rtl && shortcut.localization == KeyboardShortcut.Localization.automatic) {
        if (character == '\uF702') character = '\uF703' else if (character == '\uF703') character = '\uF702'
    }
    val keyCode = when (character) {
        '\uF700' -> AndroidKeyEvent.KEYCODE_DPAD_UP
        '\uF701' -> AndroidKeyEvent.KEYCODE_DPAD_DOWN
        '\uF702' -> AndroidKeyEvent.KEYCODE_DPAD_LEFT
        '\uF703' -> AndroidKeyEvent.KEYCODE_DPAD_RIGHT
        '\u001B' -> AndroidKeyEvent.KEYCODE_ESCAPE
        '\u0008' -> AndroidKeyEvent.KEYCODE_DEL
        '\uF728' -> AndroidKeyEvent.KEYCODE_FORWARD_DEL
        '\uF729' -> AndroidKeyEvent.KEYCODE_MOVE_HOME
        '\uF72B' -> AndroidKeyEvent.KEYCODE_MOVE_END
        '\uF72C' -> AndroidKeyEvent.KEYCODE_PAGE_UP
        '\uF72D' -> AndroidKeyEvent.KEYCODE_PAGE_DOWN
        '\uF739' -> AndroidKeyEvent.KEYCODE_CLEAR
        '\u0009' -> AndroidKeyEvent.KEYCODE_TAB
        '\u0020' -> AndroidKeyEvent.KEYCODE_SPACE
        '\u000D' -> AndroidKeyEvent.KEYCODE_ENTER
        else -> null
    }
    if (keyCode != null) return event.keyCode == keyCode ||
        (character == '\u000D' && event.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)
    val withoutCommand = event.metaState and (AndroidKeyEvent.META_CTRL_MASK or AndroidKeyEvent.META_META_MASK or AndroidKeyEvent.META_ALT_MASK).inv()
    val typed = event.getUnicodeChar(withoutCommand).toChar()
    val unshifted = event.getUnicodeChar(withoutCommand and AndroidKeyEvent.META_SHIFT_MASK.inv()).toChar()
    return character.lowercaseChar() == typed.lowercaseChar() || character.lowercaseChar() == unshifted.lowercaseChar()
}
