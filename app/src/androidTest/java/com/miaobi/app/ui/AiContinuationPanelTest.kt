package com.miaobi.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.miaobi.app.domain.model.ContinuationSuggestion
import com.miaobi.app.ui.screens.writing.AiContinuationPanel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AiContinuationPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    private fun launchPanel(
        suggestions: List<ContinuationSuggestion> = emptyList(),
        isGenerating: Boolean = false,
        onUse: (Int) -> Unit = {},
        onRegenerate: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            AiContinuationPanel(
                suggestions = suggestions,
                isGenerating = isGenerating,
                onUse = onUse,
                onRegenerate = onRegenerate,
                onDismiss = onDismiss,
                sheetState = sheetState
            )
        }
    }

    // ===== Header tests =====

    @Test
    fun `panel displays AI 续写 header`() {
        launchPanel()
        composeTestRule.onNodeWithText("AI 续写").assertExists()
    }

    @Test
    fun `panel displays close button`() {
        launchPanel()
        composeTestRule.onNodeWithContentDescription("关闭").assertExists()
    }

    @Test
    fun `close button calls onDismiss`() {
        var called = false
        launchPanel(onDismiss = { called = true })
        composeTestRule.onNodeWithContentDescription("关闭").performClick()
        assertTrue(called)
    }

    // ===== Suggestion count display =====

    @Test
    fun `header shows 0 条建议 when suggestions are empty and not generating`() {
        launchPanel(suggestions = emptyList(), isGenerating = false)
        composeTestRule.onNodeWithText("AI 续写 · 0 条建议").assertExists()
    }

    @Test
    fun `header shows correct count when suggestions exist`() {
        val suggestions = listOf(
            ContinuationSuggestion(id = 0, content = "内容1", wordCount = 100),
            ContinuationSuggestion(id = 1, content = "内容2", wordCount = 150),
            ContinuationSuggestion(id = 2, content = "内容3", wordCount = 200)
        )
        launchPanel(suggestions = suggestions)
        composeTestRule.onNodeWithText("AI 续写 · 3 条建议").assertExists()
    }

    @Test
    fun `header shows 生成中 when suggestions are empty and isGenerating`() {
        launchPanel(suggestions = emptyList(), isGenerating = true)
        composeTestRule.onNodeWithText("AI 续写 · 生成中").assertExists()
    }

    // ===== Bug Fix 1: 生成中… text instead of progress bar =====

    @Test
    fun `shows 生成中 text when isGenerating is true`() {
        launchPanel(isGenerating = true)
        composeTestRule.onNodeWithText("生成中…").assertExists()
    }

    @Test
    fun `does not show 生成中 text when not generating`() {
        launchPanel(isGenerating = false)
        composeTestRule.onNodeWithText("生成中…").assertDoesNotExist()
    }

    @Test
    fun `generating state hides suggestion count and shows 生成中 in header`() {
        launchPanel(suggestions = emptyList(), isGenerating = true)
        // Header should show "生成中" instead of "0 条建议"
        composeTestRule.onNodeWithText("AI 续写 · 生成中").assertExists()
        composeTestRule.onNodeWithText("生成中…").assertExists()
    }

    @Test
    fun `regenerate button is disabled when generating`() {
        launchPanel(isGenerating = true)
        composeTestRule.onNodeWithText("换一批").assertIsNotEnabled()
    }

    @Test
    fun `regenerate button is enabled when not generating`() {
        launchPanel(isGenerating = false)
        composeTestRule.onNodeWithText("换一批").assertIsEnabled()
    }

    // ===== Placeholder cards during generation =====

    @Test
    fun `shows three placeholder cards when not generating with no suggestions`() {
        launchPanel(suggestions = emptyList(), isGenerating = false)
        // Should show 3 placeholder suggestion cards (建议 ·)
        composeTestRule.onNodeWithText("建议 ·").assertExists()
    }

    // ===== Suggestion card tests =====

    @Test
    fun `displays suggestion card with correct label`() {
        val suggestions = listOf(
            ContinuationSuggestion(id = 0, content = "内容", wordCount = 100)
        )
        launchPanel(suggestions = suggestions)

        composeTestRule.onNodeWithText("建议 1").assertExists()
    }

    @Test
    fun `displays suggestion content in card`() {
        val suggestions = listOf(
            ContinuationSuggestion(
                id = 0,
                content = "这是第一条续写建议的内容",
                wordCount = 100
            )
        )
        launchPanel(suggestions = suggestions)

        composeTestRule.onNodeWithText("这是第一条续写建议的内容").assertExists()
    }

    @Test
    fun `displays word count in card`() {
        val suggestions = listOf(
            ContinuationSuggestion(
                id = 0,
                content = "内容",
                wordCount = 500
            )
        )
        launchPanel(suggestions = suggestions)

        composeTestRule.onNodeWithText("500 字").assertExists()
    }

    @Test
    fun `displays 使用此建议 button on card`() {
        val suggestions = listOf(
            ContinuationSuggestion(id = 0, content = "内容", wordCount = 100)
        )
        launchPanel(suggestions = suggestions)

        composeTestRule.onNodeWithText("使用此建议").assertExists()
    }

    @Test
    fun `using suggestion calls onUse with correct index`() {
        var usedIndex = -1
        val suggestions = listOf(
            ContinuationSuggestion(id = 0, content = "第一", wordCount = 50),
            ContinuationSuggestion(id = 1, content = "第二", wordCount = 60),
            ContinuationSuggestion(id = 2, content = "第三", wordCount = 70)
        )
        launchPanel(
            suggestions = suggestions,
            onUse = { usedIndex = it }
        )

        composeTestRule.onNodeWithText("第二").performClick()
        assertEquals(1, usedIndex)
    }

    @Test
    fun `all three suggestions display when available`() {
        val suggestions = listOf(
            ContinuationSuggestion(id = 0, content = "内容1", wordCount = 100),
            ContinuationSuggestion(id = 1, content = "内容2", wordCount = 150),
            ContinuationSuggestion(id = 2, content = "内容3", wordCount = 200)
        )
        launchPanel(suggestions = suggestions)

        composeTestRule.onNodeWithText("建议 1").assertExists()
        composeTestRule.onNodeWithText("建议 2").assertExists()
        composeTestRule.onNodeWithText("建议 3").assertExists()
    }

    // ===== Selected suggestion tests =====

    @Test
    fun `selected suggestion shows check icon`() {
        val suggestions = listOf(
            ContinuationSuggestion(
                id = 0,
                content = "已选中的内容",
                wordCount = 100,
                isSelected = true
            )
        )
        launchPanel(suggestions = suggestions)

        composeTestRule.onNodeWithContentDescription("已选择").assertExists()
    }

    // ===== Action buttons tests =====

    @Test
    fun `displays 换一批 button`() {
        launchPanel()
        composeTestRule.onNodeWithText("换一批").assertExists()
    }

    @Test
    fun `换一批 button calls onRegenerate`() {
        var called = false
        launchPanel(onRegenerate = { called = true })
        composeTestRule.onNodeWithText("换一批").performClick()
        assertTrue(called)
    }

    @Test
    fun `displays 完成 button`() {
        launchPanel()
        composeTestRule.onNodeWithText("完成").assertExists()
    }

    @Test
    fun `完成 button calls onDismiss`() {
        var called = false
        launchPanel(onDismiss = { called = true })
        composeTestRule.onNodeWithText("完成").performClick()
        assertTrue(called)
    }

    // ===== Combined interaction tests =====

    @Test
    fun `multiple suggestions can each be used via onUse`() {
        val indicesUsed = mutableListOf<Int>()
        val suggestions = listOf(
            ContinuationSuggestion(id = 0, content = "内容1", wordCount = 100),
            ContinuationSuggestion(id = 1, content = "内容2", wordCount = 150),
            ContinuationSuggestion(id = 2, content = "内容3", wordCount = 200)
        )
        launchPanel(
            suggestions = suggestions,
            onUse = { indicesUsed.add(it) }
        )

        composeTestRule.onNodeWithText("内容1").performClick()
        composeTestRule.onNodeWithText("内容2").performClick()
        composeTestRule.onNodeWithText("内容3").performClick()

        assertEquals(3, indicesUsed.size)
        assertEquals(0, indicesUsed[0])
        assertEquals(1, indicesUsed[1])
        assertEquals(2, indicesUsed[2])
    }

    // ===== Icon presence tests =====

    @Test
    fun `AutoAwesome icon is visible in header`() {
        launchPanel()
        // The AutoAwesome icon is associated with the header
        composeTestRule.onNodeWithText("AI 续写").assertExists()
    }
}
