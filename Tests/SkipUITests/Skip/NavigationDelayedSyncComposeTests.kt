// Copyright 2026 Vindhya Data Science
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/// A stack keeps its entries' states until one second after its back stack last changed, then
/// drops those of popped entries. That sync used the back stack as it was when the second began,
/// so an entry pushed in the frame before it ran lost its state and the next composition
/// discarded it as unrestorable. The pushing view had to compose again to push it a second time,
/// as a new entry; when it did not, the destination was gone. GeneFox (RNABrowser-ghim): the
/// expression screen pushed from a GEO dataset that had just loaded was rebuilt on a layout
/// switch, and sometimes dropped.
@RunWith(AndroidJUnit4::class)
class NavigationDelayedSyncComposeTests {
    @get:Rule val rule = createComposeRule()

    @Test fun anEntryPushedJustBeforeTheDelayedSyncKeepsItsPlace() {
        val count = 16
        val items = List(count) { mutableStateOf<String?>(null) }
        val navigators = arrayOfNulls<Navigator>(count)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val context = remember { ComposeContext() }
            Column {
                items.forEachIndexed { index, item ->
                    Box(Modifier.height(24.dp)) {
                        NavigationStack(root = {
                            ComposeView(content = { _ -> navigators[index] = LocalNavigator.current })
                                .navigationDestination(
                                    item = Binding(get = { item.value }, set = { item.value = it }),
                                    destination = { value: String -> Text(verbatim = "Pushed $value") })
                        }).Compose(context = context)
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        // Every stack schedules its sync on its first composition. Push one stack's item per
        // frame across the end of that second, so one push lands in the frame before the sync.
        rule.mainClock.advanceTimeBy(1000L - 8L * 16L)
        for (index in 0 until count) {
            items[index].value = "$index"
            Snapshot.sendApplyNotifications()
            rule.mainClock.advanceTimeByFrame()
        }
        rule.mainClock.autoAdvance = true
        rule.mainClock.advanceTimeBy(3000L)
        rule.waitForIdle()
        val firstPush = Navigator.route(for_ = Navigator.destinationCount - 1, valueString = "0")
        val kept = (0 until count).map { navigators[it]?.isViewPresented(id = firstPush, asTop = true) == true }
        assertEquals("A stack discarded its first push and pushed the destination again", List(count) { true }, kept)
        for (index in 0 until count) {
            rule.onNodeWithText("Pushed $index").assertExists()
        }
    }
}
