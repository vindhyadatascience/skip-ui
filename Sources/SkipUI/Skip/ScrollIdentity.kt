// Copyright 2026 Vindhya Data Science
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import skip.lib.swiftAnyHashableEquals

/** Both explicit .id values and lazy implicit IDs cross the same erased identity boundary.
 *  AnyHashable is a typealias again (RNABrowser-fiea.30); compare with its exact numeric equality. */
internal fun scrollIdentityEquals(lhs: Any, rhs: Any): Boolean = swiftAnyHashableEquals(lhs, rhs)
