// Copyright 2026 Vindhya Data Science
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import skip.lib.*

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class NavigationDebounceTests {
    private fun navigator() = Navigator(NavBackStack<NavKey>(SkipNavigationStackRootKey.root), dictionaryOf(), null)

    @Test fun independentStacksAndBackNavigationHaveIndependentTapWindows() {
        val first = navigator()
        val second = navigator()
        assertTrue(first.permitLinkNavigation(at = 10.0))
        assertFalse(first.permitLinkNavigation(at = 10.1))
        assertTrue(second.permitLinkNavigation(at = 10.1))
        first.navigateBack()
        assertTrue(first.permitLinkNavigation(at = 10.1))
        assertFalse(first.permitLinkNavigation(at = 10.2))
        assertTrue(first.permitLinkNavigation(at = 10.5))
    }

    @Test fun aClockCorrectionDoesNotPermanentlyBlockNavigation() {
        val value = navigator()
        assertTrue(value.permitLinkNavigation(at = 10.0))
        assertTrue(value.permitLinkNavigation(at = 9.0))
        assertFalse(value.permitLinkNavigation(at = 9.1))
        assertTrue(value.permitLinkNavigation(at = 9.5))
    }
    @Test fun erasedClassDestinationsPushTheirActualRegisteredRoutes() {
        val stack = NavBackStack<NavKey>(SkipNavigationStackRootKey.root)
        val first = FirstErasedNavigationRoute(7)
        val second = SecondErasedNavigationRoute(7)
        val destinations = dictionaryOf(
            Tuple2<AnyHashable, NavigationDestination>(FirstErasedNavigationRoute::class, NavigationDestination(destination = { Text("First") })),
            Tuple2<AnyHashable, NavigationDestination>(SecondErasedNavigationRoute::class, NavigationDestination(destination = { Text("Second") })))
        val value = Navigator(stack, destinations, null)
        value.navigate(to = first)
        assertEquals(2, stack.size)
        val firstState = value.state(forPushKey = stack.last() as SkipNavigationStackPushKey)
        assertNotNull(firstState)
        assertSame(first, firstState?.targetValue)
        value.navigateBack()
        assertEquals(1, stack.size)
        value.navigate(to = second)
        assertEquals(2, stack.size)
        val secondState = value.state(forPushKey = stack.last() as SkipNavigationStackPushKey)
        assertNotNull(secondState)
        assertSame(second, secondState?.targetValue)
    }

    @Test fun subclassValuesFindTheirRegisteredSuperclassBeforePushing() {
        val stack = NavBackStack<NavKey>(SkipNavigationStackRootKey.root)
        val child = ChildErasedNavigationRoute(9)
        val destinations = dictionaryOf(Tuple2<AnyHashable, NavigationDestination>(BaseErasedNavigationRoute::class,
            NavigationDestination(destination = { Text("Base") })))
        val value = Navigator(stack, destinations, null)
        value.navigate(to = child)
        assertEquals(2, stack.size)
        val state = value.state(forPushKey = stack.last() as SkipNavigationStackPushKey)
        assertNotNull(state)
        assertSame(child, state?.targetValue)
    }

}
