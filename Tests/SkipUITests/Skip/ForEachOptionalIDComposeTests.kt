// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ForEachOptionalIDComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun collectionNilIDReachesTheCollectorAsARealIdentity() = assertCollector(
        OptionalIDForEachFactory().collection() as ForEach, nilIndex = 0
    )

    @Test fun rangeNilIDReachesTheCollectorAsARealIdentity() = assertCollector(
        OptionalIDForEachFactory().range() as ForEach, nilIndex = 2
    )

    @Test fun bindingNilIDReachesTheCollectorAsARealIdentity() = assertCollector(
        OptionalIDForEachFactory().binding(OptionalIDStorage()) as ForEach, nilIndex = 0
    )

    @Test fun listCollectionNilIDReachesTheCollectorAsARealIdentity() = assertCollector(
        requireNotNull((OptionalIDForEachFactory().listCollection() as List).forEach), nilIndex = 0
    )

    @Test fun listRangeNilIDReachesTheCollectorAsARealIdentity() = assertCollector(
        requireNotNull((OptionalIDForEachFactory().listRange() as List).forEach), nilIndex = 2
    )

    @Test fun listBindingNilIDReachesTheCollectorAsARealIdentity() = assertCollector(
        requireNotNull((OptionalIDForEachFactory().listBinding(OptionalIDStorage()) as List).forEach), nilIndex = 0
    )

    @Test fun nilBundleIdentityIsStableAndCannotCollideWithUserStrings() {
        val entry = OptionalIDStorage().values[0]
        val first = requireNotNull((OptionalIDForEachFactory().collection() as ForEach).identifier?.invoke(entry))
        val second = requireNotNull((OptionalIDForEachFactory().collection() as ForEach).identifier?.invoke(entry))
        val key = composeBundleString(first)
        assertEquals(key, composeBundleString(second))
        assertNotEquals(key, composeBundleString(key))
        assertNotEquals(composeBundleString(key), composeBundleString(key + key))
        assertEquals("11", composeBundleString(11))
    }

    private fun assertCollector(loop: ForEach, nilIndex: Int) {
        val collector = LazyItemCollector()
        collector.initialize(
            startItemIndex = 0,
            item = { _, _ -> },
            indexedItems = { _, _, _, _, _, _, _ -> },
            objectItems = { _, _, _, _, _, _, _ -> },
            objectBindingItems = { _, _, _, _, _, _, _, _ -> },
            sectionHeader = { _, _ -> 1 },
            sectionFooter = { _, _, _ -> 1 }
        )
        loop.produceLazyItems(collector, emptyList(), 0)
        assertEquals(3, collector.count)
        val ids = (0 until 3).map { collector.id(for_ = it) }
        assertNotNull("Optional.none must be an erased ID, not a missing ID", ids[nilIndex])
        assertEquals("All three source IDs are distinct", 3, ids.toSet().size)
        for (index in 0 until 3) {
            val id = ids[index]
            assertNotNull("Source ID at $index must exist", id)
            if (id != null) assertEquals(index, collector.index(for_ = id))
        }
        assertNull("Out-of-range absence remains distinct from a nil-valued ID", collector.id(for_ = 3))
    }

    @Test fun collectionNilIDKeepsItsStateAfterReordering() = assertRowIdentity(mode = 0)
    @Test fun rangeNilIDKeepsItsStateWhenTheRangeSlides() = assertRowIdentity(mode = 1)
    @Test fun bindingNilIDKeepsItsStateAfterReordering() = assertRowIdentity(mode = 2)

    @Test fun listCollectionNilIDKeepsItsStateAfterReordering() = assertRowIdentity(mode = 0, list = true)
    @Test fun listRangeNilIDKeepsItsStateWhenTheRangeSlides() = assertRowIdentity(mode = 1, list = true)
    @Test fun listBindingNilIDKeepsItsStateAfterReordering() = assertRowIdentity(mode = 2, list = true)

    private fun assertRowIdentity(mode: Int, list: Boolean = false) {
        rule.setContent {
            if (list) OptionalIDListProbe(mode).Compose(ComposeContext())
            else OptionalIDForEachProbe(mode).Compose(ComposeContext())
        }
        rule.onNodeWithText("nil count 0").performClick()
        rule.onNodeWithText("nil count 1").assertIsDisplayed()
        repeat(2) {
            rule.onNodeWithText("Move optional IDs").performClick()
            rule.waitForIdle()
            rule.onNodeWithText("nil count 1").assertIsDisplayed()
        }
        rule.onNodeWithText("nil count 1").performClick()
        rule.onNodeWithText("nil count 2").assertIsDisplayed()
    }
}
