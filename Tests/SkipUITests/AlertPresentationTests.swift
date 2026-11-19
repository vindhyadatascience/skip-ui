// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import Foundation
import XCTest

#if SKIP
@testable import SkipUI
#endif

final class AlertPresentationTests: SkipUITestCase {
    func testProbesBuildOnEveryPlatform() {
        _ = AlertBindingProbe()
        _ = ConfirmationBindingProbe()
        _ = LoneCancelAlertProbe()
    }
}

/// An alert whose isPresented binding is derived from the data its action uses.
struct AlertBindingProbe: View {
    @State private var pending: String? = nil
    @State private var deleted = "none"

    var body: some View {
        VStack {
            Text("Alert deleted \(deleted)")
            Button("Ask alert") { pending = "row" }
        }
        .alert("Delete row?", isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } })) {
            Button("Delete", role: .destructive) { deleted = pending ?? "nil" }
            Button("Cancel", role: .cancel) {}
        }
    }
}

/// The same shape through confirmationDialog.
struct ConfirmationBindingProbe: View {
    @State private var pending: String? = nil
    @State private var deleted = "none"

    var body: some View {
        VStack {
            Text("Dialog deleted \(deleted)")
            Button("Ask dialog") { pending = "row" }
        }
        .confirmationDialog("Delete row?", isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } })) {
            Button("Delete row", role: .destructive) { deleted = pending ?? "nil" }
        }
    }
}

/// An alert whose only button is a cancel-role OK: SwiftUI shows exactly one button.
struct LoneCancelAlertProbe: View {
    @State private var presented = false
    @State private var acknowledged = false

    var body: some View {
        VStack {
            Text(acknowledged ? "Acknowledged" : "Not acknowledged")
            Button("Show notice") { presented = true }
        }
        .alert("Notice", isPresented: $presented) {
            Button("OK", role: .cancel) { acknowledged = true }
        } message: {
            Text("Something happened.")
        }
    }
}
