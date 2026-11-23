// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/// `.id(value)` content that leaves composition inside a SaveableStateHolder (a
/// NavigationStack entry covered by a push) must restore against the saver that saved it.
@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class IdStateRestorationComposeTests {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun anIdShownAgainAfterItsEntryWasRestoredUnderAnotherIdStartsFresh() {
        val id = mutableStateOf("first")
        val shown = mutableStateOf(true)
        rule.setContent {
            val holder = rememberSaveableStateHolder()
            val context = remember { ComposeContext() }
            if (shown.value) {
                holder.SaveableStateProvider("entry") {
                    IdStateRestorationProbe().id(id.value).Compose(context)
                }
            }
        }
        rule.onNodeWithText("Count 0").performClick()
        rule.onNodeWithText("Count 1").assertIsDisplayed()
        // Covered while showing "first" (a tab switch, a push): its state is saved.
        rule.runOnIdle { shown.value = false }
        rule.waitForIdle()
        // A link replaces the item before the entry is shown again.
        rule.runOnIdle { id.value = "second"; shown.value = true }
        rule.onNodeWithText("Count 0").assertIsDisplayed()
        // A later link brings "first" back in place: a new identity, so it starts fresh.
        rule.runOnIdle { id.value = "first" }
        rule.onNodeWithText("Count 0").performClick()
        rule.onNodeWithText("Count 1").assertIsDisplayed()
    }

    @Test fun contentKeepsItsStateAcrossRepeatedRestoresAndResetsOnANewId() {
        val id = mutableStateOf("first")
        val shown = mutableStateOf(true)
        rule.setContent {
            val holder = rememberSaveableStateHolder()
            val context = remember { ComposeContext() }
            if (shown.value) {
                holder.SaveableStateProvider("entry") {
                    IdStateRestorationProbe().id(id.value).Compose(context)
                }
            }
        }
        rule.onNodeWithText("Count 0").performClick()
        for (round in 0 until 3) {
            rule.runOnIdle { shown.value = false }
            rule.waitForIdle()
            rule.runOnIdle { shown.value = true }
            rule.onNodeWithText("Count 1").assertIsDisplayed()
        }
        rule.runOnIdle { shown.value = false }
        rule.waitForIdle()
        // A different id while covered: the restored content must start fresh, not resume.
        rule.runOnIdle { id.value = "second"; shown.value = true }
        rule.onNodeWithText("Count 0").assertIsDisplayed()
    }
}
