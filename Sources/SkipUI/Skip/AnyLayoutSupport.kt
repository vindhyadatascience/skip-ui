// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** VDS: a child of `AnyLayout`, composed through a movable slot; the stack sees the child itself. */
internal class MovableRenderable(val renderable: Renderable, private val horizontal: Boolean, private val slot: @Composable (Renderable, ComposeContext, Boolean) -> Unit) : View, Renderable {
    @Composable override fun Render(context: ComposeContext) { slot(renderable, context, horizontal) }
    override fun strip(): Renderable = renderable.strip()
    override fun <R> forEachModifier(perform: (ModifierProtocol) -> R?): R? = renderable.forEachModifier(perform)
}

/**
 * VDS: the children of an `AnyLayout` as one view builder for the current stack, each composed
 * through a movable slot kept per child position for the caller's lifetime. When the stack
 * changes from a row to a column the slots move with their composition instead of restarting.
 */
@Composable internal fun rememberMovableChildren(renderables: kotlin.collections.List<Renderable>, horizontal: Boolean): ComposeBuilder {
    val slots = remember { ArrayList<@Composable (Renderable, ComposeContext, Boolean) -> Unit>() }
    while (slots.size < renderables.size) {
        // The axis is an input: a moved child recomposes under its new stack, whose environment
        // (flexible-size and alignment modifiers) differs; moving alone would keep the old one's.
        // The compiler skips on used parameters only, so the axis is read (into a state no one observes).
        slots.add(movableContentOf { renderable: Renderable, context: ComposeContext, axisIsHorizontal: Boolean ->
            rememberUpdatedState(axisIsHorizontal)
            renderable.Render(context)
        })
    }
    val children = renderables.mapIndexed { index: Int, renderable: Renderable -> MovableRenderable(renderable, horizontal, slots[index]) }
    return ComposeBuilder(content = { context ->
        children.forEach { child -> child.Compose(context) }
        ComposeResult.ok
    })
}
