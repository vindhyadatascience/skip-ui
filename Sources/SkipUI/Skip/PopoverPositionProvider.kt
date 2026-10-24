// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt

/** Places the requested arrow edge against the source, flipping when that side cannot fit. */
internal class PopoverPositionProvider(private val arrowEdge: Edge, gapPixels: Float,
    private val attachmentRect: (IntRect, LayoutDirection) -> IntRect = { bounds, _ -> bounds }) : PopupPositionProvider {
    private val gap = gapPixels.roundToInt()

    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
        layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val source = attachmentRect(anchorBounds, layoutDirection)
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val below = source.bottom + gap
        val above = source.top - popupContentSize.height - gap
        val right = source.right + gap
        val left = source.left - popupContentSize.width - gap
        val centeredX = source.left + (source.width - popupContentSize.width) / 2
        val centeredY = source.top + (source.height - popupContentSize.height) / 2
        val x: Int
        val y: Int
        when (arrowEdge) {
            Edge.top -> { x = centeredX; y = if (below <= maxY) below else above }
            Edge.bottom -> { x = centeredX; y = if (above >= 0) above else below }
            Edge.leading, Edge.trailing -> {
                val preferRight = (arrowEdge == Edge.leading) == (layoutDirection == LayoutDirection.Ltr)
                x = if (preferRight) { if (right <= maxX) right else left }
                    else { if (left >= 0) left else right }
                y = centeredY
            }
        }
        return IntOffset(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
    }
}
