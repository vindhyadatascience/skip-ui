// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

/** Real Text formatting must receive raw Number/String arguments, not hash-wrapper objects. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class TextRawInterpolationComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var originalLocale: Locale
    @Before fun setProcessFormatterLocale() { originalLocale = Locale.getDefault(); Locale.setDefault(Locale.US) }
    @After fun restoreProcessFormatterLocale() { Locale.setDefault(originalLocale) }

    @Test fun localizedStringKeyPlainAndMarkdownPreserveIntegerFloatingAndStringValues() {
        rule.setContent { TextRawInterpolationRuntimeProbe().Compose(ComposeContext()) }
        rule.onNodeWithTag("raw-key-plain").assertTextEquals("Raw 7 / 3.500000 / alpha")
        rule.onNodeWithTag("raw-key-markdown").assertTextEquals("Raw 7 / 3.500000 / alpha")
    }

    @Test fun localizedResourcePlainAndMarkdownKeepRawNumbersAcrossTheFoundationBridge() {
        rule.setContent { TextRawInterpolationRuntimeProbe().Compose(ComposeContext()) }
        rule.onNodeWithTag("raw-resource-plain").assertTextEquals("Resource 7 / 3.500000 / alpha")
        rule.onNodeWithTag("raw-resource-markdown").assertTextEquals("Resource 7 / 3.500000 / alpha")
    }

    @Test fun concatenatedStyledRunsUseTheSameRawFormatterArguments() {
        rule.setContent { TextRawConcatenationProbe().Compose(ComposeContext()) }
        rule.onNodeWithTag("raw-concatenated-runs").assertTextEquals("Run 7 / 3.500000 / alpha end")
    }
}
