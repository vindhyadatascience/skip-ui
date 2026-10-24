// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

// The single render contains three 140x64 panels; the Android host viewport must fit all three.
// SKIP INSERT: @org.robolectric.annotation.Config(qualifiers = "w420dp-h64dp-mdpi")
final class FixedTextOverflowTests: XCSnapshotTestCase {
    /// Native SwiftUI is the reference: a frame sizes layout; clipped() is what clips paint.
    func testTinyFrameKeepsTextPaintUntilClippingIsExplicit() throws {
        let snapshot = try render(view: HStack(spacing: 0.0) {
            FixedTextPaintProbe(clipped: false)
            FixedTextPaintProbe(clipped: true)
            Color.red.frame(width: 80.0, height: 4.0).padding(30.0).background(Color.white)
        })
        let panels = threeComparisonPanels(snapshot.pixmap, panelWidth: 140, panelHeight: 64)
        XCTAssertNotEqual(panels[0], panels[2])
        XCTAssertNotEqual(panels[0], panels[1])
    }
    #if !SKIP
    func testNativeDefaultTruncationIsTail() {
        XCTAssertEqual(EnvironmentValues().truncationMode, .tail)
    }
    #endif
}

struct FixedTextPaintProbe: View {
    let clipped: Bool

    private var framedText: some View {
        Text("First\nSecond").font(.system(size: 24.0)).foregroundStyle(Color.black)
            .frame(width: 80.0, height: 4.0)
            .background(Color.red)
    }

    var body: some View {
        if clipped {
            framedText.clipped().padding(30.0).background(Color.white)
        } else {
            framedText.padding(30.0).background(Color.white)
        }
    }
}

struct FixedTextLayoutProbe: View {
    var body: some View {
        VStack(spacing: 30.0) {
            Text("First\nSecond").font(.system(size: 24.0))
                .accessibilityIdentifier("unlimited-fixed-text")
                .frame(width: 80.0, height: 4.0)
                .accessibilityIdentifier("tiny-text-frame")
            Text("A deliberately long single line")
                .lineLimit(1)
                .accessibilityIdentifier("limited-fixed-text")
                .frame(width: 80.0, height: 24.0)
        }
    }
}
