// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class KeyboardShortcutRuntimeTests: SkipUITestCase {
    func testAllPublicShortcutOverloadsAndLiteralKey() {
        _ = KeyboardShortcutRuntimeProbe()
        _ = KeyboardArrowShortcutProbe(withoutMirroring: true)
        _ = Button("Localized") {}.keyboardShortcut("s", modifiers: [.command, .shift], localization: .custom)
        _ = Button("No shortcut") {}.keyboardShortcut(nil)
    }
}

struct KeyboardShortcutRuntimeProbe: View {
    @State private var query = ""
    @State private var hits = 0
    @State private var defaults = 0
    @State private var cancels = 0
    @State private var version = 1
    @State private var enabled = true
    @State private var shown = true
    @State private var toggle = false
    var body: some View {
        VStack {
            TextField("Type while shortcuts remain active", text: $query).accessibilityIdentifier("shortcut-field")
            Text("Shortcut hits \(hits)")
            Text("Default hits \(defaults)")
            Text("Cancel hits \(cancels)")
            Text("Toggle \(toggle ? "on" : "off")")
            if shown {
                Button("Find") { hits += version }.keyboardShortcut("f").disabled(!enabled)
            }
            Button("Default action") { defaults += 1 }.keyboardShortcut(.defaultAction)
            Button("Cancel action") { cancels += 1 }.keyboardShortcut(.cancelAction)
            Toggle("Shortcut toggle", isOn: $toggle).keyboardShortcut("t")
            Button("Change handler version") { version = 3 }
            Button("Toggle shortcut enabled") { enabled.toggle() }
            Button("Toggle shortcut presence") { shown.toggle() }
        }
    }
}

struct KeyboardArrowShortcutProbe: View {
    let withoutMirroring: Bool
    @State private var hits = 0
    var body: some View {
        VStack {
            Button("Arrow action") { hits += 1 }
                .keyboardShortcut(.leftArrow, modifiers: [], localization: withoutMirroring ? .withoutMirroring : .automatic)
            Text("Arrow hits \(hits)")
        }
    }
}
