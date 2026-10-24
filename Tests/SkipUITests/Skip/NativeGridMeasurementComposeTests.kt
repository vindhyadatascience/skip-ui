// Copyright 2026 Vindhya Data Science
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class NativeGridMeasurementComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun pairCells(alignment: Alignment) = NativeGridCells(skip.lib.arrayOf(
        GridItem(GridItem.Size.fixed(40.0), spacing = 8.0, alignment = alignment),
        GridItem(GridItem.Size.fixed(40.0), spacing = 0.0, alignment = Alignment.top)))

    @Test fun adaptiveUsesItsFullShareBeforeApplyingPerCellMaximum() {
        val cells = NativeGridCells(skip.lib.arrayOf(GridItem(GridItem.Size.adaptive(60.0, 100.0), spacing = 8.0)))
        val slots = cells.resolve(240, Density(1f))
        assertEquals(listOf(75, 74, 75), slots.map { it.size })
        assertEquals(listOf(8, 8, 0), slots.map { it.gap })
        var position = 0
        assertEquals(listOf(0, 83, 165), slots.map { val start = position; position += it.size + it.gap; start })
        val capped = NativeGridCells(skip.lib.arrayOf(GridItem(GridItem.Size.adaptive(60.0, 80.0), spacing = 8.0)))
        assertEquals(listOf(80), capped.resolve(120, Density(1f)).map { it.size })
    }

    private fun mountPair(alignment: Alignment, tall: MutableIntState, count: MutableIntState = mutableIntStateOf(2)) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val cells = remember { pairCells(alignment) }
                LazyVerticalGrid(columns = cells, modifier = Modifier.width(88.dp).height(100.dp)) {
                    items(count.intValue) { index ->
                        NativeGridCell(cells, index, vertical = true) {
                            Box(Modifier.fillMaxWidth().height((if (index == 0) 10 else tall.intValue).dp)
                                .testTag(if (index == 0) "short" else "tall"))
                        }
                    }
                }
            }
        }
    }

    private fun relativeY(short: String, tall: String): Float {
        rule.waitForIdle()
        val first = rule.onNodeWithTag(short).fetchSemanticsNode().layoutInfo.coordinates.positionInRoot().y
        val second = rule.onNodeWithTag(tall).fetchSemanticsNode().layoutInfo.coordinates.positionInRoot().y
        return first - second
    }

    @Test fun centerAlignmentUsesPeerNaturalHeightAndCanShrink() {
        val tall = mutableIntStateOf(40)
        mountPair(Alignment.center, tall)
        assertEquals(15f, relativeY("short", "tall"), 0.1f)
        rule.runOnIdle { tall.intValue = 20 }
        assertEquals(5f, relativeY("short", "tall"), 0.1f)
        rule.onNodeWithTag("short").assertHeightIsEqualTo(10.dp)
    }

    @Test fun bottomAlignmentUsesPeerNaturalHeightAndCanShrink() {
        val tall = mutableIntStateOf(40)
        mountPair(Alignment.bottom, tall)
        assertEquals(30f, relativeY("short", "tall"), 0.1f)
        rule.runOnIdle { tall.intValue = 20 }
        assertEquals(10f, relativeY("short", "tall"), 0.1f)
    }

    @Test fun removingTheTallPeerDoesNotRetainItsOldHeight() {
        val count = mutableIntStateOf(2)
        val tall = mutableIntStateOf(40)
        mountPair(Alignment.center, tall, count)
        assertEquals(15f, relativeY("short", "tall"), 0.1f)
        val before = rule.onNodeWithTag("tall").fetchSemanticsNode().layoutInfo.coordinates.positionInRoot().y
        rule.runOnIdle { count.intValue = 1 }
        rule.waitForIdle()
        rule.onNodeWithTag("tall").assertDoesNotExist()
        val short = rule.onNodeWithTag("short").fetchSemanticsNode().layoutInfo.coordinates.positionInRoot().y
        assertEquals(before, short, 0.1f)
    }

    @Test fun fullSpanSectionsWithResetIndicesHaveIndependentPeers() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val cells = remember { pairCells(Alignment.center) }
                LazyVerticalGrid(columns = cells, modifier = Modifier.width(88.dp).height(120.dp)) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.height(4.dp)) }
                    items(2) { index -> NativeGridCell(cells, index, true, rowGroup = 0) {
                        Box(Modifier.fillMaxWidth().height((if (index == 0) 10 else 40).dp).testTag("first-$index"))
                    } }
                    item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.height(4.dp)) }
                    items(2) { index -> NativeGridCell(cells, index, true, rowGroup = 1) {
                        Box(Modifier.fillMaxWidth().height((if (index == 0) 10 else 20).dp).testTag("second-$index"))
                    } }
                }
            }
        }
        assertEquals(15f, relativeY("first-0", "first-1"), 0.1f)
        assertEquals(5f, relativeY("second-0", "second-1"), 0.1f)
    }

    @Test fun horizontalGridUsesPeerNaturalWidth() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val cells = remember { pairCells(Alignment.center) }
                LazyHorizontalGrid(rows = cells, modifier = Modifier.width(100.dp).height(88.dp)) {
                    items(2) { index -> NativeGridCell(cells, index, false) {
                        Box(Modifier.fillMaxHeight().width((if (index == 0) 10 else 40).dp).testTag("horizontal-$index"))
                    } }
                }
            }
        }
        rule.waitForIdle()
        val short = rule.onNodeWithTag("horizontal-0").fetchSemanticsNode().layoutInfo.coordinates.positionInRoot().x
        val tall = rule.onNodeWithTag("horizontal-1").fetchSemanticsNode().layoutInfo.coordinates.positionInRoot().x
        assertEquals(15f, short - tall, 0.1f)
    }
}
