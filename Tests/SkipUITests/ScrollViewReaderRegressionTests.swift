// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest
#if SKIP
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
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
