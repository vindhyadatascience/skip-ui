// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import android.os.Build
import android.view.View as AndroidView
import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test

/** Requires a real OS IME animation; helper simulations cannot establish page recomposition. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class KeyboardInsetRecompositionComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun requireOSIME() {
        assumeFalse("Requires Android OS IME animation", Build.FINGERPRINT.contains("robolectric"))
    }

    @Test fun navigationAndSheetDoNotReevaluateTheirWholeBodyEveryIMEFrameV1() { checkIME(1) }
    @Test fun navigationAndSheetDoNotReevaluateTheirWholeBodyEveryIMEFrameV2() { checkIME(2) }

    private fun checkIME(version: Int) {
        val navigation = KeyboardBodyCounter()
        val sheet = KeyboardBodyCounter()
        rule.setContent {
            EnvironmentValues.shared.setValues({ values ->
                values.set_layoutImplementationVersion(version)
                ComposeResult.ok
            }, {
                NavigationKeyboardRuntimeProbe(navigation, sheet).Compose(ComposeContext())
            })
        }
        repeat(3) { checkField("navigation-ime-field", navigation) }
        rule.onNodeWithText("Show keyboard sheet").performClick()
        repeat(3) { checkField("sheet-ime-field", sheet) }
        android.util.Log.i("SkipUIIMEProof", "Layout $version repeated navigation/sheet IME cycles passed; navigation=${navigation.bodies}, sheet=${sheet.bodies}")
    }

    // Sheets have their own window: reading the activity's insets would miss its IME.
    private fun focusedRoot(): AndroidView =
        WindowInspector.getGlobalWindowViews().firstOrNull { it.hasWindowFocus() }
            ?: rule.activity.window.decorView

    private fun imeVisible(): Boolean =
        ViewCompat.getRootWindowInsets(focusedRoot())?.isVisible(WindowInsetsCompat.Type.ime()) == true

    private fun checkField(tag: String, counter: KeyboardBodyCounter) {
        rule.waitForIdle()
        val before = counter.bodies
        rule.onNodeWithTag(tag).performClick()
        rule.waitUntil(10_000) { imeVisible() }
        rule.waitForIdle()
        rule.onNodeWithTag("$tag-stable").assertWidthIsEqualTo(120.dp).assertHeightIsEqualTo(30.dp)
        val afterShow = counter.bodies
        assertTrue("Whole content re-evaluated ${afterShow - before} times while IME opened", afterShow - before <= 4)
        rule.runOnUiThread {
            val root = focusedRoot()
            ViewCompat.getWindowInsetsController(root)?.hide(WindowInsetsCompat.Type.ime())
            root.findFocus()?.clearFocus()
        }
        rule.waitUntil(10_000) { !imeVisible() }
        rule.waitForIdle()
        val afterHide = counter.bodies
        assertTrue("Whole content re-evaluated ${afterHide - afterShow} times while IME closed", afterHide - afterShow <= 4)
    }
}
