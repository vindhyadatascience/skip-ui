// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class IdStateRestorationTests: SkipUITestCase {
    func testProbeBuilds() { _ = IdStateRestorationProbe() }
}

/// A stateful view placed under `.id(...)`. Its `@State` box is saved through the
/// saver the id modifier supplies (IdStateRestorationComposeTests.kt drives it).
struct IdStateRestorationProbe: View {
    @State private var count = 0

    var body: some View {
        Button("Count \(count)") { count += 1 }
    }
}
