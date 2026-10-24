// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import org.junit.Test

/** A real modifier gesture writes through a plain struct's nested projected Binding. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class BindingProjectionComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun modifierAndPlainStructWriteBackNestedState() {
        rule.setContent { BindingProjectionRuntimeProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Value 5").assertIsDisplayed()
        rule.onNodeWithTag("binding-projection-action").performTouchInput { click() }
        rule.onNodeWithText("Value 6").assertIsDisplayed()
        rule.onNodeWithTag("binding-projection-action").performTouchInput { click() }
        rule.onNodeWithText("Value 7").assertIsDisplayed()
        rule.onNodeWithText("Siblings inner outer").assertIsDisplayed()
    }
}
