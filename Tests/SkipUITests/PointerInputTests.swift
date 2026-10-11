// Copyright 2023–2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

#if SKIP
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.geometry.Offset
#endif

/// Mouse and trackpad input on Android: `onHover` reports the pointer entering and leaving a
/// view, and a secondary (right) click opens a `contextMenu` at once, as on a desktop.
final class PointerInputTests: SkipUITestCase {
    // SKIP INSERT: @get:org.junit.Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    func testHoverReportsEnterAndExit() throws {
        #if !SKIP
        throw XCTSkip("Compose pointer input is Android-only")
        #else
        composeRule.setContent {
            HoverProbe().Compose()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hover-state").assertTextEquals("out")

        composeRule.onNodeWithTag("hover-target").performMouseInput {
            moveTo(center)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hover-state").assertTextEquals("in")

        composeRule.onNodeWithTag("hover-target").performMouseInput {
            moveTo(Offset(x: Float(-50.0), y: Float(-50.0)))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hover-state").assertTextEquals("out")
        #endif
    }

    func testRightClickOpensContextMenu() throws {
        #if !SKIP
        throw XCTSkip("Compose pointer input is Android-only")
        #else
        composeRule.setContent {
            ContextMenuProbe().Compose()
        }
        composeRule.waitForIdle()
        XCTAssertTrue(composeRule.onAllNodesWithText("Copy accession").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithTag("menu-target").performMouseInput {
            rightClick(center)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Copy accession").assertIsDisplayed()
        // The right-click opened the menu; it did not also tap the content.
        composeRule.onNodeWithTag("tap-count").assertTextEquals("0")
        #endif
    }
}

struct HoverProbe: View {
    @State var hovering = false

    var body: some View {
        VStack {
            Text("Hover me")
                .padding(40.0)
                .accessibilityIdentifier("hover-target")
                .onHover { hovering = $0 }
            Text(hovering ? "in" : "out")
                .accessibilityIdentifier("hover-state")
        }
    }
}

struct ContextMenuProbe: View {
    @State var taps = 0

    var body: some View {
        VStack {
            Button("GSE12345") { taps += 1 }
                .padding(40.0)
                .accessibilityIdentifier("menu-target")
                .contextMenu {
                    Button("Copy accession") { }
                }
            Text("\(taps)")
                .accessibilityIdentifier("tap-count")
        }
    }
}
