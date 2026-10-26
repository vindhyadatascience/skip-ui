// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest
#if SKIP
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
#endif

final class ScrollViewReaderRegressionTests: SkipUITestCase {
    // SKIP INSERT: @get:org.junit.Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    func testFirstAppearanceRevealsPlainScrollTarget() throws {
        #if !SKIP
        throw XCTSkip("Compose scroll regression")
        #else
        composeRule.setContent {
            ScrollViewReader { proxy in
                ScrollView(.horizontal) {
                    HStack {
                        Text("first").frame(width: 600.0).id("first")
                        Text("last").frame(width: 80.0).id("last")
                            .accessibilityIdentifier("plain-last")
                    }
                }
                .frame(width: 120.0, height: 50.0)
                .onAppear { proxy.scrollTo("last") }
            }.Compose()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("plain-last").assertIsDisplayed()
        #endif
    }

    /// RNABrowser-fiea.7: the original plain vertical ScrollView/VStack/ForEach repro.
    /// Confirm the target starts outside the viewport, then moves into view and back.
    /// No anchor is requested: this asserts reveal behavior, not alignment semantics.
    func testPlainVerticalVStackScrollsToOffscreenForEachID() throws {
        #if !SKIP
        throw XCTSkip("Compose scroll regression")
        #else
        var retainedProxy: ScrollViewProxy? = nil
        composeRule.setContent {
            ScrollViewReader { proxy in
                ScrollView(.vertical) {
                    VStack(spacing: 0.0) {
                        ForEach(0..<12, id: \.self) { row in
                            Text("row \(row)")
                                .frame(width: 120.0, height: 80.0)
                                .id(row)
                                .accessibilityIdentifier("vertical-row-\(row)")
                        }
                    }
                }
                .frame(width: 120.0, height: 120.0)
                .onAppear { retainedProxy = proxy }
            }.Compose()
        }
        composeRule.waitForIdle()
        XCTAssertNotNil(retainedProxy)
        composeRule.onNodeWithTag("vertical-row-0").assertIsDisplayed()
        composeRule.onNodeWithTag("vertical-row-11").assertIsNotDisplayed()

        composeRule.runOnIdle { retainedProxy?.scrollTo(11) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vertical-row-11").assertIsDisplayed()
        composeRule.onNodeWithTag("vertical-row-0").assertIsNotDisplayed()

        composeRule.runOnIdle { retainedProxy?.scrollTo(0) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vertical-row-0").assertIsDisplayed()
        composeRule.onNodeWithTag("vertical-row-11").assertIsNotDisplayed()
        #endif
    }

    func testFirstAppearanceRevealsLazyScrollTarget() throws {
        #if !SKIP
        throw XCTSkip("Compose scroll regression")
        #else
        composeRule.setContent {
            ScrollViewReader { proxy in
                ScrollView(.horizontal) {
                    LazyHStack {
                        Text("first").frame(width: 600.0).id("first")
                        Text("last").frame(width: 80.0).id("last")
                            .accessibilityIdentifier("lazy-last")
                    }
                }
                .frame(width: 120.0, height: 50.0)
                .onAppear { proxy.scrollTo("last") }
            }.Compose()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lazy-last").assertIsDisplayed()
        #endif
    }
    func testRetainedProxyRoutesLaterRequestsAndIgnoresMissingIDs() throws {
        #if !SKIP
        throw XCTSkip("Compose scroll regression")
        #else
        var initialProxy: ScrollViewProxy? = nil
        composeRule.setContent {
            ScrollViewReader { proxy in
                ScrollView(.horizontal) {
                    HStack {
                        Text("first").frame(width: 80.0).id("first")
                            .accessibilityIdentifier("retained-first")
                        Text("gap").frame(width: 600.0)
                        Text("last").frame(width: 80.0).id("last")
                            .accessibilityIdentifier("retained-last")
                    }
                }
                .frame(width: 120.0, height: 50.0)
                .onAppear { initialProxy = proxy }
            }.Compose()
        }
        composeRule.runOnIdle { initialProxy?.scrollTo("missing") }
        composeRule.onNodeWithTag("retained-first").assertIsDisplayed()
        composeRule.runOnIdle { initialProxy?.scrollTo("last") }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("retained-last").assertIsDisplayed()
        composeRule.runOnIdle { initialProxy?.scrollTo("first") }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("retained-first").assertIsDisplayed()
        #endif
    }

    func testPendingRequestWaitsForTargetPlacement() throws {
        #if !SKIP
        throw XCTSkip("Compose scroll regression")
        #else
        let showTarget = State(initialValue: false)
        composeRule.setContent {
            ScrollViewReader { proxy in
                ScrollView(.horizontal) {
                    HStack {
                        Text("gap").frame(width: 600.0)
                        if showTarget.wrappedValue {
                            Text("late").frame(width: 80.0).id("late")
                                .accessibilityIdentifier("late-target")
                        }
                    }
                }
                .frame(width: 120.0, height: 50.0)
                .onAppear { proxy.scrollTo("late") }
            }.Compose()
        }
        composeRule.runOnIdle { showTarget.wrappedValue = true }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("late-target").assertIsDisplayed()
        #endif
    }

}
