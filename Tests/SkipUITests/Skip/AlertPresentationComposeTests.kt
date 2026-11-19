// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class AlertPresentationComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun waitForText(text: String) {
        rule.waitUntil(5000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    /** The tapped button must see the data its isPresented binding is derived from. */
    @Test fun alertActionRunsBeforeTheBindingClears() {
        rule.setContent { AlertBindingProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Ask alert").performClick()
        waitForText("Delete")
        rule.onNodeWithText("Delete").performClick()
        waitForText("Alert deleted row")
        rule.onNodeWithText("Alert deleted row").assertExists()
    }

    @Test fun confirmationDialogActionRunsBeforeTheBindingClears() {
        rule.setContent { ConfirmationBindingProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Ask dialog").performClick()
        waitForText("Delete row")
        rule.onNodeWithText("Delete row").performClick()
        waitForText("Dialog deleted row")
        rule.onNodeWithText("Dialog deleted row").assertExists()
    }

    /** A lone cancel-role button is the alert's only button; no default OK is added beside it. */
    @Test fun loneCancelButtonIsTheOnlyButton() {
        rule.setContent { LoneCancelAlertProbe().Compose(ComposeContext()) }
        rule.onNodeWithText("Show notice").performClick()
        waitForText("Something happened.")
        rule.onAllNodesWithText("OK").assertCountEquals(1)
        rule.onNodeWithText("OK").performClick()
        waitForText("Acknowledged")
        rule.onNodeWithText("Acknowledged").assertExists()
    }
}
