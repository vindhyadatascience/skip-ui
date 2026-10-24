// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class GestureInvalidationTests: SkipUITestCase {
    func testStackedAndChangingGesturePublicAPIs() {
        _ = StackedGestureRuntimeProbe()
        _ = ChangingDragRuntimeProbe()
    }
}

struct StackedGestureRuntimeProbe: View {
    @State private var version = 0
    @State private var handledVersion = -1
    @State private var doubleTaps = 0
    @State private var simultaneousTaps = 0
    @State private var doubleEnabled = false

    var body: some View {
        VStack {
            Button("Next handler") { version += 1 }
            Button("Enable double tap") { doubleEnabled = true }
            Text("Handled \(handledVersion)")
            Text("Double \(doubleTaps)")
            Text("Simultaneous \(simultaneousTaps)")
            Color.red.frame(width: 120.0, height: 100.0)
                .accessibilityIdentifier("stacked-gesture-target")
                .onTapGesture { handledVersion = version }
                .gesture(TapGesture(count: 2).onEnded { doubleTaps += 1 }, including: doubleEnabled ? .all : .none)
                .simultaneousGesture(TapGesture().onEnded { simultaneousTaps += 1 })
        }
    }
}

struct ChangingDragRuntimeProbe: View {
    @State private var minimum = 10.0
    @State private var changes = 0
    @State private var ends = 0

    var body: some View {
        VStack {
            Button("Zero distance") { minimum = 0.0 }
            Button("Positive distance") { minimum = 10.0 }
            Text("Changes \(changes)")
            Text("Ends \(ends)")
            Color.blue.frame(width: 120.0, height: 100.0)
                .accessibilityIdentifier("changing-drag-target")
                .gesture(DragGesture(minimumDistance: minimum).onChanged { _ in changes += 1 }.onEnded { _ in ends += 1 })
        }
    }
}
