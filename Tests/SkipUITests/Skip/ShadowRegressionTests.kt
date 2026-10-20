// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ShadowRegressionTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun shadowComposesOneChildAndPreservesLayout() {
        val tick = mutableStateOf(0)
        var compositions = 0
        rule.setContent {
            val current = tick.value
            Shadowed(ComposeContext(), Color.Black.copy(alpha = 0.33f), 3.dp, 5.dp, 8.dp) { context ->
                SideEffect { compositions++ }
                Box(context.modifier.size((40 + current).dp, 30.dp).background(Color.Red).testTag("shadow-child"))
            }
        }
        rule.onNodeWithTag("shadow-child").assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(30.dp)
        rule.runOnIdle {
            assertEquals("A shadow must not compose its child a second time", 1, compositions)
            tick.value = 1
        }
        rule.onNodeWithTag("shadow-child").assertWidthIsEqualTo(41.dp)
        rule.runOnIdle { assertEquals(2, compositions) }
    }
}
