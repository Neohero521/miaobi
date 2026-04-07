package com.miaobi.app.domain.usecase

import com.miaobi.app.domain.model.AiStreamResponse
import com.miaobi.app.domain.repository.AiRepository
import com.miaobi.app.util.PromptBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContinueStoryUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    operator fun invoke(
        prompt: String,
        characters: String,
        worldSettings: String,
        historyContent: String
    ): Flow<AiStreamResponse> {
        return aiRepository.continueStory(
            prompt = prompt,
            characters = characters,
            worldSettings = worldSettings,
            historyContent = historyContent
        )
    }
}

class GenerateTitleUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    operator fun invoke(
        content: String,
        characters: String,
        worldSettings: String
    ): Flow<AiStreamResponse> {
        return aiRepository.generateTitle(
            content = content,
            characters = characters,
            worldSettings = worldSettings
        )
    }
}

class SaveChapterDraftUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(chapterId: Long, content: String): Long {
        return aiRepository.saveDraft(chapterId, content)
    }
}

class GenerateBranchesUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    /**
     * 并行生成多条不同走向的续写分支
     *
     * @param currentContent 当前正文内容
     * @param characters 角色设定文本
     * @param worldSettings 世界观设定文本
     * @param userInstruction 用户指令（可选）
     * @param branchCount 分支数量（2/3/4）
     * @param lengthOption 续写长度选项
     * @return Flow，每条分支完成时 emit 一次（渐进式 UI 更新）
     */
    operator fun invoke(
        currentContent: String,
        characters: String,
        worldSettings: String,
        userInstruction: String?,
        branchCount: Int,
        lengthOption: com.miaobi.app.domain.model.LengthOption
    ): Flow<List<com.miaobi.app.domain.model.BranchOption>> = flow {
        // 初始化空的分支状态
        val initialBranches = (0 until branchCount).map { i ->
            com.miaobi.app.domain.model.BranchOption(index = i)
        }
        emit(initialBranches)

        // 并行生成所有分支
        val results = withContext(Dispatchers.IO) {
            (0 until branchCount).map { i ->
                async {
                    generateSingleBranch(
                        currentContent = currentContent,
                        characters = characters,
                        worldSettings = worldSettings,
                        userInstruction = userInstruction,
                        branchIndex = i,
                        lengthOption = lengthOption
                    )
                }
            }.awaitAll()
        }

        // 所有分支完成，emit 最终结果
        emit(results)
    }

    private suspend fun generateSingleBranch(
        currentContent: String,
        characters: String,
        worldSettings: String,
        userInstruction: String?,
        branchIndex: Int,
        lengthOption: com.miaobi.app.domain.model.LengthOption
    ): com.miaobi.app.domain.model.BranchOption {
        val fullContent = StringBuilder()
        var isFinished = false
        var error: String? = null

        // 构建分支差异化 prompt
        val messages = PromptBuilder.buildBranchContinuationPrompt(
            currentContent = currentContent,
            characters = characters,
            worldSettings = worldSettings,
            userInstruction = userInstruction,
            branchIndex = branchIndex,
            lengthOption = lengthOption
        )

        // 流式收集续写内容
        aiRepository.continueStoryWithSystem(
            messages = messages,
            lengthOption = lengthOption
        ).collect { response ->
            if (response.error != null) {
                error = response.error
            } else if (response.isFinished) {
                isFinished = true
            } else {
                fullContent.append(response.content)
            }
        }

        val content = fullContent.toString()

        // 生成摘要标签（并行）
        val summaryTag = if (content.isNotBlank() && error == null) {
            generateSummaryTag(content)
        } else {
            null
        }

        return com.miaobi.app.domain.model.BranchOption(
            index = branchIndex,
            content = content,
            summaryTag = summaryTag ?: if (error != null) "⚠️ 生成失败" else "📖 未知走向",
            isSelected = false,
            isGenerating = false,
            error = error
        )
    }

    private suspend fun generateSummaryTag(content: String): String? {
        return withContext(Dispatchers.IO) {
            var tag: String? = null
            val messages = PromptBuilder.buildBranchSummaryPrompt(content)
            aiRepository.continueStoryWithSystem(
                messages = messages,
                lengthOption = com.miaobi.app.domain.model.LengthOption.SHORT
            ).collect { response ->
                if (!response.isFinished && response.error == null && response.content.isNotBlank()) {
                    tag = response.content.trim()
                }
            }
            tag
        }
    }
}

class RewriteTextUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    operator fun invoke(
        originalText: String,
        style: com.miaobi.app.domain.model.RewriteStyle,
        characters: String,
        worldSettings: String
    ): Flow<com.miaobi.app.domain.model.AiStreamResponse> {
        return aiRepository.rewriteText(
            originalText = originalText,
            style = style,
            characters = characters,
            worldSettings = worldSettings
        )
    }
}
