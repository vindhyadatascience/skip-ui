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
                // A flexible leading column beside a fixed-width one, as a page's figure and inspector are.
                VStack(alignment: .leading) {
                    Text("Leading child")
                }
                .frame(maxWidth: .infinity, alignment: .topLeading)
                // In a column of its own, as a page's inspector is: the column renders (and so
                // evaluates) the child, so only a moved composition keeps the child's state.
                VStack {
                    AnyLayoutStatefulChild()
                }
                .frame(width: wide ? 120.0 : nil, alignment: .topLeading)
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

/// The analysis page's shape: in a vertical scroll, a flexible column beside a much taller fixed one.
struct AnyLayoutScrollProbe: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12.0) {
                AnyLayout(HStackLayout(alignment: .top, spacing: 24.0)).callAsFunction {
                    VStack(alignment: .leading, spacing: 12.0) {
                        VStack(alignment: .leading, spacing: 12.0) {
                            Text("Short header")
                        }
                        .frame(maxWidth: 200.0, alignment: .leading)
                        Text("Figure")
                    }
                    .frame(maxWidth: .infinity, alignment: .topLeading)
                    VStack(alignment: .leading, spacing: 12.0) {
                        Text("Inspector top")
                        Color.gray.frame(height: 400.0)
                    }
                    .frame(width: 120.0, alignment: .topLeading)
                }
                VStack(alignment: .leading) {
                    Text("Below")
                }
                .frame(maxWidth: .infinity, alignment: .topLeading)
            }
        }
    }
}
