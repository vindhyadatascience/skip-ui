// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class KeyboardInsetRecompositionTests: SkipUITestCase {
    func testPublicNavigationAndSheetKeyboardProbes() {
        _ = NavigationKeyboardRuntimeProbe(navigationCounter: KeyboardBodyCounter(), sheetCounter: KeyboardBodyCounter())
    }
}

final class KeyboardBodyCounter {
    var bodies = 0
}

struct NavigationKeyboardRuntimeProbe: View {
    let navigationCounter: KeyboardBodyCounter
    let sheetCounter: KeyboardBodyCounter
    @State private var showSheet = false

    var body: some View {
        NavigationStack {
            KeyboardCountedContent(counter: navigationCounter, fieldTag: "navigation-ime-field")
                .toolbar {
                    #if os(iOS) || os(Android)
                    ToolbarItem(placement: .bottomBar) {
                        Button("Show keyboard sheet") { showSheet = true }
                    }
                    #else
                    ToolbarItem(placement: .automatic) {
                        Button("Show keyboard sheet") { showSheet = true }
                    }
                    #endif
                }
                .sheet(isPresented: $showSheet) {
                    KeyboardCountedContent(counter: sheetCounter, fieldTag: "sheet-ime-field")
                }
        }
    }
}

struct KeyboardCountedContent: View {
    let counter: KeyboardBodyCounter
    let fieldTag: String
    @State private var text = ""

    var body: some View {
        counter.bodies += 1
        return VStack {
            TextField("Type here", text: $text).accessibilityIdentifier(fieldTag)
            Text("Stable content").frame(width: 120.0, height: 30.0)
                .accessibilityIdentifier(fieldTag + "-stable")
        }
    }
}
