// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class NavigationErasedKeyRuntimeTests: SkipUITestCase {
    func testUnrelatedRouteClassesWithEqualValuesAndHashesStayDistinctWhenErased() {
        let first = FirstErasedNavigationRoute(value: 7)
        let second = SecondErasedNavigationRoute(value: 7)
        XCTAssertEqual(first.hashValue, second.hashValue)
        XCTAssertNotEqual((first as AnyHashable), (second as AnyHashable))
        _ = NavigationErasedKeyRuntimeProbe()
    }
}

final class FirstErasedNavigationRoute: Hashable {
    let value: Int
    init(value: Int) { self.value = value }
    static func == (lhs: FirstErasedNavigationRoute, rhs: FirstErasedNavigationRoute) -> Bool { lhs.value == rhs.value }
    func hash(into hasher: inout Hasher) { hasher.combine(value) }
}
final class SecondErasedNavigationRoute: Hashable {
    let value: Int
    init(value: Int) { self.value = value }
    static func == (lhs: SecondErasedNavigationRoute, rhs: SecondErasedNavigationRoute) -> Bool { lhs.value == rhs.value }
    func hash(into hasher: inout Hasher) { hasher.combine(value) }
}
class BaseErasedNavigationRoute: Hashable {
    let value: Int
    init(value: Int) { self.value = value }
    static func == (lhs: BaseErasedNavigationRoute, rhs: BaseErasedNavigationRoute) -> Bool { lhs.value == rhs.value }
    func hash(into hasher: inout Hasher) { hasher.combine(value) }
}
final class ChildErasedNavigationRoute: BaseErasedNavigationRoute {
    override init(value: Int) { super.init(value: value) }
}

struct NavigationErasedKeyRuntimeProbe: View {
    var body: some View {
        NavigationStack {
            VStack {
                NavigationLink("Open first equal-hash class", value: FirstErasedNavigationRoute(value: 7))
                NavigationLink("Open second equal-hash class", value: SecondErasedNavigationRoute(value: 7))
                NavigationLink("Open subclass through base type", value: ChildErasedNavigationRoute(value: 9))
            }
            .navigationDestination(for: FirstErasedNavigationRoute.self) { route in
                ErasedNavigationDestinationProbe(label: "First route \(route.value)")
            }
            .navigationDestination(for: SecondErasedNavigationRoute.self) { route in
                ErasedNavigationDestinationProbe(label: "Second route \(route.value)")
            }
            .navigationDestination(for: BaseErasedNavigationRoute.self) { route in
                ErasedNavigationDestinationProbe(label: "Base route \(route.value)")
            }
        }
    }
}
struct ErasedNavigationDestinationProbe: View {
    let label: String
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack {
            Text(label)
            Button("Back from erased route") { dismiss() }
        }
    }
}
