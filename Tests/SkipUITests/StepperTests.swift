// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import Foundation
import SwiftUI
import XCTest

#if !SKIP
import SkipUI
#else
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
#endif

final class StepperTests: SkipUITestCase {
    // SKIP INSERT: @get:org.junit.Rule val composeRule = createComposeRule()

    func testBoundedIntLabel() throws { try checkBoundedOverload(integer: true, form: .label) }
    func testBoundedIntLocalizedKey() throws { try checkBoundedOverload(integer: true, form: .key) }
    func testBoundedIntLocalizedResource() throws { try checkBoundedOverload(integer: true, form: .resource) }
    func testBoundedIntString() throws { try checkBoundedOverload(integer: true, form: .string) }
    func testBoundedDoubleLabel() throws { try checkBoundedOverload(integer: false, form: .label) }
    func testBoundedDoubleLocalizedKey() throws { try checkBoundedOverload(integer: false, form: .key) }
    func testBoundedDoubleLocalizedResource() throws { try checkBoundedOverload(integer: false, form: .resource) }
    func testBoundedDoubleString() throws { try checkBoundedOverload(integer: false, form: .string) }

    /// Every public overload must carry both bounds and its callback into the existing
    /// renderer. Observe values at begin/end so callback order is checked around the write.
    private func checkBoundedOverload(integer: Bool, form: StepperLabelForm) throws {
        #if !SKIP
        throw XCTSkip("Stepper interaction requires Compose")
        #else
        let intValue = State(initialValue: 2)
        let doubleValue = State(initialValue: 0.5)
        let log = StepperEditingLog()
        let title = "Bounded title"
        let key: LocalizedStringKey = "Bounded title"
        let resource: LocalizedStringResource = "Bounded title"
        composeRule.setContent {
            if integer {
                switch form {
                case .label:
                    Stepper(value: intValue.projectedValue, in: -2...6, step: 4,
                        label: { Text(title) },
                        onEditingChanged: { log.record($0, observed: Double(intValue.wrappedValue)) }).Compose()
                case .key:
                    Stepper(key, value: intValue.projectedValue, in: -2...6, step: 4,
                        onEditingChanged: { log.record($0, observed: Double(intValue.wrappedValue)) }).Compose()
                case .resource:
                    Stepper(resource, value: intValue.projectedValue, in: -2...6, step: 4,
                        onEditingChanged: { log.record($0, observed: Double(intValue.wrappedValue)) }).Compose()
                case .string:
                    Stepper(title, value: intValue.projectedValue, in: -2...6, step: 4,
                        onEditingChanged: { log.record($0, observed: Double(intValue.wrappedValue)) }).Compose()
                }
            } else {
                switch form {
                case .label:
                    Stepper(value: doubleValue.projectedValue, in: 0.25...0.75, step: 0.25,
                        label: { Text(title) },
                        onEditingChanged: { log.record($0, observed: doubleValue.wrappedValue) }).Compose()
                case .key:
                    Stepper(key, value: doubleValue.projectedValue, in: 0.25...0.75, step: 0.25,
                        onEditingChanged: { log.record($0, observed: doubleValue.wrappedValue) }).Compose()
                case .resource:
                    Stepper(resource, value: doubleValue.projectedValue, in: 0.25...0.75, step: 0.25,
                        onEditingChanged: { log.record($0, observed: doubleValue.wrappedValue) }).Compose()
                case .string:
                    Stepper(title, value: doubleValue.projectedValue, in: 0.25...0.75, step: 0.25,
                        onEditingChanged: { log.record($0, observed: doubleValue.wrappedValue) }).Compose()
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Increment").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Decrement").assertIsEnabled()
        XCTAssertTrue(log.states().isEmpty)
        press("Increment")
        let upper = integer ? 6.0 : 0.75
        let initial = integer ? 2.0 : 0.5
        let lower = integer ? -2.0 : 0.25
        XCTAssertEqual(integer ? Double(intValue.wrappedValue) : doubleValue.wrappedValue, upper)
        composeRule.onNodeWithContentDescription("Increment").assertIsNotEnabled()
        // A physical click on a disabled button must not write or emit editing callbacks.
        composeRule.onNodeWithContentDescription("Increment").performTouchInput { click() }
        composeRule.waitForIdle()
        XCTAssertEqual(log.states(), [true, false])
        press("Decrement")
        press("Decrement")
        XCTAssertEqual(integer ? Double(intValue.wrappedValue) : doubleValue.wrappedValue, lower)
        composeRule.onNodeWithContentDescription("Decrement").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Decrement").performTouchInput { click() }
        composeRule.waitForIdle()
        XCTAssertEqual(log.states(), [true, false, true, false, true, false])
        XCTAssertEqual(log.values(), [initial, upper, upper, initial, initial, lower])
        #endif
    }

    func testBoundedPartialStepsKeepExistingBridgeBehavior() throws {
        try checkPartialSteps(bridged: false)
    }

    func testExistingBridgePartialStepControl() throws {
        try checkPartialSteps(bridged: true)
    }

    /// The existing bridge clamps a short final decrement, but disables an increment
    /// whose whole step will not fit. This overload repair must not change that renderer.
    private func checkPartialSteps(bridged: Bool) throws {
        #if !SKIP
        throw XCTSkip("Stepper interaction requires Compose")
        #else
        let value = State(initialValue: 0.25)
        let log = StepperEditingLog()
        composeRule.setContent {
            if bridged {
                Stepper(getValue: { value.wrappedValue }, setValue: { value.wrappedValue = $0 },
                    step: 0.5, minValue: 0.0, maxValue: 1.0,
                    bridgedOnEditingChanged: { log.record($0, observed: value.wrappedValue) },
                    bridgedLabel: Text("Partial")).Compose()
            } else {
                Stepper(value: value.projectedValue, in: 0.0...1.0, step: 0.5,
                    label: { Text("Partial") },
                    onEditingChanged: { log.record($0, observed: value.wrappedValue) }).Compose()
            }
        }
        composeRule.waitForIdle()
        press("Decrement")
        XCTAssertEqual(value.wrappedValue, 0.0)
        composeRule.onNodeWithContentDescription("Decrement").assertIsNotEnabled()
        composeRule.runOnIdle { value.wrappedValue = 0.75 }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Increment").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Increment").performTouchInput { click() }
        composeRule.waitForIdle()
        XCTAssertEqual(value.wrappedValue, 0.75)
        XCTAssertEqual(log.states(), [true, false])
        press("Decrement")
        XCTAssertEqual(value.wrappedValue, 0.25)
        press("Decrement")
        XCTAssertEqual(value.wrappedValue, 0.0)
        XCTAssertEqual(log.values(), [0.25, 0.0, 0.75, 0.25, 0.25, 0.0])
        #endif
    }

    func testBoundsDoNotRewriteTheBindingAtConstruction() throws {
        #if !SKIP
        throw XCTSkip("Stepper interaction requires Compose")
        #else
        let value = State(initialValue: 9)
        let log = StepperEditingLog()
        composeRule.setContent {
            Stepper("Outside", value: value.projectedValue, in: 0...6, step: 2,
                onEditingChanged: { log.record($0, observed: Double(value.wrappedValue)) }).Compose()
        }
        composeRule.waitForIdle()
        XCTAssertEqual(value.wrappedValue, 9)
        XCTAssertTrue(log.states().isEmpty)
        composeRule.onNodeWithContentDescription("Increment").assertIsNotEnabled()
        press("Decrement")
        XCTAssertEqual(value.wrappedValue, 7)
        XCTAssertEqual(log.values(), [9.0, 7.0])
        #endif
    }

    func testUnboundedStepperStillCrossesZero() throws {
        #if !SKIP
        throw XCTSkip("Stepper interaction requires Compose")
        #else
        let value = State(initialValue: 0)
        let log = StepperEditingLog()
        composeRule.setContent {
            Stepper("Unbounded", value: value.projectedValue, step: 2,
                onEditingChanged: { log.record($0, observed: Double(value.wrappedValue)) }).Compose()
        }
        composeRule.waitForIdle()
        press("Decrement")
        press("Decrement")
        XCTAssertEqual(value.wrappedValue, -4)
        composeRule.onNodeWithContentDescription("Increment").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Decrement").assertIsEnabled()
        press("Increment")
        XCTAssertEqual(value.wrappedValue, -2)
        XCTAssertEqual(log.states(), [true, false, true, false, true, false])
        XCTAssertEqual(log.values(), [0.0, -2.0, -2.0, -4.0, -4.0, -2.0])
        #endif
    }

    #if SKIP
    private func press(_ description: String) {
        composeRule.onNodeWithContentDescription(description).performClick()
        composeRule.waitForIdle()
    }
    #endif
}

private enum StepperLabelForm { case label, key, resource, string }

#if SKIP
private final class StepperEditingLog {
    private var editingStates: [Bool] = []
    private var observedValues: [Double] = []

    func record(_ editing: Bool, observed: Double) {
        editingStates.append(editing)
        observedValues.append(observed)
    }

    func states() -> [Bool] { editingStates }
    func values() -> [Double] { observedValues }
}
#else
/// Deliberately typechecked against SkipUI itself, not native SwiftUI, which already
/// supplies these overloads. Never invoked: SkipUI's native ViewBuilder is a stub.
private func boundedStepperAPICompileRepro() {
    let integer = SkipUI.Binding<Int>.constant(2)
    let double = SkipUI.Binding<Double>.constant(0.5)
    let key: SkipUI.LocalizedStringKey = "Bounded title"
    let resource: LocalizedStringResource = "Bounded title"
    let title = "Bounded title"
    _ = SkipUI.Stepper(value: integer, in: 0...4) { SkipUI.Text(verbatim: title) }
    _ = SkipUI.Stepper(key, value: integer, in: 0...4, step: 2, onEditingChanged: { _ in })
    _ = SkipUI.Stepper(resource, value: integer, in: 0...4, step: 2, onEditingChanged: { _ in })
    _ = SkipUI.Stepper(title, value: integer, in: 0...4, step: 2, onEditingChanged: { _ in })
    _ = SkipUI.Stepper(value: double, in: 0.0...1.0, step: 0.5,
        label: { SkipUI.Text(verbatim: title) }, onEditingChanged: { _ in })
    _ = SkipUI.Stepper(key, value: double, in: 0.0...1.0, step: 0.5, onEditingChanged: { _ in })
    _ = SkipUI.Stepper(resource, value: double, in: 0.0...1.0)
    _ = SkipUI.Stepper(title, value: double, in: 0.0...1.0, step: 0.5, onEditingChanged: { _ in })
}
#endif
