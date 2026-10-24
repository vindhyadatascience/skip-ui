// Copyright 2026 Vindhya Data Science
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import skip.lib.AnyHashable
import skip.lib.anyHashableOptional

class ScrollIdentityTests {
    @Test fun rawAndWrappedIDsMatchInBothDirections() {
        assertTrue(scrollIdentityEquals(39, AnyHashable(39)))
        assertTrue(scrollIdentityEquals(AnyHashable(39), 39))
        assertTrue(scrollIdentityEquals(AnyHashable(39), AnyHashable(39)))
        assertTrue(scrollIdentityEquals("target", AnyHashable("target")))
        assertFalse(scrollIdentityEquals("39", AnyHashable(39)))
        assertFalse(scrollIdentityEquals(40, AnyHashable(39)))
    }
    @Test fun numericIDsUseTheRuntimeExactHashableEquality() {
        assertTrue(scrollIdentityEquals(39, AnyHashable(39.0)))
        assertTrue(scrollIdentityEquals(AnyHashable(-0.0), 0.0f))
        assertFalse(scrollIdentityEquals(9_007_199_254_740_993L, AnyHashable(9_007_199_254_740_992.0)))
        assertFalse(scrollIdentityEquals(Double.NaN, AnyHashable(Double.NaN)))
    }
    @Test fun aTypedNilIdentityRemainsAValueWithItsWrappedType() {
        val first = anyHashableOptional(null, "Int")
        assertTrue(scrollIdentityEquals(first, anyHashableOptional(null, "Int")))
        assertFalse(scrollIdentityEquals(first, anyHashableOptional(null, "String")))
        assertFalse(scrollIdentityEquals(first, "nil"))
    }
}
