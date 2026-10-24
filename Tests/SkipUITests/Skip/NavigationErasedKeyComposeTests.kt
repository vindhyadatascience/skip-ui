// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Test

/** The public Swift NavigationLink/destination chain exercises the real typed KClass dictionary. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class NavigationErasedKeyComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun equalValueEqualHashClassesSelectTheirOwnTypedDestinationAndReturn() {
        rule.setContent { NavigationErasedKeyRuntimeProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Open first equal-hash class").performClick()
        awaitDisplayed("First route 7")
        rule.onNodeWithText("First route 7").assertIsDisplayed()
        rule.onNodeWithText("Second route 7").assertDoesNotExist()
        rule.onNodeWithText("Back from erased route").performClick()
        awaitDisplayed("Open second equal-hash class")
        rule.onNodeWithText("Open second equal-hash class").performClick()
        awaitDisplayed("Second route 7")
        rule.onNodeWithText("Second route 7").assertIsDisplayed()
        rule.onNodeWithText("First route 7").assertDoesNotExist()
    }
    @Test fun subclassRouteFindsTheRegisteredSuperclassDestinationThroughTheErasedKeyBase() {
        rule.setContent { NavigationErasedKeyRuntimeProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Open subclass through base type").performClick()
        awaitDisplayed("Base route 9")
        rule.onNodeWithText("Base route 9").assertIsDisplayed()
        rule.onNodeWithText("Back from erased route").performClick()
        awaitDisplayed("Open subclass through base type")
        rule.onNodeWithText("Open subclass through base type").assertIsDisplayed()
    }
    private fun awaitDisplayed(text: String) {
        // Navigation3 entry placement/transition completes after the click callback.
        try {
            rule.waitUntil(5000) {
                kotlin.runCatching { rule.onNodeWithText(text).assertIsDisplayed() }.isSuccess
            }
        } catch (failure: Throwable) {
            throw AssertionError("Navigation placement for $text\n" + rule.onRoot(useUnmergedTree = true).printToString(), failure)
        }
    }

}
