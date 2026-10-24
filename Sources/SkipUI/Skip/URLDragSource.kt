// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.net.Uri
import android.view.View as AndroidView
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import skip.foundation.URL

/** The same transfer factory is used by the actual Compose drag-start callback. */
internal fun urlDragTransferData(context: Context, payload: URL): DragAndDropTransferData? {
    val uri = Uri.parse(payload.absoluteString)
    return when (uri.scheme?.lowercase()) {
        "content" -> try {
            DragAndDropTransferData(
                clipData = ClipData.newUri(context.contentResolver, "File", uri),
                flags = AndroidView.DRAG_FLAG_GLOBAL or AndroidView.DRAG_FLAG_GLOBAL_URI_READ)
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        "http", "https" -> DragAndDropTransferData(
            clipData = ClipData.newRawUri("Link", uri), flags = AndroidView.DRAG_FLAG_GLOBAL)
        else -> null
    }
}

@Composable internal fun Modifier.skipURLDragSource(context: Context, payload: URL, previewShape: Shape? = null, eoFill: Boolean = false): Modifier {
    val preview = contentShapeFor(2) ?: previewShape?.let { ContentShapeDescriptor(it, eoFill) }
    if (preview == null) return dragAndDropSource { _ -> urlDragTransferData(context, payload) }
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    return drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }.dragAndDropSource(drawDragDecoration = {
        drawURLDragPreview(layer, preview, density)
    }, transferData = { _ -> urlDragTransferData(context, payload) })
}

/** The OS drag decoration uses the same recorded drawing, with only its preview clipped. */
internal fun DrawScope.drawURLDragPreview(layer: GraphicsLayer, preview: ContentShapeDescriptor, density: Density) {
    val path = preview.shape.asComposePath(size, density)
    path.fillType = if (preview.eoFill) PathFillType.EvenOdd else PathFillType.NonZero
    clipPath(path) { drawLayer(layer) }
}

/** Dialog/themed Compose hosts may supply a wrapper rather than the Activity itself. */
internal fun dragDropActivity(context: Context): Activity? {
    var current = context
    while (true) {
        if (current is Activity) return current
        if (current !is ContextWrapper) return null
        val base = current.baseContext
        if (base === current) return null
        current = base
    }
}
