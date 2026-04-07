package com.miaobi.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.miaobi.app.MainActivity
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `settings screen has API key input`() {
        composeTestRule.onNodeWithText("API Key").assertExists()
    }

    @Test
    fun `settings screen has API URL input`() {
        composeTestRule.onNodeWithText("API 地址").assertExists()
    }

    @Test
    fun `settings screen has model name input`() {
        composeTestRule.onNodeWithText("模型名称").assertExists()
    }

    @Test
    fun `settings screen has save button`() {
        composeTestRule.onNodeWithText("保存设置").assertExists()
    }

    @Test
    fun `API URL has default value`() {
        composeTestRule.onNodeWithText("https://api.siliconflow.cn/v1").assertExists()
    }

    @Test
    fun `model name has default value`() {
        composeTestRule.onNodeWithText("Qwen/Qwen2.5-7B-Instruct").assertExists()
    }

    // ===== Bug Fix 3: API Key visibility toggle icons =====

    @Test
    fun `API key field has visibility toggle button`() {
        // The toggle button should exist for API Key field
        // Initially showApiKey is false, so LockOpen icon should be shown
        composeTestRule.onNodeWithContentDescription("显示").assertExists()
    }

    @Test
    fun `clicking visibility toggle changes icon from LockOpen to VisibilityOff`() {
        // Initially the icon should be LockOpen (hidden state)
        composeTestRule.onNodeWithContentDescription("显示").performClick()
        
        // After clicking, it should show "隐藏" content description with VisibilityOff icon
        composeTestRule.onNodeWithContentDescription("隐藏").assertExists()
    }

    @Test
    fun `clicking visibility toggle twice returns to original state`() {
        // Click to show
        composeTestRule.onNodeWithContentDescription("显示").performClick()
        // Should now show "隐藏"
        composeTestRule.onNodeWithContentDescription("隐藏").assertExists()
        
        // Click to hide again
        composeTestRule.onNodeWithContentDescription("隐藏").performClick()
        // Should return to "显示"
        composeTestRule.onNodeWithContentDescription("显示").assertExists()
    }

    @Test
    fun `API key input accepts text`() {
        composeTestRule.onNodeWithText("API Key").performTextInput("my-secret-key")
        composeTestRule.onNodeWithText("my-secret-key").assertExists()
    }
}
