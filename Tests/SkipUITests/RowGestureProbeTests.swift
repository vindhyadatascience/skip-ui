// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import Foundation
import XCTest

final class RowGestureProbeTests: SkipUITestCase {
    func testProbesBuildOnEveryPlatform() {
        _ = DeletableRowProbe()
        _ = DraggableContextMenuRowProbe()
    }
}

/// An inset-grouped section of rows with `.onDelete` whose row content and background are clear,
/// so whatever the swipe container paints behind a row at rest shows through.
struct DeletableRowProbe: View {
    @State private var rows = ["One", "Two"]

    var body: some View {
        List {
            Section {
                ForEach(rows, id: \.self) { row in
                    Color.clear.frame(height: 44.0)
                        .accessibilityIdentifier("deletable-\(row)")
                        .listRowBackground(Color.clear)
                }
                .onDelete { offsets in rows.remove(atOffsets: offsets) }
            }
        }
    }
}

/// A row that is both a URL drag source and carries a context menu, as a report figure row does.
struct DraggableContextMenuRowProbe: View {
    var body: some View {
        Text("Figure row").frame(width: 240.0, height: 60.0)
            .draggable(URL(string: "https://example.com/figure.png")!)
            .contextMenu {
                Button("Move Down") {}
            }
    }
}
