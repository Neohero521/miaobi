package com.miaobi.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.miaobi.app.MainActivity
import org.junit.Rule
import org.junit.Test

class BookshelfScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `bookshelf displays empty state message when no stories`() {
        composeTestRule.onNodeWithText("暂无故事").assertExists()
    }

    @Test
    fun `bookshelf has create button`() {
        composeTestRule.onNodeWithContentDescription("创建故事").assertExists()
    }

    @Test
    fun `create button opens dialog`() {
        composeTestRule.onNodeWithContentDescription("创建故事").performClick()

        composeTestRule.onNodeWithText("创建新故事").assertExists()
    }

    @Test
    fun `create dialog has title input`() {
        composeTestRule.onNodeWithContentDescription("创建故事").performClick()

        composeTestRule.onNodeWithText("故事标题").assertExists()
    }

    @Test
    fun `create dialog has description input`() {
        composeTestRule.onNodeWithContentDescription("创建故事").performClick()

        composeTestRule.onNodeWithText("故事简介").assertExists()
    }

    @Test
    fun `create dialog has cancel button`() {
        composeTestRule.onNodeWithContentDescription("创建故事").performClick()

        composeTestRule.onNodeWithText("取消").assertExists()
    }

    @Test
    fun `create dialog has confirm button`() {
        composeTestRule.onNodeWithContentDescription("创建故事").performClick()

        composeTestRule.onNodeWithText("创建").assertExists()
    }

    @Test
    fun `cancel button closes dialog`() {
        composeTestRule.onNodeWithContentDescription("创建故事").performClick()
        composeTestRule.onNodeWithText("取消").performClick()

        composeTestRule.onNodeWithText("创建新故事").assertDoesNotExist()
    }

    @Test
    fun `bookshelf has settings button`() {
        composeTestRule.onNodeWithContentDescription("设置").assertExists()
    }

    @Test
    fun `story items are clickable`() {
        // When stories exist, they should be clickable
        composeTestRule.onAllOf(
            hasTestTag("StoryItem"),
            isEnabled(),
            isClickable()
        ).onFirst().assertExists()
    }
}
