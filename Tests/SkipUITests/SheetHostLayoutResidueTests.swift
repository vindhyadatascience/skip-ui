// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class SheetHostLayoutResidueTests: SkipUITestCase {
    func testPublicInlineSheetHosts() {
        _ = SheetHostLayoutResidueProbe(sheetOnButton: true)
        _ = SheetHostLayoutResidueProbe(sheetOnButton: false)
    }
}

struct SheetHostLayoutResidueProbe: View {
    let sheetOnButton: Bool
    @State private var presented = false
    @State private var dismissals = 0
    var body: some View {
        VStack(spacing: 8.0) {
            Text("Above inline sheet row").frame(height: 20.0)
                .accessibilityIdentifier("above-inline-sheet")
            if sheetOnButton {
                HStack(spacing: 12.0) {
                    Button("Open inline sheet") { presented = true }
                        .sheet(isPresented: $presented, onDismiss: { dismissals += 1 }) {
                            InlineSheetExpandingContent()
                        }
                    Text("Row sibling").frame(height: 20.0)
                }
                .accessibilityIdentifier("in-layout-sheet-host")
            } else {
                HStack(spacing: 12.0) {
                    Button("Open inline sheet") { presented = true }
                    Text("Row sibling").frame(height: 20.0)
                }
                .sheet(isPresented: $presented, onDismiss: { dismissals += 1 }) {
                    InlineSheetExpandingContent()
                }
                .accessibilityIdentifier("in-layout-sheet-host")
            }
            Text("Below inline sheet row").frame(height: 20.0)
                .accessibilityIdentifier("below-inline-sheet")
            Text("Inline dismissals \(dismissals)")
        }
        .frame(width: 320.0, height: 420.0, alignment: .top)
    }
}

struct InlineSheetExpandingContent: View {
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            VStack {
                Text("Actual expanding inline sheet")
                Button("Close inline sheet") { dismiss() }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityIdentifier("inline-sheet-content")
        }
        .presentationDetents([.height(260.0)])
    }
}
