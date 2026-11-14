// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class AnyLayoutTests: SkipUITestCase {
    func testStackLayoutsBuildOnEveryFace() {
        _ = AnyLayoutIdentityProbe()
        _ = AnyLayout(HStackLayout()).callAsFunction { Text("row") }
        _ = AnyLayout(VStackLayout(alignment: .trailing, spacing: 2.0)).callAsFunction { Text("column") }
    }
}

/// A stateful child, with a sheet it presents, inside a layout that changes axis.
struct AnyLayoutIdentityProbe: View {
    @State private var wide = false

    private var layout: AnyLayout {
        wide ? AnyLayout(HStackLayout(alignment: .top, spacing: 4.0)) : AnyLayout(VStackLayout(alignment: .leading, spacing: 4.0))
    }

    var body: some View {
        VStack {
            Button("Switch layout") { wide.toggle() }
            // Spelled out: Skip resolves a View-building call by its declaration, not a value call.
            layout.callAsFunction {
                Text("Leading child")
                // In a column of its own, as a page's inspector is: the column renders (and so
                // evaluates) the child, so only a moved composition keeps the child's state.
                VStack {
                    AnyLayoutStatefulChild()
                }
            }
        }
    }
}

struct AnyLayoutStatefulChild: View {
    @State private var count = 0
    @State private var showsSheet = false

    var body: some View {
        VStack {
            Button("Count \(count)") { count += 1 }
            Button("Show sheet") { showsSheet = true }
        }
        .sheet(isPresented: $showsSheet) {
            Text("Sheet body")
        }
    }
}
