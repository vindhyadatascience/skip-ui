// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import Foundation
import XCTest

#if SKIP
@testable import SkipUI
#endif

/// VDS: a long dropdown is capped to the larger side of its anchor so Material keeps it anchored.
final class DropdownMenuHeightLimitTests: SkipUITestCase {
    #if SKIP
    func testCapIsTheLargerSideOfTheAnchor() {
        // A chip low on a phone: more room above than below, so the menu caps to the space above.
        XCTAssertEqual(dropdownMenuMaxHeight(anchorTop: 1500, anchorBottom: 1605, visibleTop: 63, visibleHeight: 2277, marginPx: 126), 1310)
        // A chip high on the window: the space below wins.
        XCTAssertEqual(dropdownMenuMaxHeight(anchorTop: 300, anchorBottom: 400, visibleTop: 63, visibleHeight: 2277, marginPx: 126), 1750)
    }

    func testNoRoomOnEitherSideMeansNoCap() {
        XCTAssertEqual(dropdownMenuMaxHeight(anchorTop: 100, anchorBottom: 2200, visibleTop: 0, visibleHeight: 2277, marginPx: 126), -1)
    }
    #endif
}
