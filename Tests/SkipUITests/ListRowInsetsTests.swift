// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

/// The public API must compile on both faces; actual row geometry is checked by
/// ListRowInsetsComposeTests, through List's normal renderer rather than a padding helper.
final class ListRowInsetsTests: SkipUITestCase {
    func testPublicOptionalInsetsAPI() {
        _ = ListRowInsetsProbe()
    }
}

struct ListRowInsetsProbe: View {
    var body: some View {
        List {
            Color.red.frame(height: 20.0).accessibilityIdentifier("insets-zero")
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.blue.accessibilityIdentifier("insets-zero-background"))
            Color.red.frame(height: 20.0).accessibilityIdentifier("insets-custom")
                .listRowInsets(EdgeInsets(top: 6.0, leading: 12.0, bottom: 8.0, trailing: 24.0))
                .listRowBackground(Color.blue.accessibilityIdentifier("insets-custom-background"))
            Color.red.frame(height: 20.0).accessibilityIdentifier("insets-default")
            Color.red.frame(height: 20.0).accessibilityIdentifier("insets-reset")
                .listRowInsets(EdgeInsets(top: 0.0, leading: 60.0, bottom: 0.0, trailing: 70.0))
                .listRowInsets(nil)
            Color.red.frame(height: 20.0).accessibilityIdentifier("insets-outer")
                .listRowInsets(EdgeInsets(top: 0.0, leading: 60.0, bottom: 0.0, trailing: 70.0))
                .listRowInsets(EdgeInsets(top: 6.0, leading: 12.0, bottom: 8.0, trailing: 24.0))
        }
        .listStyle(.plain)
        .frame(width: 240.0, height: 600.0)
    }
}
