// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape

private class ContentShapeMarker(val identity: Any) : Modifier.Element

/**
 * Compose's layer shape participates in real hit testing, including sibling fall-through.
 * Record the child's drawing inside that hit layer, then replay once outside it: a SwiftUI
 * contentShape restricts interaction without clipping paint. The child is never composed or
 * measured twice, and rememberGraphicsLayer releases the retained record on disposal.
 */
@Composable internal fun Modifier.skipContentShape(shape: Shape, eoFill: Boolean, identity: Any): Modifier {
    if (foldIn(false) { found, element -> found || (element is ContentShapeMarker && element.identity === identity) }) {
        return this
    }
    val density = LocalDensity.current
    val outline = GenericShape { size, _ ->
        fillType = if (eoFill) PathFillType.EvenOdd else PathFillType.NonZero
        addPath(shape.asComposePath(size, density))
    }
    val paint = rememberGraphicsLayer()
    return then(ContentShapeMarker(identity))
        .drawWithContent {
            drawContent()
            drawLayer(paint)
        }
        .graphicsLayer(shape = outline, clip = true)
        .drawWithContent {
            paint.record { this@drawWithContent.drawContent() }
        }
}

internal class ContentShapeDescriptor(val shape: Shape, val eoFill: Boolean)
private class ContentShapeRoleMarker(val kinds: Int, val descriptor: ContentShapeDescriptor) : Modifier.Element

internal fun Modifier.contentShapeFor(kind: Int): ContentShapeDescriptor? =
    foldIn<ContentShapeDescriptor?>(null) { found, element -> found ?: (element as? ContentShapeRoleMarker)?.takeIf { it.kinds and kind != 0 }?.descriptor }

@Composable internal fun Modifier.skipContentShapeRoles(kinds: Int, shape: Shape, eoFill: Boolean): Modifier {
    val unsupported = kinds and (4 or 8)
    LaunchedEffect(unsupported) {
        if (unsupported != 0) android.util.Log.w("SkipUI", "contentShape: " +
            listOfNotNull(if (unsupported and 4 != 0) "contextMenuPreview (Android context-menu previews are unavailable)" else null,
                if (unsupported and 8 != 0) "hoverEffect (Android hoverEffect is unavailable)" else null).joinToString(", "))
    }
    var result = then(ContentShapeRoleMarker(kinds, ContentShapeDescriptor(shape, eoFill)))
    if (kinds and 16 != 0) {
        val density = LocalDensity.current
        val outline = GenericShape { size, _ ->
            fillType = if (eoFill) PathFillType.EvenOdd else PathFillType.NonZero
            addPath(shape.asComposePath(size, density))
        }
        // Compose exports this actual outline through Android's accessibility shape extra data.
        // It is independent of the pointer interaction shape and does not clip painted content.
        result = result.semantics { this.shape = outline }
    }
    return result
}
