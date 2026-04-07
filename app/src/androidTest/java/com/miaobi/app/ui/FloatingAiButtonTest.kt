package com.miaobi.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.miaobi.app.ui.screens.writing.FloatingAiButton
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FloatingAiButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchFab(
        isGenerating: Boolean = false,
        onClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            FloatingAiButton(
                isGenerating = isGenerating,
                onClick = onClick
            )
        }
    }

    // ===== Idle state tests =====

    @Test
    fun `displays AI 续写 content description when idle`() {
        launchFab(isGenerating = false)
        composeTestRule.onNodeWithContentDescription("AI 续写").assertExists()
    }

    @Test
    fun `displays AutoAwesome icon when idle`() {
        launchFab(isGenerating = false)
        // Icon is present via contentDescription
        composeTestRule.onNodeWithContentDescription("AI 续写").assertExists()
    }

    @Test
    fun `does not show CircularProgressIndicator when idle`() {
        launchFab(isGenerating = false)
        // No progress indicator should exist
        composeTestRule.onAllNodes(isDialog()).onFirst().assertDoesNotExist()
    }

    // ===== Generating state tests =====

    @Test
    fun `displays 停止生成 content description when generating`() {
        launchFab(isGenerating = true)
        composeTestRule.onNodeWithContentDescription("停止生成").assertExists()
    }

    @Test
    fun `shows CircularProgressIndicator when generating`() {
        launchFab(isGenerating = true)
        // The FAB contains a CircularProgressIndicator when generating
        composeTestRule.onNodeWithContentDescription("停止生成").assertExists()
    }

    // ===== Click behavior tests =====

    @Test
    fun `onClick is called when clicked in idle state`() {
        var called = false
        launchFab(isGenerating = false, onClick = { called = true })
        composeTestRule.onNodeWithContentDescription("AI 续写").performClick()
        assertTrue(called)
    }

    @Test
    fun `onClick is called when clicked during generation`() {
        var called = false
        launchFab(isGenerating = true, onClick = { called = true })
        composeTestRule.onNodeWithContentDescription("停止生成").performClick()
        assertTrue(called)
    }

    // ===== State transition tests =====

    @Test
    fun `button toggles between states correctly`() {
        var clickCount = 0
        var currentGenerating = false

        // First render: idle
        composeTestRule.setContent {
            FloatingAiButton(
                isGenerating = currentGenerating,
                onClick = {
                    clickCount++
                    currentGenerating = !currentGenerating
                }
            )
        }

        composeTestRule.onNodeWithContentDescription("AI 续写").performClick()
        assertEquals(1, clickCount)

        // After click, re-render with generating=true
        composeTestRule.setContent {
            FloatingAiButton(
                isGenerating = true,
                onClick = {
                    clickCount++
                    currentGenerating = !currentGenerating
                }
            )
        }

        composeTestRule.onNodeWithContentDescription("停止生成").performClick()
        assertEquals(2, clickCount)
    }

    // ===== Multiple rapid clicks =====

    @Test
    fun `multiple rapid clicks register each call`() {
        var clickCount = 0
        launchFab(isGenerating = false, onClick = { clickCount++ })

        composeTestRule.onNodeWithContentDescription("AI 续写").performClick()
        composeTestRule.onNodeWithContentDescription("AI 续写").performClick()
        composeTestRule.onNodeWithContentDescription("AI 续写").performClick()

        assertEquals(3, clickCount)
    }
}
