// Copyright 2026 Vindhya Data Science
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import skip.lib.AnyHashable

class ScrollIdentityTests {
    private fun erasedID(value: AnyHashable): AnyHashable = value

    @Test fun rawAndErasedIDsMatchInBothDirections() {
        assertTrue(scrollIdentityEquals(39, erasedID(39)))
        assertTrue(scrollIdentityEquals(erasedID(39), 39))
        assertTrue(scrollIdentityEquals(erasedID(39), erasedID(39)))
        assertTrue(scrollIdentityEquals("target", erasedID("target")))
        assertFalse(scrollIdentityEquals("39", erasedID(39)))
        assertFalse(scrollIdentityEquals(40, erasedID(39)))
    }
    @Test fun numericIDsUseTheRuntimeExactHashableEquality() {
        assertTrue(scrollIdentityEquals(39, erasedID(39.0)))
        assertTrue(scrollIdentityEquals(erasedID(-0.0), 0.0f))
        assertFalse(scrollIdentityEquals(9_007_199_254_740_993L, erasedID(9_007_199_254_740_992.0)))
        assertFalse(scrollIdentityEquals(Double.NaN, erasedID(Double.NaN)))
    }

}
