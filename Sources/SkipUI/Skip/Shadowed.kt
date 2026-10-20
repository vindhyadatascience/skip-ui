// Copyright 2023–2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil

/**
 * Shadow the child's alpha silhouette without composing or measuring it twice.
 * The retained display list is shared by the normal and tinted/blurred layers. Unlike an
 * elevation shadow this preserves transparent content, arbitrary shapes, color and x/y offsets.
 */
@Composable fun Shadowed(context: ComposeContext, color: Color, offsetX: Dp, offsetY: Dp, blurRadius: Dp, content: @Composable (ComposeContext) -> Unit) {
    require(blurRadius.value.isFinite() && blurRadius.value >= 0f) { "Shadow radius must be finite and non-negative" }
    val direction = LocalLayoutDirection.current
    val contentLayer = rememberGraphicsLayer()
    val shadowLayer = rememberGraphicsLayer()
    val modifier = context.modifier.drawWithCache {
        val radiusPx = blurRadius.toPx()
        val padding = ceil(radiusPx).toInt()
        val x = offsetX.toPx().toInt().toFloat() * if (direction == LayoutDirection.Rtl) -1f else 1f
        val y = offsetY.toPx().toInt().toFloat()
        shadowLayer.colorFilter = ColorFilter.colorMatrix(shadowColorMatrix(color))
        shadowLayer.renderEffect = if (radiusPx > 0f) BlurEffect(radiusPx, radiusPx, TileMode.Decal) else null
        onDrawWithContent {
            contentLayer.record {
                this@onDrawWithContent.drawContent()
            }
            shadowLayer.record(size = IntSize(contentLayer.size.width + padding * 2, contentLayer.size.height + padding * 2)) {
                translate(padding.toFloat(), padding.toFloat()) { drawLayer(contentLayer) }
            }
            translate(x - padding, y - padding) { drawLayer(shadowLayer) }
            drawLayer(contentLayer)
        }
    }
    Layout(modifier = modifier, content = { content(context.content()) }) { measurables, constraints ->
        val placeable = measurables[0].measure(constraints)
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
}

private fun shadowColorMatrix(color: Color): ColorMatrix {
    return ColorMatrix().apply {
        set(0, 0, 0f)
        set(1, 1, 0f)
        set(2, 2, 0f)
        set(0, 4, color.red * 255)
        set(1, 4, color.green * 255)
        set(2, 4, color.blue * 255)
        set(3, 3, color.alpha)
    }
}
