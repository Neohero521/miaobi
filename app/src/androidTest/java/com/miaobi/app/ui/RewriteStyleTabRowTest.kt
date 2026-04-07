package com.miaobi.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.miaobi.app.domain.model.RewriteStyle
import com.miaobi.app.ui.screens.writing.RewriteStyleTabRow
import com.miaobi.app.ui.screens.writing.WritingStyles
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RewriteStyleTabRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchTabRow(
        selectedStyle: RewriteStyle = RewriteStyle.MODERN,
        onStyleSelected: (RewriteStyle) -> Unit = {},
        enabled: Boolean = true
    ) {
        composeTestRule.setContent {
            RewriteStyleTabRow(
                selectedStyle = selectedStyle,
                onStyleSelected = onStyleSelected,
                enabled = enabled
            )
        }
    }

    // ===== All style tabs exist =====

    @Test
    fun `displays all five style tabs`() {
        launchTabRow()

        composeTestRule.onNodeWithText("古风").assertExists()
        composeTestRule.onNodeWithText("现代").assertExists()
        composeTestRule.onNodeWithText("简洁").assertExists()
        composeTestRule.onNodeWithText("华丽").assertExists()
        composeTestRule.onNodeWithText("口语化").assertExists()
    }

    @Test
    fun `has exactly five filter chips`() {
        launchTabRow()

        // Count FilterChips by finding nodes with the style labels
        WritingStyles.forEach { style ->
            composeTestRule.onNodeWithText(style.label).assertExists()
        }
    }

    // ===== Style selection tests =====

    @Test
    fun `calls onStyleSelected when a style tab is clicked`() {
        var selectedStyle: RewriteStyle? = null
        launchTabRow(
            selectedStyle = RewriteStyle.MODERN,
            onStyleSelected = { selectedStyle = it }
        )

        composeTestRule.onNodeWithText("古风").performClick()

        assertEquals(RewriteStyle.CLASSICAL, selectedStyle)
    }

    @Test
    fun `selected style shows as selected`() {
        launchTabRow(selectedStyle = RewriteStyle.FLOWERY)

        composeTestRule.onNodeWithText("华丽").assertIsSelected()
    }

    @Test
    fun `unselected styles are not marked as selected`() {
        launchTabRow(selectedStyle = RewriteStyle.CLASSICAL)

        composeTestRule.onNodeWithText("现代").assertIsNotSelected()
        composeTestRule.onNodeWithText("简洁").assertIsNotSelected()
        composeTestRule.onNodeWithText("华丽").assertIsNotSelected()
        composeTestRule.onNodeWithText("口语化").assertIsNotSelected()
    }

    @Test
    fun `can switch selection between different styles`() {
        var lastSelected: RewriteStyle? = null
        launchTabRow(
            selectedStyle = RewriteStyle.MODERN,
            onStyleSelected = { lastSelected = it }
        )

        composeTestRule.onNodeWithText("古风").performClick()
        assertEquals(RewriteStyle.CLASSICAL, lastSelected)

        composeTestRule.onNodeWithText("简洁").performClick()
        assertEquals(RewriteStyle.CONCISE, lastSelected)

        composeTestRule.onNodeWithText("口语化").performClick()
        assertEquals(RewriteStyle.COLLOQUIAL, lastSelected)
    }

    // ===== All style labels match RewriteStyle enum =====

    @Test
    fun `all WritingStyles labels are displayed`() {
        launchTabRow()

        WritingStyles.forEach { style ->
            composeTestRule.onNodeWithText(style.label).assertExists()
        }
    }

    @Test
    fun `all five WriteStyles are present in WritingStyles list`() {
        assertEquals(5, WritingStyles.size)
        assertTrue(RewriteStyle.CLASSICAL in WritingStyles)
        assertTrue(RewriteStyle.MODERN in WritingStyles)
        assertTrue(RewriteStyle.CONCISE in WritingStyles)
        assertTrue(RewriteStyle.FLOWERY in WritingStyles)
        assertTrue(RewriteStyle.COLLOQUIAL in WritingStyles)
    }

    // ===== Enabled/disabled state tests =====

    @Test
    fun `tabs are enabled when enabled is true`() {
        launchTabRow(enabled = true)

        composeTestRule.onNodeWithText("古风").assertIsEnabled()
        composeTestRule.onNodeWithText("现代").assertIsEnabled()
        composeTestRule.onNodeWithText("简洁").assertIsEnabled()
    }

    @Test
    fun `tabs are disabled when enabled is false`() {
        launchTabRow(enabled = false)

        composeTestRule.onNodeWithText("古风").assertIsNotEnabled()
        composeTestRule.onNodeWithText("现代").assertIsNotEnabled()
        composeTestRule.onNodeWithText("简洁").assertIsNotEnabled()
        composeTestRule.onNodeWithText("华丽").assertIsNotEnabled()
        composeTestRule.onNodeWithText("口语化").assertIsNotEnabled()
    }

    @Test
    fun `clicking disabled tab does not call onStyleSelected`() {
        var called = false
        launchTabRow(
            enabled = false,
            onStyleSelected = { called = true }
        )

        composeTestRule.onNodeWithText("古风").performClick()

        assertTrue(!called)
    }

    // ===== Default selection =====

    @Test
    fun `defaults to MODERN style selected`() {
        var lastSelected: RewriteStyle? = null
        launchTabRow(
            selectedStyle = RewriteStyle.MODERN,
            onStyleSelected = { lastSelected = it }
        )

        composeTestRule.onNodeWithText("现代").assertIsSelected()
    }

    // ===== Multiple style selections =====

    @Test
    fun `clicking each style in sequence fires correct events`() {
        val selections = mutableListOf<RewriteStyle>()
        launchTabRow(
            selectedStyle = RewriteStyle.MODERN,
            onStyleSelected = { selections.add(it) }
        )

        composeTestRule.onNodeWithText("古风").performClick()
        composeTestRule.onNodeWithText("华丽").performClick()
        composeTestRule.onNodeWithText("简洁").performClick()

        assertEquals(3, selections.size)
        assertEquals(RewriteStyle.CLASSICAL, selections[0])
        assertEquals(RewriteStyle.FLOWERY, selections[1])
        assertEquals(RewriteStyle.CONCISE, selections[2])
    }
}
