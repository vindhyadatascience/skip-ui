// Copyright 2026 Vindhya Data Science
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import org.junit.Assert.*
import org.junit.Test

class CoordinateSpaceReservedIdentityTests {
    @Test fun reservedScrollNamesRetainTheirExactStringPayloads() {
        val plain = NamedCoordinateSpace.scrollView.coordinateSpace as CoordinateSpace.NamedCase
        val horizontal = NamedCoordinateSpace.scrollView(axis = Axis.horizontal).coordinateSpace as CoordinateSpace.NamedCase
        val vertical = NamedCoordinateSpace.scrollView(axis = Axis.vertical).coordinateSpace as CoordinateSpace.NamedCase
        assertEquals("_scrollView_", plain.associated0)
        assertEquals("_scrollView_axis_1_", horizontal.associated0)
        assertEquals("_scrollView_axis_2_", vertical.associated0)
        assertNotEquals(horizontal, vertical)
    }

    @Test fun existingBridgedGlobalLocalAndNamedRoutesRemainExact() {
        assertTrue(CoordinateSpaceProtocolFrom(bridged = 0, name = "ignored").coordinateSpace.isGlobal)
        assertTrue(CoordinateSpaceProtocolFrom(bridged = 1, name = "ignored").coordinateSpace.isLocal)
        val fallback = CoordinateSpaceProtocolFrom(bridged = 2, name = null).coordinateSpace as CoordinateSpace.NamedCase
        assertEquals("", fallback.associated0)
        val named = CoordinateSpaceProtocolFrom(bridged = 2, name = "bridge").coordinateSpace as CoordinateSpace.NamedCase
        assertEquals("bridge", named.associated0)
    }
}
