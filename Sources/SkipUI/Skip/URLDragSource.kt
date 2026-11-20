// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.net.Uri
import android.view.View as AndroidView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.draganddrop.DragAndDropSourceModifierNode
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
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

/**
 * How a context menu around a drag source hears that a drag began (RNABrowser-1obr). On iOS a
 * long press opens the menu and moving the finger lifts the row into a drag, closing the menu;
 * the launcher does the same with its shortcut menus. A menu provides one of these to its
 * content, and a URL drag source inside it waits for that movement before it asks for the drag.
 */
internal class ContextMenuDragHandoff {
    var dragWillStart: () -> Unit = {}
}

internal val LocalContextMenuDragHandoff = compositionLocalOf<ContextMenuDragHandoff?> { null }

/** Test seam: told of each drag the source asks the OS for, before the transfer starts. */
internal var urlDragTransferRequestObserver: ((Offset) -> Unit)? = null

// VDS: the content is recorded on every draw and the OS drag decoration reuses that recording.
// Compose's default-shadow `dragAndDropSource { }` draws its content from a cached recording that
// only a resize refreshes, so a change inside the source (a light/dark switch recolouring its
// text) stayed on screen in the old colours (RNABrowser-idsl).
@Composable internal fun Modifier.skipURLDragSource(context: Context, payload: URL, previewShape: Shape? = null, eoFill: Boolean = false): Modifier {
    val preview = contentShapeFor(2) ?: previewShape?.let { ContentShapeDescriptor(it, eoFill) }
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val handoff = LocalContextMenuDragHandoff.current
    return drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }.then(URLDragSourceElement(drawDragDecoration = {
        if (preview == null) drawLayer(layer) else drawURLDragPreview(layer, preview, density)
    }, transferData = { offset ->
        urlDragTransferRequestObserver?.invoke(offset)
        urlDragTransferData(context, payload)
    }, handoff = handoff))
}

/**
 * Compose's `dragAndDropSource` with its own start detector. Touch starts the drag at a long
 * press, as Compose's default does, unless a context menu wraps the source: then the long press
 * is the menu's, and the drag starts once the finger moves past the touch slop, closing the menu.
 * A mouse drags once it moves past the slop, as Compose's default does.
 */
private data class URLDragSourceElement(
    val drawDragDecoration: DrawScope.() -> Unit,
    val transferData: (Offset) -> DragAndDropTransferData?,
    val handoff: ContextMenuDragHandoff?
) : ModifierNodeElement<URLDragSourceNode>() {
    override fun create() = URLDragSourceNode(drawDragDecoration, transferData, handoff)
    override fun update(node: URLDragSourceNode) {
        node.drawDragDecoration = drawDragDecoration
        node.transferData = transferData
        node.handoff = handoff
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "skipURLDragSource"
    }
}

private class URLDragSourceNode(
    var drawDragDecoration: DrawScope.() -> Unit,
    var transferData: (Offset) -> DragAndDropTransferData?,
    var handoff: ContextMenuDragHandoff?
) : DelegatingNode(), LayoutAwareModifierNode {
    private var size = IntSize.Zero
    private val source = delegate(DragAndDropSourceModifierNode { offset ->
        val data = transferData(offset) ?: return@DragAndDropSourceModifierNode
        startDragAndDropTransfer(data, size.toSize(), drawDragDecoration)
    })
    private var detector: SuspendingPointerInputModifierNode? = null

    override fun onAttach() {
        if (source.isRequestDragAndDropTransferRequired) {
            detector = delegate(SuspendingPointerInputModifierNode(PointerInputEventHandler { detectDragStart() }))
        }
    }

    override fun onDetach() {
        detector?.let { undelegate(it) }
        detector = null
    }

    override fun onPlaced(coordinates: LayoutCoordinates) = source.onPlaced(coordinates)

    override fun onRemeasured(size: IntSize) {
        this.size = size
        source.onRemeasured(size)
    }

    private suspend fun PointerInputScope.detectDragStart() {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val slop = viewConfiguration.touchSlop
            if (down.type == PointerType.Mouse) {
                // A secondary press is the context menu's.
                if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                if (awaitMovePastSlop(down, slop) != null) source.requestDragAndDropTransfer(down.position)
                return@awaitEachGesture
            }
            // The context menu consumes the press once it opens, so this reads positions
            // whether or not a change was consumed.
            var endedEarly = false
            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                awaitMovePastSlop(down, slop)
                endedEarly = true
            }
            if (endedEarly) return@awaitEachGesture
            val menu = handoff
            if (menu == null) {
                source.requestDragAndDropTransfer(down.position)
                return@awaitEachGesture
            }
            // The transfer starts from the pressed point: Compose looks the source up by that
            // offset, and the finger may already have left the source's bounds.
            awaitMovePastSlop(down, slop) ?: return@awaitEachGesture
            menu.dragWillStart()
            source.requestDragAndDropTransfer(down.position)
        }
    }
}

/** The pointer's position once it has moved past `slop` from `down` while pressed; nil once it lifts. */
private suspend fun AwaitPointerEventScope.awaitMovePastSlop(down: PointerInputChange, slop: Float): Offset? {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return null
        if (!change.pressed) return null
        if ((change.position - down.position).getDistance() > slop) return change.position
    }
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
