package com.miaobi.app.data.repository

import com.miaobi.app.data.local.dao.ChapterDraftDao
import com.miaobi.app.data.local.entity.ChapterDraftEntity
import com.miaobi.app.data.local.entity.ChapterDraftVersionEntity
import com.miaobi.app.data.remote.AiSseClient
import com.miaobi.app.domain.model.AiConfig
import com.miaobi.app.domain.model.AiMessage
import com.miaobi.app.domain.model.AiRequest
import com.miaobi.app.domain.model.AiStreamResponse
import com.miaobi.app.domain.model.LengthOption
import com.miaobi.app.domain.model.RewriteStyle
import com.miaobi.app.domain.repository.AiRepository
import com.miaobi.app.util.PromptBuilder
import com.miaobi.app.util.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val aiSseClient: AiSseClient,
    private val settingsManager: SettingsManager,
    private val chapterDraftDao: ChapterDraftDao
) : AiRepository {

    override fun continueStoryWithSystem(
        messages: List<AiMessage>,
        lengthOption: LengthOption
    ): Flow<AiStreamResponse> {
        return kotlinx.coroutines.flow.flow {
            val config: AiConfig = settingsManager.aiConfig.first()
            val apiKey = config.apiKey
            if (apiKey.isBlank()) {
                emit(AiStreamResponse(content = "", isFinished = true, error = "请先在设置中配置API Key"))
                return@flow
            }

            val request = AiRequest(
                model = config.modelName,
                messages = messages,
                stream = true,
                temperature = 0.8f,
                maxTokens = lengthOption.maxTokens,
                lengthOption = lengthOption
            )

            aiSseClient.streamChat(
                apiUrl = config.apiUrl,
                apiKey = apiKey,
                request = request
            ).collect { response ->
                emit(response)
            }
        }
    }

    override fun continueStory(
        prompt: String,
        characters: String,
        worldSettings: String,
        historyContent: String,
        lengthOption: LengthOption
    ): Flow<AiStreamResponse> {
        return kotlinx.coroutines.flow.flow {
            val config: AiConfig = settingsManager.aiConfig.first()
            val apiKey = config.apiKey
            if (apiKey.isBlank()) {
                emit(AiStreamResponse(content = "", isFinished = true, error = "请先在设置中配置API Key"))
                return@flow
            }

            val messages = PromptBuilder.buildStoryContinuationPrompt(
                currentContent = historyContent,
                characters = characters,
                worldSettings = worldSettings,
                userInstruction = prompt,
                lengthOption = lengthOption
            )

            val request = AiRequest(
                model = config.modelName,
                messages = messages,
                stream = true,
                temperature = 0.8f,
                maxTokens = lengthOption.maxTokens,
                lengthOption = lengthOption
            )

            aiSseClient.streamChat(
                apiUrl = config.apiUrl,
                apiKey = apiKey,
                request = request
            ).collect { response ->
                emit(response)
            }
        }
    }

    override fun generateTitle(
        content: String,
        characters: String,
        worldSettings: String
    ): Flow<AiStreamResponse> {
        return kotlinx.coroutines.flow.flow {
            val config: AiConfig = settingsManager.aiConfig.first()
            val apiKey = config.apiKey
            if (apiKey.isBlank()) {
                emit(AiStreamResponse(content = "", isFinished = true, error = "请先在设置中配置API Key"))
                return@flow
            }

            val messages = PromptBuilder.buildTitleGenerationPrompt(
                content = content,
                characters = characters,
                worldSettings = worldSettings
            )

            val request = AiRequest(
                model = config.modelName,
                messages = messages,
                stream = false,
                temperature = 0.7f,
                maxTokens = 50
            )

            val response = aiSseClient.nonStreamChat(
                apiUrl = config.apiUrl,
                apiKey = apiKey,
                request = request
            )
            emit(response)
        }
    }

    override suspend fun saveDraft(chapterId: Long, content: String): Long {
        // Get current draft count for this chapter
        val count = chapterDraftDao.getDraftCount(chapterId)
        
        // If we already have 20 drafts, delete the oldest (by createdAt)
        if (count >= 20) {
            chapterDraftDao.deleteOldestDraft(chapterId)
        }
        
        val wordCount = content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        val draft = ChapterDraftEntity(
            chapterId = chapterId,
            content = content,
            wordCount = wordCount,
            version = count + 1,
            createdAt = System.currentTimeMillis()
        )
        
        // Insert draft
        val draftId = chapterDraftDao.insertDraft(draft)
        
        // Save version snapshot
        val version = ChapterDraftVersionEntity(
            draftId = draftId,
            version = count + 1,
            content = content,
            wordCount = wordCount,
            createdAt = System.currentTimeMillis()
        )
        chapterDraftDao.insertDraftVersion(version)
        
        return draftId
    }

    override fun rewriteText(
        originalText: String,
        style: RewriteStyle,
        characters: String,
        worldSettings: String
    ): Flow<AiStreamResponse> {
        return kotlinx.coroutines.flow.flow {
            val config: AiConfig = settingsManager.aiConfig.first()
            val apiKey = config.apiKey
            if (apiKey.isBlank()) {
                emit(AiStreamResponse(content = "", isFinished = true, error = "请先在设置中配置API Key"))
                return@flow
            }

            val messages = PromptBuilder.buildRewritePrompt(
                originalText = originalText,
                style = style,
                characters = characters,
                worldSettings = worldSettings
            )

            val request = AiRequest(
                model = config.modelName,
                messages = messages,
                stream = true,
                temperature = 0.85f,
                maxTokens = 800
            )

            aiSseClient.streamChat(
                apiUrl = config.apiUrl,
                apiKey = apiKey,
                request = request
            ).collect { response ->
                emit(response)
            }
        }
    }
}
