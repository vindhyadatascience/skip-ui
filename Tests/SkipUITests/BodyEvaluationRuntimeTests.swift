// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class BodyEvaluationRuntimeTests: SkipUITestCase {
    func testPublicFallbackInputProbe() { _ = BodyEvaluationFallbackProbe() }
}

struct BodyEvaluationCustomValue: Equatable {
    let value: Int
    static func == (lhs: Self, rhs: Self) -> Bool { true }
}

struct BodyEvaluationClosureChild: View {
    let action: () -> Void
    var body: some View { Button("Invoke current action", action: action) }
}

struct BodyEvaluationBindingChild: View {
    let value: Binding<Int>
    var body: some View { Button("Increment current binding") { value.wrappedValue += 1 } }
}

struct BodyEvaluationCustomChild: View {
    let value: BodyEvaluationCustomValue
    var body: some View { Text("Custom input \(value.value)") }
}

struct BodyEvaluationFallbackProbe: View {
    @State private var version = 0
    @State private var observed = -1
    @State private var rightSelected = false
    @State private var left = 0
    @State private var right = 0

    var body: some View {
        let capturedVersion = version
        let capturedTarget = rightSelected
        let binding = Binding<Int>(get: { capturedTarget ? right : left }, set: { value in
            if capturedTarget { right = value } else { left = value }
        })
        return VStack {
            Text("Observed \(observed)")
            Text("Left \(left) Right \(right)")
            Button("Change fallback inputs") { version += 1; rightSelected.toggle() }
            BodyEvaluationClosureChild(action: { observed = capturedVersion })
            BodyEvaluationBindingChild(value: binding)
            BodyEvaluationCustomChild(value: BodyEvaluationCustomValue(value: capturedVersion))
        }
    }
}
