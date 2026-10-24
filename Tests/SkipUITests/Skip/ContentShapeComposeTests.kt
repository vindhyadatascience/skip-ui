// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import org.junit.Test

/** Actual pointer routing, including the sibling behind the shaped target. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ContentShapeComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun install(variant: Int) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                ContentShapeRuntimeProbe(variant).Compose(ComposeContext())
            }
        }
    }

    private fun tap(x: Float, y: Float) {
        rule.onNodeWithTag("content-shape-target").performTouchInput { click(Offset(x, y)) }
    }

    private fun checkCircleFallThrough(variant: Int) {
        install(variant)
        rule.onNodeWithTag("content-shape-target").assertWidthIsEqualTo(100.dp).assertHeightIsEqualTo(100.dp)
        tap(2f, 2f)
        rule.onNodeWithText("Under 1").assertIsDisplayed()
        rule.onNodeWithText("Over 0").assertIsDisplayed()
        tap(50f, 50f)
        rule.onNodeWithText("Under 1").assertIsDisplayed()
        rule.onNodeWithText("Over 1").assertIsDisplayed()
    }

    @Test fun innerContentShapeFiltersAndFallsThrough() { checkCircleFallThrough(0) }
    @Test fun outerContentShapeFiltersAndFallsThrough() { checkCircleFallThrough(1) }

    @Test fun evenOddHoleFallsThroughToSibling() {
        install(2)
        tap(50f, 50f)
        rule.onNodeWithText("Under 1").assertIsDisplayed()
        rule.onNodeWithText("Over 0").assertIsDisplayed()
        tap(5f, 50f)
        rule.onNodeWithText("Over 1").assertIsDisplayed()
    }

    @Test fun nonZeroFillKeepsTheSameWindingCenterInteractive() {
        install(3)
        tap(50f, 50f)
        rule.onNodeWithText("Under 0").assertIsDisplayed()
        rule.onNodeWithText("Over 1").assertIsDisplayed()
    }

    @Test fun explicitRectangleOverridesTheDrawnCirclesHitRegion() {
        install(4)
        tap(2f, 2f)
        rule.onNodeWithText("Under 0").assertIsDisplayed()
        rule.onNodeWithText("Over 1").assertIsDisplayed()
    }
    @Test fun outerRectangleOverridesTheBareDrawnShapeBeforeItsGestureNodes() {
        install(5)
        tap(2f, 2f)
        rule.onNodeWithText("Under 0").assertIsDisplayed()
        rule.onNodeWithText("Over 1").assertIsDisplayed()
    }

}
