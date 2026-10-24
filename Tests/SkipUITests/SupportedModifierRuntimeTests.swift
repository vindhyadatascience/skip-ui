// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

/// These APIs already have production implementations; keep behavioral proof while
/// repairing the genuinely missing modifiers in the same batch.
final class SupportedModifierRuntimeTests: SkipUITestCase {
    func testBindableProjectionWritesAndReadsItsObservableModel() {
        let model = ModifierBindingModel()
        @Bindable var bindable = model
        let text = $bindable.text
        text.wrappedValue = "edited"
        XCTAssertEqual(model.text, "edited")
        model.text = "external"
        XCTAssertEqual(text.wrappedValue, "external")
    }
}

@Observable final class ModifierBindingModel {
    var text = "initial"
}

struct BindableModifierProbe: View {
    @Bindable var model: ModifierBindingModel

    var body: some View {
        VStack {
            TextField("Edit", text: $model.text).accessibilityIdentifier("bindable-field")
            Text("Model \(model.text)")
        }
    }
}

struct DestructiveButtonProbe: View {
    @State private var clicks = 0

    var body: some View {
        VStack {
            Button(role: .destructive) { clicks += 1 } label: {
                Text("Delete item").accessibilityIdentifier("destructive-label")
            }
            Text("Clicks \(clicks)")
        }
    }
}

struct DeleteModifierProbe: View {
    @State private var rows = [10, 20, 30]
    @State private var lastDeleted = -1

    var body: some View {
        VStack {
            Text("Deleted \(lastDeleted)")
            List {
                ForEach(rows, id: \.self) { value in
                    Text("Row \(value)")
                        .frame(maxWidth: .infinity, minHeight: 44.0)
                        .accessibilityIdentifier("delete-row-\(value)")
                        .deleteDisabled(value == 20)
                }
                .onDelete { offsets in
                    lastDeleted = offsets.first ?? -1
                    rows.remove(atOffsets: offsets)
                }
            }
            .listStyle(.plain)
            .frame(width: 300.0, height: 300.0)
        }
    }
}
