// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import Foundation
import XCTest

#if SKIP
@testable import SkipUI
#endif

final class MenuItemSelectionSlotTests: SkipUITestCase {
    func testProbesBuildOnEveryPlatform() {
        _ = ForEachMenuProbe()
        _ = ForEachPickerProbe()
    }
}

/// A Menu whose first item comes from a `ForEach` (which tags it with its id) and whose second
/// is written out: both are plain actions, so their titles start at the same x.
struct ForEachMenuProbe: View {
    var body: some View {
        Menu("Row actions") {
            ForEach(["Export Project"], id: \.self) { title in
                Button(title) {}
            }
            Button("Delete Project") {}
        }
    }
}

/// A menu-style Picker still marks its selected item.
struct ForEachPickerProbe: View {
    @State private var choice = "Alpha"

    var body: some View {
        Picker("Choice", selection: $choice) {
            ForEach(["Alpha", "Beta"], id: \.self) { title in
                Text(title).tag(title)
            }
        }
        .pickerStyle(.menu)
    }
}
