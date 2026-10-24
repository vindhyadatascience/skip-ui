// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

/// Optional.none is a valid generic SwiftUI ID, distinct from an absent identifier callback.
final class ForEachOptionalIDTests: SkipUITestCase {
    func testPublicCollectionRangeAndBindingAcceptOptionalIDs() {
        let factory = OptionalIDForEachFactory()
        _ = factory.collection()
        _ = factory.range()
        _ = factory.binding(OptionalIDStorage())
        _ = OptionalIDForEachProbe(mode: 0)
        _ = OptionalIDForEachProbe(mode: 1)
        _ = OptionalIDForEachProbe(mode: 2)

        #if !SKIP
        // Pin the native reference API's actual ID type, including its nil element.
        let native: SwiftUI.ForEach<[OptionalIDEntry], Int?, OptionalIDStatefulRow> = SwiftUI.ForEach(
            OptionalIDStorage().values, id: \.identity
        ) { entry in OptionalIDStatefulRow(label: entry.label) }
        XCTAssertEqual(native.data.map { $0.identity }, [nil, 11, 22])
        #endif
    }

    func testPublicListCollectionRangeAndBindingAcceptOptionalIDs() {
        let factory = OptionalIDForEachFactory()
        _ = factory.listCollection()
        _ = factory.listRange()
        _ = factory.listBinding(OptionalIDStorage())
        _ = OptionalIDListProbe(mode: 0)
        _ = OptionalIDListProbe(mode: 1)
        _ = OptionalIDListProbe(mode: 2)

        #if !SKIP
        let native: SwiftUI.List<Never, SwiftUI.ForEach<[OptionalIDEntry], Int?, OptionalIDStatefulRow>> = SwiftUI.List(
            OptionalIDStorage().values, id: \.identity
        ) { entry in OptionalIDStatefulRow(label: entry.label) }
        _ = native
        #endif
    }
}

struct OptionalIDEntry {
    var identity: Int?
    let label: String
}

final class OptionalIDStorage {
    var values = [OptionalIDEntry(identity: nil, label: "nil"),
                  OptionalIDEntry(identity: 11, label: "eleven"),
                  OptionalIDEntry(identity: 22, label: "twenty-two")]
}

extension Int {
    var optionalForEachID: Int? { self == 2 ? nil : self }
}

/// Calls the real public overloads in Swift. JVM tests inspect the resulting SDK loop/collector.
struct OptionalIDForEachFactory {
    func collection() -> some View {
        ForEach(OptionalIDStorage().values, id: \.identity) { entry in
            OptionalIDStatefulRow(label: entry.label)
        }
    }

    func range() -> some View {
        ForEach(0..<3, id: \.optionalForEachID) { value in
            OptionalIDStatefulRow(label: value == 2 ? "nil" : "value \(value)")
        }
    }

    func binding(_ storage: OptionalIDStorage) -> some View {
        let values = Binding<[OptionalIDEntry]>(get: { storage.values }, set: { storage.values = $0 })
        return ForEach(values, id: \.identity) { entry in
            OptionalIDStatefulRow(label: entry.wrappedValue.label)
        }
    }

    func listCollection() -> some View {
        List(OptionalIDStorage().values, id: \.identity) { entry in
            OptionalIDStatefulRow(label: entry.label)
        }
    }

    func listRange() -> some View {
        List(0..<3, id: \.optionalForEachID) { value in
            OptionalIDStatefulRow(label: value == 2 ? "nil" : "value \(value)")
        }
    }

    func listBinding(_ storage: OptionalIDStorage) -> some View {
        let values = Binding<[OptionalIDEntry]>(get: { storage.values }, set: { storage.values = $0 })
        return List(values, id: \.identity) { entry in
            OptionalIDStatefulRow(label: entry.wrappedValue.label)
        }
    }
}

struct OptionalIDStatefulRow: View {
    let label: String
    @State private var count = 0

    var body: some View {
        Button("\(label) count \(count)") { count += 1 }
    }
}

/// Reordering (or sliding a range) moves the nil-ID row to a different positional slot.
struct OptionalIDForEachProbe: View {
    let mode: Int
    @State private var values = OptionalIDStorage().values
    @State private var start = 0

    var body: some View {
        VStack {
            Button("Move optional IDs") {
                values = [values[2], values[1], values[0]]
                start = start == 0 ? 2 : 0
            }
            if mode == 0 {
                ForEach(values, id: \.identity) { entry in
                    OptionalIDStatefulRow(label: entry.label)
                }
            } else if mode == 1 {
                ForEach(start..<(start + 3), id: \.optionalForEachID) { value in
                    OptionalIDStatefulRow(label: value == 2 ? "nil" : "value \(value)")
                }
            } else {
                ForEach($values, id: \.identity) { entry in
                    OptionalIDStatefulRow(label: entry.wrappedValue.label)
                }
            }
        }
    }
}

struct OptionalIDListProbe: View {
    let mode: Int
    @State private var values = OptionalIDStorage().values
    @State private var start = 0

    var body: some View {
        VStack {
            Button("Move optional IDs") {
                values = [values[2], values[1], values[0]]
                start = start == 0 ? 2 : 0
            }
            if mode == 0 {
                List(values, id: \.identity) { entry in
                    OptionalIDStatefulRow(label: entry.label)
                }
            } else if mode == 1 {
                List(start..<(start + 3), id: \.optionalForEachID) { value in
                    OptionalIDStatefulRow(label: value == 2 ? "nil" : "value \(value)")
                }
            } else {
                List($values, id: \.identity) { entry in
                    OptionalIDStatefulRow(label: entry.wrappedValue.label)
                }
            }
        }
    }
}
