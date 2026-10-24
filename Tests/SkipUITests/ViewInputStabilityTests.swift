// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class ViewInputStabilityTests: SkipUITestCase {
    func testPublicParentAndChildProbe() {
        _ = ViewInputStabilityProbe(unchanged: ViewBodyCounter(), changed: ViewBodyCounter())
    }
}

final class ViewBodyCounter { var bodies = 0 }
struct CountedValueChild: View {
    let value: Int
    let counter: ViewBodyCounter
    var body: some View {
        counter.bodies += 1
        return Text("Child value \(value)")
    }
}
struct ViewInputStabilityProbe: View {
    let unchanged: ViewBodyCounter
    let changed: ViewBodyCounter
    @State private var tick = 0
    var body: some View {
        VStack {
            Button("Invalidate only parent input") { tick += 1 }
            Text("Parent \(tick)")
            CountedValueChild(value: 42, counter: unchanged)
            CountedValueChild(value: tick, counter: changed)
        }
    }
}
