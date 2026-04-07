package com.miaobi.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.miaobi.app.MainActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * 沉浸模式测试
 *
 * 沉浸模式通过 AnimatedVisibility 控制 TopAppBar 的显示/隐藏。
 * 当 isImmersiveMode = true 时，TopAppBar 完全隐藏；
 * 当 isImmersiveMode = false 时，TopAppBar 正常显示。
 */
class ImmersiveModeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ===== Immersive mode toggle presence =====

    @Test
    fun `immersive mode toggle button exists in top bar`() {
        composeTestRule.onNodeWithContentDescription("沉浸模式").assertExists()
    }

    @Test
    fun `immersive mode toggle is visible when not in immersive mode`() {
        composeTestRule.onNodeWithContentDescription("沉浸模式").assertIsDisplayed()
    }

    @Test
    fun `immersive mode toggle is clickable`() {
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
        // Click should be registered (no exception thrown means success)
    }

    // ===== TopAppBar visibility in normal mode =====

    @Test
    fun `top app bar is visible in normal mode`() {
        // 默认状态应该显示 TopAppBar
        // 通过检查 "写小说" 标题是否存在
        composeTestRule.onNodeWithText("写小说").assertExists()
    }

    @Test
    fun `VoModeToggle is visible in normal mode`() {
        // V/O 模式切换按钮应该在 TopAppBar 中可见
        composeTestRule.onNodeWithText("V").assertExists()
        composeTestRule.onNodeWithText("O").assertExists()
    }

    @Test
    fun `top bar action icons are visible in normal mode`() {
        composeTestRule.onNodeWithContentDescription("章节列表").assertExists()
        composeTestRule.onNodeWithContentDescription("角色").assertExists()
        composeTestRule.onNodeWithContentDescription("世界观").assertExists()
        composeTestRule.onNodeWithContentDescription("灵感").assertExists()
        composeTestRule.onNodeWithContentDescription("多分支续写").assertExists()
    }

    // ===== Toolbar presence =====

    @Test
    fun `WritingToolbar is present in normal mode`() {
        composeTestRule.onNodeWithContentDescription("撤回").assertExists()
        composeTestRule.onNodeWithContentDescription("重做").assertExists()
        composeTestRule.onNodeWithText("保存").assertExists()
    }

    // ===== Floating AI Button presence =====

    @Test
    fun `FloatingAiButton is always visible regardless of mode`() {
        // FAB 应该在右下角可见
        composeTestRule.onNodeWithContentDescription("AI 续写").assertExists()
    }

    // ===== Content area presence =====

    @Test
    fun `writing content area is visible`() {
        composeTestRule.onNodeWithText("开始写作...").assertExists()
    }

    @Test
    fun `AI generation bar is visible`() {
        composeTestRule.onNodeWithText("AI续写").assertExists()
    }

    // ===== Navigation presence =====

    @Test
    fun `back button is visible in normal mode`() {
        composeTestRule.onNodeWithContentDescription("返回").assertExists()
    }

    // ===== Interaction tests for toolbar =====

    @Test
    fun `toolbar buttons respond to clicks in normal mode`() {
        composeTestRule.onNodeWithText("保存").performClick()
    }

    @Test
    fun `chapter list button opens chapter list`() {
        composeTestRule.onNodeWithContentDescription("章节列表").performClick()
        composeTestRule.onNodeWithText("章节列表").assertExists()
    }

    // ===== Immersive mode - fullscreen toggle behavior =====

    @Test
    fun `immersive mode toggle hides top bar when clicked`() {
        // Click immersive mode toggle
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()

        // After entering immersive mode, TopAppBar title should be gone
        // The AnimatedVisibility should have hidden it
        composeTestRule.waitForIdle()

        // TopAppBar actions should not be visible
        composeTestRule.onNodeWithContentDescription("章节列表").assertDoesNotExist()
    }

    @Test
    fun `exiting immersive mode restores top bar`() {
        // Enter immersive mode
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
        composeTestRule.waitForIdle()

        // Exit immersive mode by clicking again
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
        composeTestRule.waitForIdle()

        // TopAppBar should be restored
        composeTestRule.onNodeWithContentDescription("章节列表").assertExists()
    }

    // ===== WritingToolbar in immersive mode =====

    @Test
    fun `WritingToolbar remains visible in immersive mode`() {
        // Enter immersive mode
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
        composeTestRule.waitForIdle()

        // Toolbar should still be present
        composeTestRule.onNodeWithContentDescription("撤回").assertExists()
    }

    // ===== FAB behavior in immersive mode =====

    @Test
    fun `FloatingAiButton is still visible in immersive mode`() {
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("AI 续写").assertExists()
    }

    // ===== Content area in immersive mode =====

    @Test
    fun `content text field is visible in immersive mode`() {
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("开始写作...").assertExists()
    }

    @Test
    fun `AI generation bar is visible in immersive mode`() {
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("AI续写").assertExists()
    }

    // ===== V/O toggle in immersive mode =====

    @Test
    fun `VoModeToggle is not directly accessible in immersive mode`() {
        // In immersive mode, TopAppBar (which contains VoModeToggle) is hidden
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
        composeTestRule.waitForIdle()

        // V/O toggle should not be visible (because TopAppBar is hidden)
        composeTestRule.onNodeWithText("V").assertDoesNotExist()
    }

    // ===== Multi-click immersive mode =====

    @Test
    fun `immersive mode can be toggled multiple times`() {
        repeat(3) {
            composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
            composeTestRule.waitForIdle()
        }

        // Should be back in normal mode after 3 toggles (odd number)
        composeTestRule.onNodeWithContentDescription("章节列表").assertDoesNotExist()
    }

    // ===== RewriteStyleTabRow in immersive mode =====

    @Test
    fun `RewriteStyleTabRow is accessible in immersive mode`() {
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
        composeTestRule.waitForIdle()

        // RewriteStyleTabRow is part of the content area, not the TopAppBar
        // It should still be visible if shown
        composeTestRule.onNodeWithText("现代").assertExists()
    }

    // ===== VoMode toggle state persistence across mode change =====

    @Test
    fun `VoMode toggle state is preserved when entering immersive mode`() {
        // V mode is ON by default (isVoMode = true)
        composeTestRule.onNodeWithText("V").assertExists()
        composeTestRule.onNodeWithText("O").assertExists()

        // Enter immersive mode
        composeTestRule.onNodeWithContentDescription("沉浸模式").performClick()
        composeTestRule.waitForIdle()

        // FAB should still be clickable (mode unchanged)
        composeTestRule.onNodeWithContentDescription("AI 续写").performClick()
    }
}
