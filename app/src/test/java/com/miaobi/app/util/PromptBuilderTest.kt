package com.miaobi.app.util

import com.miaobi.app.domain.model.AiMessage
import org.junit.Assert.*
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun `buildStoryContinuationPrompt returns two messages`() {
        val result = PromptBuilder.buildStoryContinuationPrompt(
            currentContent = "故事开始",
            characters = "小明: 主角",
            worldSettings = "古代中国",
            userInstruction = "继续发展"
        )

        assertEquals(2, result.size)
        assertEquals("system", result[0].role)
        assertEquals("user", result[1].role)
    }

    @Test
    fun `buildStoryContinuationPrompt system message contains characters`() {
        val result = PromptBuilder.buildStoryContinuationPrompt(
            currentContent = "故事",
            characters = "小明: 主角\n小红: 配角",
            worldSettings = ""
        )

        val systemContent = result[0].content
        assertTrue(systemContent.contains("小明"))
        assertTrue(systemContent.contains("小红"))
    }

    @Test
    fun `buildStoryContinuationPrompt system message contains world settings`() {
        val result = PromptBuilder.buildStoryContinuationPrompt(
            currentContent = "故事",
            characters = "",
            worldSettings = "古代中国\n武侠世界"
        )

        val systemContent = result[0].content
        assertTrue(systemContent.contains("古代中国"))
        assertTrue(systemContent.contains("武侠世界"))
    }

    @Test
    fun `buildStoryContinuationPrompt user message contains content`() {
        val result = PromptBuilder.buildStoryContinuationPrompt(
            currentContent = "从前有座山",
            characters = "",
            worldSettings = ""
        )

        val userContent = result[1].content
        assertTrue(userContent.contains("从前有座山"))
    }

    @Test
    fun `buildStoryContinuationPrompt uses instruction when provided`() {
        val result = PromptBuilder.buildStoryContinuationPrompt(
            currentContent = "故事",
            characters = "",
            worldSettings = "",
            userInstruction = "让主角遇到困难"
        )

        val userContent = result[1].content
        assertTrue(userContent.contains("让主角遇到困难"))
    }

    @Test
    fun `buildStoryContinuationPrompt uses default instruction when not provided`() {
        val result = PromptBuilder.buildStoryContinuationPrompt(
            currentContent = "故事",
            characters = "",
            worldSettings = ""
        )

        val userContent = result[1].content
        assertTrue(userContent.contains("继续这个故事"))
    }

    @Test
    fun `buildStoryContinuationPrompt with empty characters`() {
        val result = PromptBuilder.buildStoryContinuationPrompt(
            currentContent = "故事",
            characters = "",
            worldSettings = "设定"
        )

        assertEquals(2, result.size)
        val systemContent = result[0].content
        assertTrue(systemContent.contains("角色设定：\n\n"))
    }

    @Test
    fun `buildTitleGenerationPrompt returns two messages`() {
        val result = PromptBuilder.buildTitleGenerationPrompt(
            content = "小说内容",
            characters = "角色",
            worldSettings = "设定"
        )

        assertEquals(2, result.size)
        assertEquals("system", result[0].role)
        assertEquals("user", result[1].role)
    }

    @Test
    fun `buildTitleGenerationPrompt system message contains instructions`() {
        val result = PromptBuilder.buildTitleGenerationPrompt(
            content = "",
            characters = "",
            worldSettings = ""
        )

        val systemContent = result[0].content
        assertTrue(systemContent.contains("标题"))
        assertTrue(systemContent.contains("2-8个字"))
    }

    @Test
    fun `buildTitleGenerationPrompt user message contains content`() {
        val result = PromptBuilder.buildTitleGenerationPrompt(
            content = "一段精彩的小说内容",
            characters = "角色设定",
            worldSettings = "世界观设定"
        )

        val userContent = result[1].content
        assertTrue(userContent.contains("一段精彩的小说内容"))
        assertTrue(userContent.contains("角色设定"))
        assertTrue(userContent.contains("世界观设定"))
    }

    @Test
    fun `buildTitleGenerationPrompt truncates long content`() {
        val longContent = "A".repeat(3000)
        val result = PromptBuilder.buildTitleGenerationPrompt(
            content = longContent,
            characters = "",
            worldSettings = ""
        )

        val userContent = result[1].content
        // Should be truncated to 2000 chars
        assertTrue(userContent.length <= 2050) // 2000 + some overhead for prefix
    }

    @Test
    fun `system prompt contains writing requirements`() {
        val result = PromptBuilder.buildStoryContinuationPrompt(
            currentContent = "故事",
            characters = "",
            worldSettings = ""
        )

        val systemContent = result[0].content
        assertTrue(systemContent.contains("500字"))
        assertTrue(systemContent.contains("文风"))
    }
}
