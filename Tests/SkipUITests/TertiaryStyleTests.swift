// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class TertiaryStyleTests: SkipUITestCase {
    func testPublicForegroundStyleAPI() {
        _ = TertiaryStyleProbe()
    }
}

struct TertiaryStyleProbe: View {
    var body: some View {
        Text("Tertiary content")
            .foregroundStyle(.tertiary)
            .accessibilityIdentifier("tertiary-content")
    }
}
