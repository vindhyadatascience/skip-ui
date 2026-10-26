// Copyright 2026 Vindhya Data Science
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
final class CoordinateSpaceIdentityTests: XCTestCase {
    func testPublicNamedLiteralAndWrappedNamesHaveTheSameIdentity() {
        let literal = NamedCoordinateSpace.named("literal")
        let wrapped = NamedCoordinateSpace.named(("literal" as AnyHashable))
        XCTAssertEqual(literal.coordinateSpace, wrapped.coordinateSpace)
        XCTAssertEqual(literal.coordinateSpace, CoordinateSpace.named(("literal" as AnyHashable)))
        XCTAssertFalse(literal.coordinateSpace.isGlobal)
        XCTAssertFalse(literal.coordinateSpace.isLocal)
    }

    func testAxisScrollSpacesAreStableAndDistinct() {
        let vertical = NamedCoordinateSpace.scrollView(axis: .vertical)
        let repeated = NamedCoordinateSpace.scrollView(axis: .vertical)
        let horizontal = NamedCoordinateSpace.scrollView(axis: .horizontal)
        XCTAssertEqual(vertical.coordinateSpace, repeated.coordinateSpace)
        XCTAssertNotEqual(vertical.coordinateSpace, horizontal.coordinateSpace)
        XCTAssertFalse(vertical.coordinateSpace.isGlobal)
        XCTAssertFalse(vertical.coordinateSpace.isLocal)
    }

    func testUnqualifiedScrollSpaceIsStableAndNamed() {
        let first = NamedCoordinateSpace.scrollView
        let second = NamedCoordinateSpace.scrollView
        XCTAssertEqual(first.coordinateSpace, second.coordinateSpace)
        XCTAssertFalse(first.coordinateSpace.isGlobal)
        XCTAssertFalse(first.coordinateSpace.isLocal)
    }
}
