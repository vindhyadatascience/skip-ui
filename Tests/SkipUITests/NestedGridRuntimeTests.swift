// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class NestedGridRuntimeTests: SkipUITestCase {
    func testPublicNestedAndSoleGridProbes() {
        _ = NestedGridRuntimeProbe()
        _ = NestedGridImplicitIDScrollProbe()
        _ = SoleLazyGridRuntimeProbe(counter: GridBodyCounter())
    }
}

struct NestedGridRuntimeProbe: View {
    @State private var count = 18
    @State private var threeColumns = false

    var body: some View {
        VStack {
            Button("Add grid items") { count += 6 }
            Button("Change grid columns") { threeColumns.toggle() }
            ScrollView {
                VStack(spacing: 10.0) {
                    Text("Before grid").frame(height: 20.0)
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8.0), count: threeColumns ? 3 : 2), spacing: 4.0) {
                        ForEach(0..<count, id: \.self) { index in
                            Text("Grid item \(index)")
                                .frame(maxWidth: .infinity, minHeight: 30.0, maxHeight: 30.0)
                                .accessibilityIdentifier("nested-grid-item-\(index)")
                        }
                    }
                    .accessibilityIdentifier("nested-grid")
                    Text("After complete grid").frame(height: 20.0).accessibilityIdentifier("grid-footer")
                }
            }
            .frame(width: 240.0, height: 160.0)
            .accessibilityIdentifier("nested-grid-scroll")
        }
    }
}

final class GridBodyCounter { var bodies = 0 }
struct GridCountedCell: View {
    let index: Int
    let counter: GridBodyCounter
    var body: some View {
        counter.bodies += 1
        return Text("Lazy item \(index)").frame(height: 30.0)
    }
}
struct SoleLazyGridRuntimeProbe: View {
    let counter: GridBodyCounter
    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())]) {
                ForEach(0..<500, id: \.self) { index in GridCountedCell(index: index, counter: counter) }
            }
            .accessibilityIdentifier("sole-lazy-grid")
        }.frame(width: 240.0, height: 160.0)
    }
}

struct NestedGridImplicitIDScrollProbe: View {
    var body: some View {
        ScrollViewReader { proxy in
            VStack {
                Button("Scroll to last implicit grid ID") { proxy.scrollTo(39) }
                ScrollView {
                    VStack(spacing: 10.0) {
                        Text("Before implicit grid")
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 4.0) {
                            ForEach(0..<40, id: \.self) { index in
                                Text("Implicit grid item \(index)").frame(height: 30.0)
                            }
                        }
                        Text("After implicit grid")
                    }
                }.frame(width: 240.0, height: 160.0)
            }
        }
    }
}
