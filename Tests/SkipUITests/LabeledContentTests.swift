// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

/// RNABrowser-fiea.33: SwiftUI draws a LabeledContent value in the secondary style.
final class LabeledContentTests: XCSnapshotTestCase {
    #if SKIP
    /// Renders `content` inside `view` and returns the foreground style the probe saw.
    private func observedStyle(_ view: (ComposeView) -> any View) throws -> ShapeStyle? {
        var seen: ShapeStyle? = nil
        let probe = ComposeView { context in
            seen = EnvironmentValues.shared._foregroundStyle
        }
        _ = try render(view: AnyView(view(probe)))
        return seen
    }
    #endif

    func testValueDefaultsToSecondary() throws {
        #if SKIP
        let style = try observedStyle { probe in LabeledContent("Title") { probe } }
        XCTAssertEqual(Color.secondary, style as? Color)
        #endif
    }

    /// Negative control: an explicit style on the content wins over the secondary default.
    func testExplicitStyleOnValueWins() throws {
        #if SKIP
        let style = try observedStyle { probe in LabeledContent("Title") { probe.foregroundStyle(Color.red) } }
        XCTAssertEqual(Color.red, style as? Color)
        XCTAssertNotEqual(Color.secondary, style as? Color)
        #endif
    }

    /// Negative control: the label is not made secondary.
    func testLabelIsNotSecondary() throws {
        #if SKIP
        var seen: ShapeStyle? = Color.secondary
        let probe = ComposeView { context in seen = EnvironmentValues.shared._foregroundStyle }
        _ = try render(view: AnyView(LabeledContent(content: { Text("v") }, label: { probe })))
        XCTAssertNil(seen)
        #endif
    }
}
