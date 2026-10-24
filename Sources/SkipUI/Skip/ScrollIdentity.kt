// Copyright 2026 Vindhya Data Science
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import skip.lib.AnyHashable

/** Both explicit .id values and lazy implicit IDs cross the same erased identity boundary. */
internal fun scrollIdentityEquals(lhs: Any, rhs: Any): Boolean = AnyHashable(lhs) == AnyHashable(rhs)
