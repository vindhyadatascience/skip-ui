// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

/**
 * VDS: a `List` is ONE Tab stop, as a desktop list is (RNABrowser-gd1n.3.9). Tab or Shift-Tab from
 * a row leaves the whole list instead of walking every row; the arrow keys move between rows (the
 * platform's directional focus search, which scrolls the lazy column). A row whose text field has
 * focus keeps ordinary Tab, so a search field heading the list stays its own stop. `Form` keeps
 * Tab through each control. Leaving goes through a zero-size focus target on each side of the
 * list that can take focus only during that hand-off, then on in the requested direction.
 */
internal class ListKeyboardNavigation {
    val before = FocusRequester()
    val after = FocusRequester()
    /** Read by the edge targets' focus properties; true only while Tab is leaving the list. */
    var isLeaving = false
    private val rowTextInputs = LinkedHashSet<MutableState<Boolean>>()

    fun register(textInputFocused: MutableState<Boolean>) { rowTextInputs.add(textInputFocused) }
    fun unregister(textInputFocused: MutableState<Boolean>) { rowTextInputs.remove(textInputFocused) }
    val isEditingText: Boolean get() = rowTextInputs.any { it.value }

    fun leave(forward: Boolean, focusManager: FocusManager) {
        isLeaving = true
        try {
            val edge = if (forward) after else before
            if (edge.requestFocus()) focusManager.moveFocus(if (forward) FocusDirection.Next else FocusDirection.Previous)
        } finally {
            isLeaving = false
        }
    }
}

@Composable internal fun rememberListKeyboardNavigation(): ListKeyboardNavigation = remember { ListKeyboardNavigation() }

/** Tracks a row's text-input focus for the list's Tab rule while the row is composed. */
@Composable internal fun RegisterListRowTextInput(navigation: ListKeyboardNavigation, textInputFocused: MutableState<Boolean>) {
    DisposableEffect(navigation, textInputFocused) {
        navigation.register(textInputFocused)
        onDispose { navigation.unregister(textInputFocused) }
    }
}

/** The list's Tab handling, on the lazy column: it sees Tab before any row does. */
@Composable internal fun Modifier.listKeyboardNavigation(navigation: ListKeyboardNavigation, enabled: Boolean): Modifier {
    if (!enabled) return this
    val focusManager = LocalFocusManager.current
    return this.onPreviewKeyEvent { event ->
        if (event.key != Key.Tab || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed || navigation.isEditingText) {
            false
        } else {
            if (event.type == KeyEventType.KeyDown) navigation.leave(forward = !event.isShiftPressed, focusManager = focusManager)
            true
        }
    }
}

/** A zero-size edge of the list that focus passes through when Tab leaves it. */
@Composable internal fun ListKeyboardEdge(navigation: ListKeyboardNavigation, trailing: Boolean) {
    Box(Modifier.size(0.dp).focusRequester(if (trailing) navigation.after else navigation.before)
        .focusProperties { canFocus = navigation.isLeaving }.focusTarget())
}
