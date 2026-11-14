// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class ListTabStopTests: SkipUITestCase {
    func testProbesBuild() {
        _ = ListTabStopProbe(asForm: false)
        _ = ListTabStopProbe(asForm: true)
    }
}

/// A list headed by a search field, with many button rows, between two controls.
struct ListTabStopProbe: View {
    let asForm: Bool
    @State private var query = ""

    var body: some View {
        VStack {
            Button("Before") {}
            if asForm {
                Form {
                    Button("Form row 0") {}
                    Button("Form row 1") {}
                }
                .frame(height: 300.0)
            } else {
                List {
                    TextField("Filter rows", text: $query)
                    ForEach(0..<30, id: \.self) { index in
                        Button("Row \(index)") {}
                    }
                }
                .frame(height: 300.0)
            }
            Button("After") {}
        }
    }
}
