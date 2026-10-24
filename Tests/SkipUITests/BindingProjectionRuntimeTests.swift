// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

final class BindingProjectionRuntimeTests: SkipUITestCase {
    func testPlainStructBindingWritesAndReadsTheOriginalStorage() {
        var stored = 3
        let binding = Binding<Int>(get: { stored }, set: { stored = $0 })
        let holder = PlainBindingHolder(value: binding)
        holder.value = 7
        XCTAssertEqual(stored, 7)
        stored = 11
        XCTAssertEqual(holder.value, 11)
        holder.increment()
        XCTAssertEqual(stored, 12)
    }

    @MainActor func testViewModifierBindingWritesTheOriginalStorage() {
        var stored = BindingOuter(inner: BindingInner(value: 3, sibling: "inner"), sibling: "outer")
        let binding = Binding<BindingOuter>(get: { stored }, set: { stored = $0 })
        let modifier = NestedBindingModifier(model: binding)
        modifier.model.inner.value = 8
        XCTAssertEqual(stored.inner.value, 8)
        XCTAssertEqual(stored.inner.sibling, "inner")
        XCTAssertEqual(stored.sibling, "outer")
        stored.inner.value = 12
        XCTAssertEqual(modifier.model.inner.value, 12)
    }

    func testNestedProjectionKeepsWritebackAndReadsFreshValues() {
        var stored = BindingOuter(inner: BindingInner(value: 3, sibling: "inner"), sibling: "outer")
        let binding = Binding<BindingOuter>(get: { stored }, set: { stored = $0 })
        let holder = NestedBindingHolder(model: binding)
        let projected = holder.valueBinding
        projected.wrappedValue = 17
        XCTAssertEqual(stored.inner.value, 17)
        XCTAssertEqual(stored.inner.sibling, "inner")
        XCTAssertEqual(stored.sibling, "outer")
        stored.inner.value = 23
        XCTAssertEqual(projected.wrappedValue, 23)
        projected.wrappedValue += 1
        XCTAssertEqual(stored.inner.value, 24)
    }
}

struct BindingInner {
    var value: Int
    var sibling: String
}

struct BindingOuter {
    var inner: BindingInner
    var sibling: String
}

struct PlainBindingHolder {
    @Binding var value: Int

    func increment() {
        value += 1
    }
}

struct NestedBindingHolder {
    @Binding var model: BindingOuter

    var valueBinding: Binding<Int> { $model.inner.value }
}

struct NestedBindingModifier: ViewModifier {
    @Binding var model: BindingOuter

    func body(content: Content) -> some View {
        content.onTapGesture {
            let holder = PlainBindingHolder(value: $model.inner.value)
            holder.increment()
        }
    }
}

struct BindingProjectionRuntimeProbe: View {
    @State private var model = BindingOuter(inner: BindingInner(value: 5, sibling: "inner"), sibling: "outer")

    var body: some View {
        VStack {
            Text("Value \(model.inner.value)")
            Text("Change nested binding")
                .frame(width: 180.0, height: 44.0)
                .accessibilityIdentifier("binding-projection-action")
                .modifier(NestedBindingModifier(model: $model))
            Text("Siblings \(model.inner.sibling) \(model.sibling)")
        }
    }
}
