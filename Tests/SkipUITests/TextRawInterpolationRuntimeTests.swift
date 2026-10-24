// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import Foundation
import XCTest

final class TextRawInterpolationRuntimeTests: SkipUITestCase {
    func testPublicNumericAndStringTextProbes() {
        _ = TextRawInterpolationRuntimeProbe()
    }

    #if !SKIP
    func testNativeResourceFloatingInterpolationUsesSixFractionDigits() {
        let count = 7
        let value = 3.5
        let name = "alpha"
        let resource = LocalizedStringResource("Resource \(count) / \(value) / \(name)", locale: Locale(identifier: "en_US"))
        XCTAssertEqual(String(localized: resource), "Resource 7 / 3.500000 / alpha")
    }
    #endif
}

/// Exercise each public localization input through the real Text renderer. These values
/// are Java Formatter arguments on Android, rather than Swift AnyHashable collection keys.
struct TextRawInterpolationRuntimeProbe: View {
    private let count = 7
    private let value = 3.5
    private let name = "alpha"

    var body: some View {
        let plain: LocalizedStringKey = "Raw \(count) / \(value) / \(name)"
        let markdown: LocalizedStringKey = "**Raw** \(count) / \(value) / \(name)"
        let resource: LocalizedStringResource = "Resource \(count) / \(value) / \(name)"
        let resourceMarkdown: LocalizedStringResource = "**Resource** \(count) / \(value) / \(name)"
        return VStack {
            Text(plain).accessibilityIdentifier("raw-key-plain")
            Text(markdown).accessibilityIdentifier("raw-key-markdown")
            Text(resource).accessibilityIdentifier("raw-resource-plain")
            Text(resourceMarkdown).accessibilityIdentifier("raw-resource-markdown")
        }
    }
}

#if SKIP
/// Styled runs call localizedTextString before folding runs into the shared renderer.
struct TextRawConcatenationProbe: View {
    private let count = 7
    private let value = 3.5
    private let name = "alpha"
    var body: some View {
        let first: LocalizedStringKey = "Run \(count) / \(value) / \(name)"
        return (Text(first).bold() + Text(verbatim: " end"))
            .accessibilityIdentifier("raw-concatenated-runs")
    }
}
#endif
