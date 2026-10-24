// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import Foundation
import XCTest

final class ContentShapeTests: XCSnapshotTestCase {
    func testInteractionShapeDoesNotClipTheOriginalPaint() throws {
        let paint = Color(red: 1.0, green: 0.0, blue: 0.0).frame(width: 80.0, height: 80.0)
        // One content installation per Compose test rule; compare all three exact pixel panels.
        let snapshot = try render(view: HStack(spacing: 0.0) {
            paint
            Color.clear.frame(width: 80.0, height: 80.0)
            paint.contentShape(Circle())
        }.background(Color.white))
        let panels = threeComparisonPanels(snapshot.pixmap, panelWidth: 80, panelHeight: 80)
        XCTAssertNotEqual(panels[0], panels[1])
        XCTAssertEqual(panels[2], panels[0])
    }

    func testPublicShapeAndEvenOddAPIs() {
        _ = ContentShapeRuntimeProbe(variant: 0)
        _ = ContentShapeRuntimeProbe(variant: 1)
        _ = ContentShapeRuntimeProbe(variant: 2)
        _ = ContentShapeRuntimeProbe(variant: 3)
        _ = ContentShapeRuntimeProbe(variant: 4)
        _ = ContentShapeRuntimeProbe(variant: 5)
    }
}

struct HoleContentShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.addRect(rect)
        path.addRect(rect.insetBy(dx: 30.0, dy: 30.0))
        return path
    }
}

struct ContentShapeRuntimeProbe: View {
    let variant: Int
    @State private var over = 0
    @State private var under = 0

    var body: some View {
        VStack {
            Text("Over \(over)")
            Text("Under \(under)")
            ZStack {
                Color.blue.frame(width: 100.0, height: 100.0)
                    .onTapGesture { under += 1 }
                if variant == 0 {
                    Color.red.frame(width: 100.0, height: 100.0)
                        .accessibilityIdentifier("content-shape-target")
                        .contentShape(Circle())
                        .onTapGesture { over += 1 }
                } else if variant == 1 {
                    Color.red.frame(width: 100.0, height: 100.0)
                        .accessibilityIdentifier("content-shape-target")
                        .onTapGesture { over += 1 }
                        .contentShape(.interaction, Circle())
                } else if variant == 4 {
                    Circle().fill(Color.red).frame(width: 100.0, height: 100.0)
                        .accessibilityIdentifier("content-shape-target")
                        .contentShape(Rectangle())
                        .onTapGesture { over += 1 }
                } else if variant == 5 {
                    Circle().fill(Color.red)
                        .accessibilityIdentifier("content-shape-target")
                        .onTapGesture { over += 1 }
                        .contentShape(Rectangle())
                        .frame(width: 100.0, height: 100.0)
                } else {
                    Color.red.frame(width: 100.0, height: 100.0)
                        .accessibilityIdentifier("content-shape-target")
                        .contentShape(HoleContentShape(), eoFill: variant == 2)
                        .onTapGesture { over += 1 }
                }
            }
        }
    }
}

/// Compare independent panels from one render instead of installing an Activity's content twice.
extension XCSnapshotTestCase {
    func threeComparisonPanels(_ pixmap: String, panelWidth: Int, panelHeight: Int) -> [String] {
        let rows = pixmap.split(separator: "\n")
        XCTAssertEqual(rows.count, panelHeight)
        var panels = ["", "", ""]
        for row in rows {
            let pixels = row.split(separator: " ")
            XCTAssertEqual(pixels.count, panelWidth * 3)
            guard pixels.count == panelWidth * 3 else { return panels }
            for panel in 0..<3 {
                for x in 0..<panelWidth {
                    panels[panel] += String(pixels[panel * panelWidth + x]) + " "
                }
                panels[panel] += "\n"
            }
        }
        return panels
    }
}
