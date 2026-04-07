package com.miaobi.app.domain.usecase

import com.miaobi.app.domain.model.AiMessage
import com.miaobi.app.domain.model.AiStreamResponse
import com.miaobi.app.domain.model.InspirationOption
import com.miaobi.app.domain.model.InspirationType
import com.miaobi.app.domain.model.LengthOption
import com.miaobi.app.domain.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GenerateInspirationUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    /**
     * 生成多条剧情发展方向灵感
     *
     * @param currentContent 当前正文内容
     * @param characters 角色设定文本
     * @param worldSettings 世界观设定文本
     * @param count 生成数量（3~5）
     * @param typeFilter 指定类型筛选（可选）
     * @return Flow，渐进式 emit 结果
     */
    operator fun invoke(
        currentContent: String,
        characters: String,
        worldSettings: String,
        count: Int = 4,
        typeFilter: InspirationType? = null
    ): Flow<List<InspirationOption>> = flow {
        val types = if (typeFilter != null) {
            listOf(typeFilter)
        } else {
            InspirationType.entries.shuffled().take(count)
        }

        // 初始化空的选项状态
        val initialOptions = types.mapIndexed { idx, type ->
            InspirationOption(
                index = idx,
                type = type,
                title = "",
                content = ""
            )
        }
        emit(initialOptions)

        // 并行生成所有灵感
        val results = withContext(Dispatchers.IO) {
            types.mapIndexed { idx, type ->
                async {
                    generateSingleInspiration(
                        currentContent = currentContent,
                        characters = characters,
                        worldSettings = worldSettings,
                        type = type,
                        index = idx
                    )
                }
            }.awaitAll()
        }

        emit(results)
    }

    private suspend fun generateSingleInspiration(
        currentContent: String,
        characters: String,
        worldSettings: String,
        type: InspirationType,
        index: Int
    ): InspirationOption {
        val fullContent = StringBuilder()
        var error: String? = null

        val messages = buildInspirationPrompt(
            currentContent = currentContent,
            characters = characters,
            worldSettings = worldSettings,
            type = type
        )

        aiRepository.continueStoryWithSystem(
            messages = messages,
            lengthOption = LengthOption.MEDIUM
        ).collect { response ->
            if (response.error != null) {
                error = response.error
            } else if (!response.isFinished) {
                fullContent.append(response.content)
            }
        }

        val content = fullContent.toString()

        // 生成标题
        val title = if (content.isNotBlank() && error == null) {
            generateTitle(content, type)
        } else {
            "${type.emoji} ${type.label}"
        }

        return InspirationOption(
            index = index,
            type = type,
            title = title,
            content = content,
            summaryTag = type.emoji,
            isSelected = false,
            isGenerating = false,
            isFavorite = false,
            error = error
        )
    }

    private suspend fun generateTitle(content: String, type: InspirationType): String {
        var title: String? = null
        val messages = listOf(
            AiMessage(
                role = "system",
                content = """
                    你是一个小说灵感标题生成专家。
                    根据以下灵感内容，为其生成一个简短有力的标题。
                    要求：
                    1. 8个字以内
                    2. 能概括这个剧情发展方向的核心
                    3. 直接输出标题，不要任何解释或符号
                """.trimIndent()
            ),
            AiMessage(
                role = "user",
                content = "灵感内容：\n${content.take(300)}"
            )
        )

        aiRepository.continueStoryWithSystem(
            messages = messages,
            lengthOption = LengthOption.SHORT
        ).collect { response ->
            if (!response.isFinished && response.error == null && response.content.isNotBlank()) {
                title = response.content.trim()
            }
        }

        return title ?: "${type.emoji} ${type.label}"
    }

    private fun buildInspirationPrompt(
        currentContent: String,
        characters: String,
        worldSettings: String,
        type: InspirationType
    ): List<AiMessage> {
        val typeGuidance = when (type) {
            InspirationType.PLOT_ESCALATION -> """
                【剧情发展方向】剧情升级
                请围绕以下方向设计剧情发展：
                - 设计一个让冲突爆发的事件或转折
                - 加快故事节奏，制造紧张感
                - 让矛盾升级到新的层次
                - 可能涉及敌人对抗、竞争、危机等元素
            """.trimIndent()

            InspirationType.EMOTIONAL_INTERACTION -> """
                【剧情发展方向】情感互动
                请围绕以下方向设计剧情发展：
                - 深化角色之间的情感联系
                - 通过互动展现人物性格和内心
                - 营造细腻的氛围和情感张力
                - 可能涉及爱情、友情、亲情等情感线索
            """.trimIndent()

            InspirationType.UNEXPECTED_TWIST -> """
                【剧情发展方向】意外转折
                请围绕以下方向设计剧情发展：
                - 引入一个意想不到的转折或新角色
                - 打破读者预期，创造惊喜
                - 改变故事的原有走向
                - 可能涉及身份揭露、命运逆转、神秘人物等元素
            """.trimIndent()

            InspirationType.CHARACTER_GROWTH -> """
                【剧情发展方向】角色成长
                请围绕以下方向设计剧情发展：
                - 聚焦角色的内心世界和心理变化
                - 展现角色的成长弧线或内心挣扎
                - 深化人物的思想深度和情感复杂度
                - 通过内心独白或关键抉择展现角色魅力
            """.trimIndent()

            InspirationType.SUSPENSE_SETUP -> """
                【剧情发展方向】悬念设置
                请围绕以下方向设计剧情发展：
                - 埋下伏笔，为后续剧情做铺垫
                - 设置悬念，吸引读者继续阅读
                - 创造神秘感或暗示未来的冲突
                - 可能涉及未解之谜、隐藏秘密、危险预兆等元素
            """.trimIndent()

            InspirationType.WORLD_EXPANSION -> """
                【剧情发展方向】世界观扩展
                请围绕以下方向设计剧情发展：
                - 展开世界观设定的更多细节
                - 引入新的规则、势力或地域
                - 丰富故事背景的层次感
                - 让读者对故事世界有更深入的了解
            """.trimIndent()
        }

        val systemPrompt = """
            你是一位专业的小说剧情策划师。
            你的任务是根据当前小说内容，生成多条有趣的剧情发展方向建议。
            
            角色设定：
            ${if (characters.isNotBlank()) characters else "无"}
            
            世界观设定：
            ${if (worldSettings.isNotBlank()) worldSettings else "无"}
            
            要求：
            1. 每个方向需要包含：标题（8字以内）+ 详细剧情描述（200-400字）
            2. 描述要具体、有画面感，可以直接用于写作参考
            3. 每个方向要有独特的创意，不要重复
            4. 保持与原内容的风格一致性
            5. 直接输出内容，不要序号或解释
            
            $typeGuidance
        """.trimIndent()

        val userContent = buildString {
            appendLine("当前小说内容：")
            appendLine("---")
            appendLine(currentContent.take(2000))
            appendLine("---")
            appendLine()
            appendLine("请根据以上内容，提供 1 个「${type.label}」方向的详细剧情发展建议。")
            appendLine("格式：先输出标题（8字以内），换行后输出详细描述（200-400字）。")
        }

        return listOf(
            AiMessage(role = "system", content = systemPrompt),
            AiMessage(role = "user", content = userContent)
        )
    }
}
