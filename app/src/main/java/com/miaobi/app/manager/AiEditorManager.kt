package com.miaobi.app.manager

import com.miaobi.app.domain.model.AiStreamResponse
import com.miaobi.app.domain.model.LengthOption
import com.miaobi.app.domain.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI编辑功能统一管理类
 * 实现润色、改写、扩写、续写、灵感生成等核心功能
 * 适配 Compose 协程作用域
 * 
 * 使用方式（通过 Hilt 注入）：
 * @Inject lateinit var aiManager: AiEditorManager
 */
@Singleton
class AiEditorManager @Inject constructor(
    private val aiRepository: AiRepository
) {
    companion object {
        // 续写长度对应字数
        const val LENGTH_SHORT = 300
        const val LENGTH_MIDDLE = 600
        const val LENGTH_LONG = 1000
    }

    /**
     * 文本润色功能
     */
    suspend fun polishText(content: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                var result = ""
                aiRepository.rewriteText(
                    originalText = content,
                    style = com.miaobi.app.domain.model.RewriteStyle.CLASSICAL,
                    characters = "",
                    worldSettings = ""
                ).collect { response ->
                    if (response.error != null) {
                        result = response.error
                    } else {
                        result += response.content
                    }
                }
                if (result.isNotEmpty() && !result.contains("error")) {
                    Result.success(result)
                } else {
                    Result.failure(Exception(result.ifEmpty { "润色失败" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 文本改写功能
     */
    suspend fun rewriteText(content: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                var result = ""
                aiRepository.rewriteText(
                    originalText = content,
                    style = com.miaobi.app.domain.model.RewriteStyle.MODERN,
                    characters = "",
                    worldSettings = ""
                ).collect { response ->
                    if (response.error != null) {
                        result = response.error
                    } else {
                        result += response.content
                    }
                }
                if (result.isNotEmpty() && !result.contains("error")) {
                    Result.success(result)
                } else {
                    Result.failure(Exception(result.ifEmpty { "改写失败" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 文本扩写功能
     */
    suspend fun expandText(content: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                var result = ""
                aiRepository.rewriteText(
                    originalText = content,
                    style = com.miaobi.app.domain.model.RewriteStyle.FLOWERY,
                    characters = "",
                    worldSettings = ""
                ).collect { response ->
                    if (response.error != null) {
                        result = response.error
                    } else {
                        result += response.content
                    }
                }
                if (result.isNotEmpty() && !result.contains("error")) {
                    Result.success(result)
                } else {
                    Result.failure(Exception(result.ifEmpty { "扩写失败" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 生成情节灵感
     */
    suspend fun generateInspiration(content: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                var result = ""
                aiRepository.rewriteText(
                    originalText = content,
                    style = com.miaobi.app.domain.model.RewriteStyle.CLASSICAL,
                    characters = "",
                    worldSettings = ""
                ).collect { response ->
                    if (response.error != null) {
                        result = response.error
                    } else {
                        result += response.content
                    }
                }
                if (result.isNotEmpty() && !result.contains("error")) {
                    Result.success(result)
                } else {
                    Result.failure(Exception(result.ifEmpty { "生成灵感失败" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 小说续写功能（支持自定义方向+长度）
     */
    suspend fun continueWrite(
        content: String,
        direction: String,
        lengthType: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val wordCount = when (lengthType) {
                    "short" -> LENGTH_SHORT
                    "long" -> LENGTH_LONG
                    else -> LENGTH_MIDDLE
                }
                val lengthOption = when (lengthType) {
                    "short" -> LengthOption.SHORT
                    "long" -> LengthOption.LONG
                    else -> LengthOption.MEDIUM
                }
                val directionPrompt = if (direction.isNotEmpty()) "续写方向要求：$direction" else ""
                val prompt = "基于以下小说内容进行续写，约$wordCount 字，$directionPrompt，保持文风一致，情节流畅，小说内容：$content"

                var result = ""
                aiRepository.continueStory(
                    prompt = prompt,
                    characters = "",
                    worldSettings = "",
                    historyContent = content,
                    lengthOption = lengthOption
                ).collect { response ->
                    if (response.error != null) {
                        result = response.error
                    } else {
                        result += response.content
                    }
                }
                if (result.isNotEmpty() && !result.contains("error")) {
                    Result.success(result)
                } else {
                    Result.failure(Exception(result.ifEmpty { "续写失败" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
