// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import Foundation
import XCTest

#if SKIP
@testable import SkipUI
#endif

final class PopoverTests: SkipUITestCase {
    func testBooleanAndItemPublicAPIs() {
        _ = PopoverRuntimeProbe()
    }

    #if SKIP
    func testAttachmentGeometryPreservesCustomCoordinatesAndDensity() {
        let bounds = CGRect(x: 100.0, y: 200.0, width: 80.0, height: 40.0)
        let whole: PopoverAttachmentAnchor = .rect(.bounds)
        XCTAssertEqual(whole.resolved(in: bounds, isRightToLeft: false, scale: 2.0), bounds)
        let rect: PopoverAttachmentAnchor = .rect(.rect(CGRect(x: 4.0, y: 5.0, width: 20.0, height: 10.0)))
        XCTAssertEqual(rect.resolved(in: bounds, isRightToLeft: false, scale: 2.0),
                       CGRect(x: 108.0, y: 210.0, width: 40.0, height: 20.0))
        let point: PopoverAttachmentAnchor = .point(UnitPoint(x: 0.25, y: 0.75))
        XCTAssertEqual(point.resolved(in: bounds, isRightToLeft: false, scale: 2.0),
                       CGRect(x: 120.0, y: 230.0, width: 0.0, height: 0.0))
        XCTAssertEqual(point.resolved(in: bounds, isRightToLeft: true, scale: 2.0),
                       CGRect(x: 160.0, y: 230.0, width: 0.0, height: 0.0))
        let outside: PopoverAttachmentAnchor = .point(UnitPoint(x: 1.5, y: -0.5))
        XCTAssertEqual(outside.resolved(in: bounds, isRightToLeft: false, scale: 1.0),
                       CGRect(x: 220.0, y: 180.0, width: 0.0, height: 0.0))
    }
    #endif
}

struct PopoverTestItem: Identifiable {
    let id: String
}

struct PopoverDismissProbe: View {
    let label: String
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack {
            Text(label)
            Button("Close popover") { dismiss() }
        }
        .padding(16.0)
    }
}

struct PopoverRuntimeProbe: View {
    @State private var presented = false
    @State private var anchorProbePresented = false
    @State private var item: PopoverTestItem? = nil

    var body: some View {
        VStack {
            Text(presented ? "Boolean true" : "Boolean false")
            Button("Open boolean") { presented = true }
                .popover(isPresented: $presented, attachmentAnchor: .rect(.bounds), arrowEdge: .top) {
                    PopoverDismissProbe(label: "Boolean popover")
                }
            Text("Rect anchor API")
                .popover(isPresented: $anchorProbePresented,
                         attachmentAnchor: .rect(.rect(CGRect(x: 4.0, y: 5.0, width: 20.0, height: 10.0))),
                         arrowEdge: .leading) { Text("Rect content") }
            Text("Point anchor API")
                .popover(isPresented: $anchorProbePresented,
                         attachmentAnchor: .point(UnitPoint(x: 0.25, y: 0.75)),
                         arrowEdge: .trailing) { Text("Point content") }
            Text("Item \(item?.id ?? "none")")
            Button("Open item") { item = PopoverTestItem(id: "first") }
                .popover(item: $item, attachmentAnchor: .point(.bottomTrailing), arrowEdge: .bottom) { selected in
                    VStack {
                        PopoverDismissProbe(label: "Selected \(selected.id)")
                        Button("Change item") { item = PopoverTestItem(id: "second") }
                    }
                }
        }
    }
}

/// A native-compatible fixture factory; the placement algorithm remains in production.
func boundsPopoverTestAnchor() -> PopoverAttachmentAnchor { .rect(.bounds) }
