// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Material 3's `MenuVerticalMargin`: a dropdown must stay this far from the window's top and bottom. */
internal val DropdownMenuVerticalMargin = 48.dp

/**
 * VDS: the tallest a dropdown can be and still fit below or above its anchor, or -1 for no limit.
 *
 * Material's `DropdownMenu` only anchors a menu that fits within the vertical margins on one side
 * of its anchor; a taller one falls back to the window edge, far from the control that opened it.
 * Capping the menu at the larger side keeps it anchored, and the menu's own scroll covers the rest.
 * The bounds are conservative against both readings of Material's window (visible frame height,
 * or the frame offset into window coordinates), with a pixel of slack for rounding.
 */
internal fun dropdownMenuMaxHeight(anchorTop: Int, anchorBottom: Int, visibleTop: Int, visibleHeight: Int, marginPx: Int): Int {
    val below = visibleHeight - marginPx - anchorBottom
    val above = anchorTop - maxOf(visibleTop, 0) - marginPx
    val space = maxOf(below, above) - 1
    return if (space > 0) space else -1
}

internal class DropdownMenuHeightLimit {
    var maxHeightPx by mutableIntStateOf(-1)
}

/**
 * VDS: measures the dropdown's anchor (the parent layout, which is what Material's popup anchors
 * to) through a zero-size probe, and returns a modifier for the `DropdownMenu` that caps its
 * height so the menu fits beside the anchor. Call it inside the same parent as the `DropdownMenu`.
 * The limit is read at measure time, so anchor moves (scrolling) relayout only an open menu and
 * never recompose the caller.
 */
@Composable
internal fun rememberDropdownMenuHeightLimit(): Modifier {
    val view = LocalView.current
    val marginPx = with(LocalDensity.current) { DropdownMenuVerticalMargin.roundToPx() }
    val limit = remember { DropdownMenuHeightLimit() }
    Spacer(Modifier.size(0.dp).onGloballyPositioned { coordinates ->
        val anchor = coordinates.parentLayoutCoordinates ?: return@onGloballyPositioned
        val frame = android.graphics.Rect()
        view.getWindowVisibleDisplayFrame(frame)
        val top = anchor.positionInWindow().y.roundToInt()
        limit.maxHeightPx = dropdownMenuMaxHeight(top, top + anchor.size.height, frame.top, frame.height(), marginPx)
    })
    return remember(limit) {
        Modifier.layout { measurable, constraints ->
            val cap = limit.maxHeightPx
            val capped = if (cap > 0 && cap < constraints.maxHeight) {
                constraints.copy(minHeight = minOf(constraints.minHeight, cap), maxHeight = cap)
            } else {
                constraints
            }
            val placeable = measurable.measure(capped)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
    }
}
