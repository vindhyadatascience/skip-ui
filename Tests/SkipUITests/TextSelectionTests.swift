// Copyright 2023–2026 Skip
// SPDX-License-Identifier: MPL-2.0
import SwiftUI
import XCTest

#if SKIP
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.doubleClick
#endif

/// `textSelection` on Android: text inside an enabled region can be selected with the mouse
/// and copied with the keyboard; `.disabled` inside it, and text with no selectability at all,
/// cannot.
final class TextSelectionTests: SkipUITestCase {
    // SKIP INSERT: @get:org.junit.Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    func testEnabledTextIsSelectedWithTheMouseAndCopiedWithTheKeyboard() throws {
        #if !SKIP
        throw XCTSkip("Compose selection is Android-only")
        #else
        composeRule.setContent {
            SelectableProbe(mode: SelectableProbe.enabled).Compose()
        }
        XCTAssertEqual(copiedAfterDoubleClickAndCtrlC(), "GSE12345")
        #endif
    }

    func testDisabledInsideAnEnabledRegionIsNotSelectable() throws {
        #if !SKIP
        throw XCTSkip("Compose selection is Android-only")
        #else
        composeRule.setContent {
            SelectableProbe(mode: SelectableProbe.disabledInsideEnabled).Compose()
        }
        XCTAssertEqual(copiedAfterDoubleClickAndCtrlC(), "")
        #endif
    }

    func testTextWithoutSelectabilityIsNotSelectable() throws {
        #if !SKIP
        throw XCTSkip("Compose selection is Android-only")
        #else
        composeRule.setContent {
            SelectableProbe(mode: SelectableProbe.unmarked).Compose()
        }
        XCTAssertEqual(copiedAfterDoubleClickAndCtrlC(), "")
        #endif
    }

    #if SKIP
    /// Double-clicks the probe's word with the mouse, presses Ctrl+C, and returns the clipboard
    /// text ("" when nothing was copied).
    private func copiedAfterDoubleClickAndCtrlC() -> String {
        let clipboard = composeRule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as! ClipboardManager
        clipboard.clearPrimaryClip()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("selectable-text").performMouseInput {
            doubleClick(center)
        }
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            let time = android.os.SystemClock.uptimeMillis()
            let meta = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON
            composeRule.activity.dispatchKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C, 0, meta))
            composeRule.activity.dispatchKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_C, 0, meta))
        }
        composeRule.waitForIdle()
        guard let clip = clipboard.getPrimaryClip(), clip.getItemCount() > 0 else {
            return ""
        }
        return clip.getItemAt(0).getText()?.toString() ?? ""
    }
    #endif
}

struct SelectableProbe: View {
    static let unmarked = 0
    static let enabled = 1
    static let disabledInsideEnabled = 2

    let mode: Int

    var body: some View {
        if mode == SelectableProbe.enabled {
            VStack { word }.textSelection(.enabled)
        } else if mode == SelectableProbe.disabledInsideEnabled {
            VStack { word.textSelection(.disabled) }.textSelection(.enabled)
        } else {
            VStack { word }
        }
    }

    private var word: some View {
        Text("GSE12345")
            .padding(24.0)
            .accessibilityIdentifier("selectable-text")
    }
}
