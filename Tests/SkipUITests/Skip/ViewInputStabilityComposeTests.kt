// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The public Swift child is reconstructed by its parent, rather than a hand-written Kotlin stand-in. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ViewInputStabilityComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun anUnchangedValueChildDoesNotRerunWhileChangedInputStillUpdates() {
        val unchanged = ViewBodyCounter()
        val changed = ViewBodyCounter()
        rule.setContent { ViewInputStabilityProbe(unchanged, changed).Compose(ComposeContext()) }
        rule.waitForIdle()
        val initialUnchanged = unchanged.bodies
        val initialChanged = changed.bodies
        assertTrue(initialUnchanged > 0)
        repeat(3) { index ->
            rule.onNodeWithText("Invalidate only parent input").performClick()
            rule.onNodeWithText("Parent ${index + 1}").assertIsDisplayed()
            rule.onNodeWithText("Child value ${index + 1}").assertIsDisplayed()
        }
        rule.waitForIdle()
        assertEquals("Stable primitive and class-reference inputs must preserve the child body", initialUnchanged, unchanged.bodies)
        assertTrue("Changed value input must evaluate its child", changed.bodies >= initialChanged + 3)
    }
}
