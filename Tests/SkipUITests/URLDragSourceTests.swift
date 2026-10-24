// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import Foundation
import XCTest

final class URLDragSourceTests: SkipUITestCase {
    func testPublicURLDragSourceAPI() {
        _ = URLDragSourceProbe(payload: URL(string: "https://example.com/figure.png")!)
        _ = Text("Preview shape").contentShape(.dragPreview, Circle()).draggable(URL(string: "https://example.com/figure.png")!)
        _ = Text("Accessible shape").contentShape(.accessibility, Circle())
    }
}

struct URLDragSourceProbe: View {
    let payload: URL

    var body: some View {
        Text("Drag figure").frame(width: 120.0, height: 80.0)
            .accessibilityIdentifier("url-drag-source")
            .draggable(payload)
    }
}

struct URLDragRoundTripProbe: View {
    let payload: URL
    @State private var received = "Waiting for drop"

    var body: some View {
        HStack(spacing: 24.0) {
            Text("Drag figure").frame(width: 120.0, height: 80.0)
                .accessibilityIdentifier("native-drag-source")
                .draggable(payload)
            Text(received).frame(width: 120.0, height: 80.0)
                .accessibilityIdentifier("native-drag-target")
                .dropDestination(for: URL.self, action: { urls, _ in
                    guard urls.count == 1, urls[0] == payload else { return false }
                    received = "Dropped 1"
                    return true
                })
        }
    }
}

/// The external receiver exercises the public SDK target, including its Activity permission take().
struct URLDragPermissionReceiverProbe: View {
    let onDrop: (URL) -> Void
    var body: some View {
        Text("Drop a figure from the other app here")
            .frame(maxWidth: .infinity, minHeight: 240.0)
            .background(Color.blue.opacity(0.2))
            .accessibilityIdentifier("cross-uid-drop-target")
            .dropDestination(for: URL.self, action: { urls, _ in
                guard urls.count == 1, urls[0].scheme == "content" else { return false }
                onDrop(urls[0])
                return true
            })
    }
}

struct URLDragPreviewShapeProbe: View {
    var body: some View {
        Color(red: 1.0, green: 0.0, blue: 0.0).frame(width: 100.0, height: 100.0)
            .contentShape(.dragPreview, Circle())
            .draggable(URL(string: "https://example.com/figure.png")!)
            .accessibilityIdentifier("unclipped-drag-source")
    }
}
struct AccessibleContentShapeProbe: View {
    var body: some View {
        Color(red: 1.0, green: 0.0, blue: 0.0).frame(width: 100.0, height: 100.0)
            .contentShape(.accessibility, Circle())
            .accessibilityIdentifier("accessible-circle")
    }
}
