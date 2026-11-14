// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember

/** VDS: a child of `AnyLayout`, composed through a movable slot; the stack sees the child itself. */
internal class MovableRenderable(val renderable: Renderable, private val slot: @Composable (Renderable, ComposeContext) -> Unit) : View, Renderable {
    @Composable override fun Render(context: ComposeContext) { slot(renderable, context) }
    override fun strip(): Renderable = renderable.strip()
    override fun <R> forEachModifier(perform: (ModifierProtocol) -> R?): R? = renderable.forEachModifier(perform)
}

/**
 * VDS: the children of an `AnyLayout` as one view builder for the current stack, each composed
 * through a movable slot kept per child position for the caller's lifetime. When the stack
 * changes from a row to a column the slots move with their composition instead of restarting.
 */
@Composable internal fun rememberMovableChildren(renderables: kotlin.collections.List<Renderable>): ComposeBuilder {
    val slots = remember { ArrayList<@Composable (Renderable, ComposeContext) -> Unit>() }
    while (slots.size < renderables.size) {
        slots.add(movableContentOf { renderable: Renderable, context: ComposeContext -> renderable.Render(context) })
    }
    val children = renderables.mapIndexed { index: Int, renderable: Renderable -> MovableRenderable(renderable, slots[index]) }
    return ComposeBuilder(content = { context ->
        children.forEach { child -> child.Compose(context) }
        ComposeResult.ok
    })
}
