// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.roundToInt

/** A resolved slot includes its following gap so Compose can retain its lazy grid renderer. */
internal data class NativeGridSlot(val size: Int, val gap: Int, val alignment: Alignment)

/** Natural size is observed only by measurement, never composition. */
internal class NativeGridMeasurement {
    val mainSize = mutableIntStateOf(0)
}

/** Shared by lazy and intrinsic-height grids; specs are snapshotted when the view is composed. */
internal class NativeGridCells(items: skip.lib.Array<GridItem>) : GridCells {
    private data class Spec(val kind: Int, val minimum: Double, val maximum: Double,
        val gap: Double, val alignment: Alignment)
    private val specs = if (items.collection.isEmpty()) {
        // Retain the existing empty-spec single-column fallback.
        listOf(Spec(1, 0.0, Double.POSITIVE_INFINITY, 0.0, Alignment.Center))
    } else items.collection.map { item ->
        val alignment = item.alignment?.asComposeAlignment() ?: Alignment.Center
        when (val size = item.size) {
            is GridItem.Size.FixedCase -> Spec(0, size.associated0, size.associated0, item.spacing ?: 8.0, alignment)
            is GridItem.Size.FlexibleCase -> Spec(1, size.minimum, size.maximum, item.spacing ?: 8.0, alignment)
            is GridItem.Size.AdaptiveCase -> Spec(2, size.minimum, size.maximum, item.spacing ?: 8.0, alignment)
        }
    }
    // A measurement result, never observable composition state. Children read it while measuring.
    internal var slots: kotlin.collections.List<NativeGridSlot> = emptyList()
        private set
    private val slotRevision = mutableIntStateOf(0)
    private data class CellKey(val group: Int, val index: Int)
    private val measurements = mutableStateMapOf<CellKey, NativeGridMeasurement>()

    internal fun rowMainSize(group: Int, index: Int, owner: NativeGridMeasurement, natural: Int): Int {
        // A cross-axis regrouping can change peers without changing an item's
        // own constraints. Observe the resolved layout during measure too.
        val count = slotRevision.intValue.let { slots.size.coerceAtLeast(1) }
        // Called during measure. Store each natural size rather than retaining a
        // monotonic row maximum, so a tall peer can shrink on the next measure.
        val key = CellKey(group, index)
        if (measurements[key] !== owner) measurements[key] = owner
        if (owner.mainSize.intValue != natural) owner.mainSize.intValue = natural
        val first = index / count * count
        return (first until first + count).maxOf { measurements[CellKey(group, it)]?.mainSize?.intValue ?: 0 }
    }

    internal fun removeMeasurement(group: Int, index: Int, owner: NativeGridMeasurement) {
        val key = CellKey(group, index)
        // Disposal only removes ownership; natural values are still accessed
        // exclusively in measure. Do not remove a new cell reusing the same key.
        if (measurements[key] === owner) measurements.remove(key)
    }

    internal fun resolve(available: Int, density: Density): kotlin.collections.List<NativeGridSlot> {
        if (specs.isEmpty()) { slots = emptyList(); return slots }
        val scale = density.density.toDouble()
        var remaining = available.toDouble() - specs.dropLast(1).sumOf { it.gap * scale }
        remaining -= specs.filter { it.kind == 0 }.sumOf { it.minimum * scale }
        var flexible = specs.count { it.kind != 0 }
        val widths = specs.map { spec ->
            if (spec.kind == 0) spec.minimum * scale else {
                val share = remaining / flexible
                // Adaptive maximum applies to expanded subcells, not the whole
                // allocation that determines how many subcells fit.
                val width = if (spec.kind == 2) share.coerceAtLeast(spec.minimum * scale)
                    else share.coerceIn(spec.minimum * scale, spec.maximum * scale)
                remaining -= width
                flexible--
                width
            }
        }
        data class LogicalSlot(val size: Double, val gap: Double, val alignment: Alignment)
        val logical = mutableListOf<LogicalSlot>()
        for (index in specs.indices) {
            val spec = specs[index]
            val gap = spec.gap * scale
            val allocated = widths[index]
            val count = if (spec.kind == 2 && spec.minimum * scale + gap > 0.0)
                floor((allocated + gap) / (spec.minimum * scale + gap)).toInt().coerceAtLeast(1) else 1
            val size = if (spec.kind == 2)
                ((allocated - (count - 1) * gap) / count).coerceAtMost(spec.maximum * scale) else allocated
            repeat(count) { subindex ->
                val followingGap = if (index == specs.lastIndex && subindex == count - 1) 0.0 else gap
                logical += LogicalSlot(size, followingGap, spec.alignment)
            }
        }
        var position = 0.0
        val resolved = logical.map { slot ->
            val start = position.roundToInt()
            val end = (position + slot.size).roundToInt()
            position += slot.size + slot.gap
            NativeGridSlot((end - start).coerceAtLeast(0), position.roundToInt() - end, slot.alignment)
        }
        if (resolved != slots) {
            slots = resolved
            slotRevision.intValue += 1
        }
        return slots
    }

    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): kotlin.collections.List<Int> =
        resolve(availableSize, this).map { (it.size + it.gap).coerceAtLeast(0) }
}

/** Keep the slot's spacing outside the child's proposed size and honor each column/row alignment. */
@Composable internal fun NativeGridCell(cells: NativeGridCells, index: Int, vertical: Boolean,
    rowGroup: Int = 0,
    content: @Composable () -> Unit) {
    val measurement = remember(cells, rowGroup, index) { NativeGridMeasurement() }
    DisposableEffect(cells, rowGroup, index, measurement) {
        onDispose { cells.removeMeasurement(rowGroup, index, measurement) }
    }
    Layout(content = content) { measurables, constraints ->
        val slot = cells.slots.getOrNull(index.mod(cells.slots.size.coerceAtLeast(1)))
        val crossSize = slot?.size ?: if (vertical) constraints.maxWidth else constraints.maxHeight
        val childConstraints = if (vertical) constraints.copy(minWidth = 0, maxWidth = crossSize, minHeight = 0)
            else constraints.copy(minHeight = 0, maxHeight = crossSize, minWidth = 0)
        val children = measurables.map { it.measure(childConstraints) }
        val natural = children.maxOfOrNull { if (vertical) it.height else it.width } ?: 0
        val mainSize = cells.rowMainSize(rowGroup, index, measurement, natural)
        val width = if (vertical) constraints.constrainWidth(crossSize + (slot?.gap ?: 0))
            else constraints.constrainWidth(mainSize)
        val height = if (!vertical) constraints.constrainHeight(crossSize + (slot?.gap ?: 0))
            else constraints.constrainHeight(mainSize)
        layout(width, height) {
            val space = if (vertical) IntSize(crossSize, height) else IntSize(width, crossSize)
            for (child in children) {
                val offset = (slot?.alignment ?: Alignment.Center).align(IntSize(child.width, child.height), space, layoutDirection)
                // The grid mirrors slots; mirror the following logical gap within this slot too.
                val logicalX = if (vertical && layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl)
                    width - crossSize + offset.x else offset.x
                child.place(logicalX, offset.y)
            }
        }
    }
}

internal class NativeGridEntry(val identity: Any?, val fullSpan: Boolean, val content: @Composable () -> Unit)

/** An outer ScrollView owns scrolling when a grid has siblings. Measure every row to expose its true height. */
@Composable internal fun NativeEagerGrid(cells: NativeGridCells, entries: kotlin.collections.List<NativeGridEntry>, vertical: Boolean,
    spacing: Double, horizontalAlignment: Alignment.Horizontal, verticalAlignment: Alignment.Vertical,
    modifier: Modifier) {
    Layout(modifier = modifier, content = {
        entries.forEachIndexed { index, entry -> key(entry.identity ?: index) { Box { entry.content() } } }
    }) { measurables, constraints ->
        val crossAvailable = if (vertical) constraints.maxWidth else constraints.maxHeight
        val slots = cells.resolve(crossAvailable, this)
        if (slots.isEmpty()) return@Layout layout(constraints.minWidth, constraints.minHeight) {}
        val usedCross = slots.sumOf { it.size + it.gap }
        val gap = (spacing * density).roundToInt()
        data class Row(val first: Int, val last: Int, val fullSpan: Boolean)
        val rows = mutableListOf<Row>()
        var start = 0
        var index = 0
        while (index < entries.size) {
            if (entries[index].fullSpan) {
                if (start < index) rows += Row(start, index, false)
                rows += Row(index, index + 1, true)
                index++
                start = index
            } else {
                index++
                if (index - start == slots.size) { rows += Row(start, index, false); start = index }
            }
        }
        if (start < index) rows += Row(start, index, false)
        val children = arrayOfNulls<androidx.compose.ui.layout.Placeable>(measurables.size)
        val rowSizes = rows.map { row ->
            var maximum = 0
            for (item in row.first until row.last) {
                val size = if (row.fullSpan) crossAvailable else slots[item - row.first].size
                val childConstraints = if (vertical) Constraints(maxWidth = size) else Constraints(maxHeight = size)
                val child = measurables[item].measure(childConstraints)
                children[item] = child
                maximum = maxOf(maximum, if (vertical) child.height else child.width)
            }
            maximum
        }
        val mainSize = rowSizes.sum() + gap * (rows.size - 1).coerceAtLeast(0)
        val width = constraints.constrainWidth(if (vertical) crossAvailable else mainSize)
        val height = constraints.constrainHeight(if (vertical) mainSize else crossAvailable)
        layout(width, height) {
            val crossOrigin = if (vertical) horizontalAlignment.align(usedCross, width, layoutDirection)
                else verticalAlignment.align(usedCross, height)
            var main = 0
            rows.forEachIndexed { rowIndex, row ->
                var cross = crossOrigin
                for (item in row.first until row.last) {
                    val child = children[item]!!
                    val slot = if (row.fullSpan) NativeGridSlot(crossAvailable, 0, Alignment.Center) else slots[item - row.first]
                    val space = if (vertical) IntSize(slot.size, rowSizes[rowIndex]) else IntSize(rowSizes[rowIndex], slot.size)
                    val offset = slot.alignment.align(IntSize(child.width, child.height), space, layoutDirection)
                    if (vertical) {
                        val physicalCross = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl)
                            width - cross - slot.size else cross
                        child.place((if (row.fullSpan) 0 else physicalCross) + offset.x, main + offset.y)
                    } else {
                        child.placeRelative(main + offset.x, (if (row.fullSpan) 0 else cross) + offset.y)
                    }
                    cross += slot.size + slot.gap
                }
                main += rowSizes[rowIndex] + gap
            }
        }
    }
}
