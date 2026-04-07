package com.miaobi.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.miaobi.app.MainActivity
import org.junit.Rule
import org.junit.Test

class WritingScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `writing screen has content text field`() {
        composeTestRule.onNodeWithText("开始写作...").assertExists()
    }

    @Test
    fun `writing screen has prompt input`() {
        composeTestRule.onNodeWithText("输入写作指令").assertExists()
    }

    @Test
    fun `writing screen has generate button`() {
        composeTestRule.onNodeWithText("续写").assertExists()
    }

    @Test
    fun `writing screen has chapter list button`() {
        composeTestRule.onNodeWithContentDescription("章节列表").assertExists()
    }

    @Test
    fun `writing screen has character sheet button`() {
        composeTestRule.onNodeWithContentDescription("角色设定").assertExists()
    }

    @Test
    fun `writing screen has world setting button`() {
        composeTestRule.onNodeWithContentDescription("世界观设定").assertExists()
    }

    @Test
    fun `writing screen has draft history button`() {
        composeTestRule.onNodeWithContentDescription("草稿历史").assertExists()
    }

    @Test
    fun `chapter list button opens chapter sheet`() {
        composeTestRule.onNodeWithContentDescription("章节列表").performClick()

        composeTestRule.onNodeWithText("添加章节").assertExists()
    }

    @Test
    fun `character sheet button opens character sheet`() {
        composeTestRule.onNodeWithContentDescription("角色设定").performClick()

        composeTestRule.onNodeWithText("添加角色").assertExists()
    }

    @Test
    fun `world setting button opens setting sheet`() {
        composeTestRule.onNodeWithContentDescription("世界观设定").performClick()

        composeTestRule.onNodeWithText("添加世界观设定").assertExists()
    }

    @Test
    fun `draft history button opens draft sheet`() {
        composeTestRule.onNodeWithContentDescription("草稿历史").performClick()

        composeTestRule.onNodeWithText("选择草稿版本").assertExists()
    }

    @Test
    fun `add chapter dialog has title input`() {
        composeTestRule.onNodeWithContentDescription("章节列表").performClick()
        composeTestRule.onNodeWithText("添加章节").performClick()

        composeTestRule.onNodeWithText("章节标题").assertExists()
    }

    @Test
    fun `add character dialog has name input`() {
        composeTestRule.onNodeWithContentDescription("角色设定").performClick()
        composeTestRule.onNodeWithText("添加角色").performClick()

        composeTestRule.onNodeWithText("角色名称").assertExists()
    }

    @Test
    fun `add character dialog has description input`() {
        composeTestRule.onNodeWithContentDescription("角色设定").performClick()
        composeTestRule.onNodeWithText("添加角色").performClick()

        composeTestRule.onNodeWithText("角色描述").assertExists()
    }

    @Test
    fun `add world setting dialog has name input`() {
        composeTestRule.onNodeWithContentDescription("世界观设定").performClick()
        composeTestRule.onNodeWithText("添加世界观设定").performClick()

        composeTestRule.onNodeWithText("设定名称").assertExists()
    }

    @Test
    fun `content text field accepts input`() {
        composeTestRule.onNodeWithText("开始写作...").performTextInput("新内容")

        composeTestRule.onNodeWithText("新内容").assertExists()
    }

    @Test
    fun `prompt input accepts text`() {
        composeTestRule.onNodeWithText("输入写作指令").performTextInput("续写故事")

        composeTestRule.onNodeWithText("续写故事").assertExists()
    }
}
