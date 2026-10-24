// Copyright 2026 Skip
// SPDX-License-Identifier: MPL-2.0
package skip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Test

/** Public in-layout sheet modifiers, not a direct call to the internal modal helper. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class SheetHostLayoutResidueComposeTests {
    @get:org.junit.Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun sheetAttachedToAButtonDoesNotPermanentlyExpandItsRow() = checkHost(true)
    @Test fun sheetAttachedToAnEntireRowKeepsItsSiblingPositionsAfterDismissal() = checkHost(false)

    private fun checkHost(sheetOnButton: Boolean) {
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            SheetHostLayoutResidueProbe(sheetOnButton).Compose(ComposeContext())
        } }
        rule.waitForIdle()
        val tags = listOf("above-inline-sheet", "in-layout-sheet-host", "below-inline-sheet")
        val before = tags.map(::geometry)
        repeat(3) { cycle ->
            rule.onNodeWithText("Open inline sheet").performClick()
            // Dialog-window placement completes asynchronously after the presenting click.
            try {
                rule.waitUntil(5000) {
                    kotlin.runCatching { rule.onNodeWithTag("inline-sheet-content").assertIsDisplayed() }.isSuccess
                }
            } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
                val roots = rule.onAllNodes(isRoot(), useUnmergedTree = true)
                val trees = (0 until roots.fetchSemanticsNodes().size).joinToString("\n") { roots[it].printToString() }
                throw AssertionError("Inline sheet failed actual placement:\n$trees", failure)
            }
            rule.onNodeWithTag("inline-sheet-content").assertIsDisplayed()
            rule.onNodeWithText("Close inline sheet").performClick()
            rule.waitUntil(5000) { rule.onAllNodesWithTag("inline-sheet-content").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithText("Inline dismissals ${cycle + 1}").assertIsDisplayed()
            rule.waitForIdle()
            tags.forEachIndexed { index, tag ->
                assertEquals("$tag must retain its complete original geometry after cycle ${cycle + 1}", before[index], geometry(tag))
            }
        }
    }
    private data class Geometry(val x: Float, val y: Float, val width: Int, val height: Int)
    private fun geometry(tag: String): Geometry {
        val coordinates = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().layoutInfo.coordinates
        val origin = coordinates.positionInRoot()
        return Geometry(origin.x, origin.y, coordinates.size.width, coordinates.size.height)
    }
}
