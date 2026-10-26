// Copyright 2026 Vindhya Data Science
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class NativeGridImplicitIDRouteTests {
    private fun erasedID(value: skip.lib.AnyHashable): skip.lib.AnyHashable = value

    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun rawProxyIDFindsAnIntrinsicForEachTarget() = checkRoute(erasedProxy = false)
    @Test fun erasedProxyIDFindsTheSameIntrinsicForEachTarget() = checkRoute(erasedProxy = true)

    private fun checkRoute(erasedProxy: Boolean) {
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            intrinsicProbe(erasedProxy).Compose(ComposeContext())
        } }
        rule.onNodeWithText("Raw grid ID 39").assertExists().assertIsNotDisplayed()
        rule.onNodeWithText("Scroll intrinsic ID").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Raw grid ID 39").assertIsDisplayed()
    }

    private fun intrinsicProbe(erasedProxy: Boolean): View = ScrollViewReader { proxy ->
        VStack { ComposeBuilder { context ->
            Button(title = "Scroll intrinsic ID", action = {
                val id: Any = if (erasedProxy) erasedID(39) else 39
                proxy.scrollTo(id)
            }).Compose(context)
            ScrollView { VStack { ComposeBuilder { inner ->
                Text(verbatim = "Before raw grid").Compose(inner)
                LazyVGrid(columns = skip.lib.arrayOf(GridItem(), GridItem()), spacing = 4.0) {
                    ForEach(skip.lib.Array((0 until 40).toList(), nocopy = true), id = { it }) { index ->
                        Text(verbatim = "Raw grid ID $index").frame(height = 30.0)
                    }
                }.Compose(inner)
                Text(verbatim = "After raw grid").Compose(inner)
                ComposeResult.ok
            } } }.frame(width = 240.0, height = 160.0).Compose(context)
            ComposeResult.ok
        } }
    }
}
