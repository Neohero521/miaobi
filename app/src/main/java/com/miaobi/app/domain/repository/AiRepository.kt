package com.miaobi.app.domain.repository

import com.miaobi.app.domain.model.AiMessage
import com.miaobi.app.domain.model.AiStreamResponse
import com.miaobi.app.domain.model.LengthOption
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    /**
     * 带自定义 system messages 的续写（用于多分支差异化 prompt）
     */
    fun continueStoryWithSystem(
        messages: List<AiMessage>,
        lengthOption: LengthOption = LengthOption.MEDIUM
    ): Flow<AiStreamResponse>

    fun continueStory(
        prompt: String,
        characters: String,
        worldSettings: String,
        historyContent: String,
        lengthOption: LengthOption = LengthOption.MEDIUM
    ): Flow<AiStreamResponse>

    fun generateTitle(
        content: String,
        characters: String,
        worldSettings: String
    ): Flow<AiStreamResponse>

    suspend fun saveDraft(chapterId: Long, content: String): Long

    fun rewriteText(
        originalText: String,
        style: com.miaobi.app.domain.model.RewriteStyle,
        characters: String,
        worldSettings: String
    ): Flow<com.miaobi.app.domain.model.AiStreamResponse>
}
