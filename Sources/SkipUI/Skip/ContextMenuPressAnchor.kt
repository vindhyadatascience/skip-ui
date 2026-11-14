// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** VDS: the point a context menu press began, in the pressed content's own pixels. */
internal class ContextMenuPressAnchor {
    var point by mutableStateOf(Offset.Zero)
        private set
    fun set(point: Offset) { this.point = point }
}

/**
 * VDS: a zero-size box at the press point. Material's `DropdownMenu` opens beside its parent's
 * bounds, so a menu placed in this box opens at the pointer rather than at the content's edge,
 * and the dropdown height limit (which measures the same parent) sizes it from there. The
 * point is read at placement, so a new press moves the anchor without recomposing the content.
 * Pointer positions are physical, so the offset is not mirrored in right-to-left layouts.
 */
@Composable internal fun ContextMenuAnchorBox(anchor: ContextMenuPressAnchor, content: @Composable () -> Unit) {
    Box(Modifier.absoluteOffset { IntOffset(anchor.point.x.roundToInt(), anchor.point.y.roundToInt()) }.size(0.dp)) {
        content()
    }
}
