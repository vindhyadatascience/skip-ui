// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Test

/** Keeps the original assertions while reporting the actual tree at the failing state transition. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ForEachStateIdentityDiagnosticTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun collectionReportsActualStateBeforeAndAfterReorder() = report(0)
    @Test fun rangeReportsActualStateBeforeAndAfterSlide() = report(1)
    @Test fun bindingReportsActualStateBeforeAndAfterReorder() = report(2)

    private fun report(mode: Int) {
        rule.setContent { OptionalIDForEachProbe(mode).Compose(ComposeContext()) }
        snapshot(mode, "mounted")
        rule.onNodeWithText("nil count 0").performClick()
        rule.onNodeWithText("nil count 1").assertIsDisplayed()
        snapshot(mode, "incremented")
        rule.onNodeWithText("Move optional IDs").performClick()
        rule.waitForIdle()
        snapshot(mode, "reordered")
        rule.onNodeWithText("nil count 1").assertIsDisplayed()
    }

    private fun snapshot(mode: Int, phase: String) {
        println("FOREACH_STATE_DIAGNOSTIC mode=$mode phase=$phase\n" +
            rule.onRoot(useUnmergedTree = true).printToString())
    }
}
