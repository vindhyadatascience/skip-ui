// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ProposedWidthIntrinsicsComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    /**
     * RNABrowser-owl1: a parent that asks for intrinsic sizes (a flexible height inside a scroll
     * view does) must not crash on a `ProposedWidthReader`. BoxWithConstraints is a
     * SubcomposeLayout and throws on any intrinsic query; the reader answers with its last
     * measured size instead.
     */
    @Test fun anIntrinsicQueryDoesNotCrashAndTheContentStillLaysOut() {
        rule.setContent {
            Column(Modifier.width(200.dp).height(IntrinsicSize.Min)) {
                ProposedWidthBox(Modifier.testTag("reader")) { _ ->
                    Box(Modifier.size(80.dp, 40.dp).testTag("content"))
                }
            }
        }
        rule.onNodeWithTag("content").assertExists().assertWidthIsEqualTo(80.dp)
    }

    @Test fun maxIntrinsicWidthAndHeightAreAnsweredToo() {
        rule.setContent {
            Column(Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Max)) {
                ProposedWidthBox(Modifier) { _ -> Box(Modifier.size(30.dp).testTag("small")) }
            }
        }
        rule.onNodeWithTag("small").assertExists()
    }
}
