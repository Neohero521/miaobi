package com.miaobi.app.util

import com.miaobi.app.domain.model.AiMessage
import com.miaobi.app.domain.model.LengthOption
import com.miaobi.app.domain.model.RewriteStyle

object PromptBuilder {

    // 多分支方向引导（对应 2/3/4 条分支）
    private val BRANCH_HINTS = listOf(
        "续写时侧重剧情升级和冲突爆发，节奏加快，高潮迭起。",
        "续写时侧重角色情感互动，营造细腻氛围，深化人物关系。",
        "续写时引入意外转折或新角色，打破原有节奏，制造悬念。",
        "续写时探索角色内心独白，深化心理描写，丰富人物塑造。"
    )

    fun buildStoryContinuationPrompt(
        currentContent: String,
        characters: String,
        worldSettings: String,
        userInstruction: String? = null,
        lengthOption: LengthOption = LengthOption.MEDIUM
    ): List<AiMessage> {
        val systemPrompt = buildSystemPrompt(characters, worldSettings, lengthOption)
        val userContent = buildUserContentForContinuation(currentContent, userInstruction, lengthOption)

        return listOf(
            AiMessage(role = "system", content = systemPrompt),
            AiMessage(role = "user", content = userContent)
        )
    }

    /**
     * 为多分支生成构建带差异化方向引导的续写 Prompt
     */
    fun buildBranchContinuationPrompt(
        currentContent: String,
        characters: String,
        worldSettings: String,
        userInstruction: String?,
        branchIndex: Int,
        lengthOption: LengthOption = LengthOption.MEDIUM
    ): List<AiMessage> {
        val branchHint = BRANCH_HINTS.getOrElse(branchIndex) { BRANCH_HINTS.last() }

        val systemPrompt = buildSystemPrompt(characters, worldSettings, lengthOption)
            .replace(
                "请根据已有的内容，延续故事发展，保持文风一致。",
                "请根据已有的内容，延续故事发展，保持文风一致。\n\n【本次续写方向】$branchHint"
            )

        val userContent = buildUserContentForContinuation(currentContent, userInstruction, lengthOption)
        return listOf(
            AiMessage(role = "system", content = systemPrompt),
            AiMessage(role = "user", content = userContent)
        )
    }

    /**
     * 为分支内容生成摘要标签的 Prompt
     */
    fun buildBranchSummaryPrompt(branchContent: String): List<AiMessage> {
        return listOf(
            AiMessage(role = "system", content = """
                你是一个小说内容摘要专家。请为下面的小说续写内容生成一个简短有力的摘要标签。
                要求：
                1. 8个字以内
                2. 能概括这段内容的核心走向或氛围
                3. 使用 emoji + 文字的形式，如：「💥 激烈冲突」「💖 情感升温」「🔮 命运转折」「⚡ 节奏加快」
                4. 只输出标签，不要解释，不要换行
            """.trimIndent()),
            AiMessage(role = "user", content = "小说续写内容：\n${branchContent.take(500)}")
        )
    }

    fun buildTitleGenerationPrompt(
        content: String,
        characters: String,
        worldSettings: String
    ): List<AiMessage> {
        val systemPrompt = """
            你是一个专业的小说标题生成专家。
            请根据提供的小说内容、角色设定和世界观设定，生成一个吸引人的小说标题。
            要求：
            1. 标题要简洁有力，2-8个字
            2. 要能体现小说的核心主题或氛围
            3. 不要使用特殊符号或emoji
            4. 直接输出标题，不要任何解释
        """.trimIndent()

        val userContent = buildUserContentForTitle(content, characters, worldSettings)

        return listOf(
            AiMessage(role = "system", content = systemPrompt),
            AiMessage(role = "user", content = userContent)
        )
    }

    private fun buildSystemPrompt(characters: String, worldSettings: String, lengthOption: LengthOption): String {
        return """
            你是一位专业的小说作家，擅长续写故事。
            请根据已有的内容，延续故事发展，保持文风一致。
            
            角色设定：
            $characters
            
            世界观设定：
            $worldSettings
            
            要求：
            1. 保持原有文风和叙事节奏
            2. 续写内容要自然流畅，符合逻辑
            3. 不要重复已有内容，直接延续
            4. 续写字数要求：${lengthOption.minChars}-${lengthOption.maxChars}字
            5. 不要输出任何解释或说明，只输出小说内容
            6. **重要**：请一次性生成3个不同的续写方向，用 `===方向1===`、`===方向2===`、`===方向3===` 分隔
               - 方向1：剧情升级/冲突爆发方向
               - 方向2：情感互动/人物关系方向
               - 方向3：意外转折/悬念探索方向
               每个方向都是独立的完整续写，不是简单的分段
        """.trimIndent()
    }

    private fun buildUserContentForContinuation(
        currentContent: String,
        userInstruction: String?,
        lengthOption: LengthOption
    ): String {
        return buildString {
            appendLine("已有的小说内容：")
            appendLine("---")
            appendLine(currentContent)
            appendLine("---")
            if (!userInstruction.isNullOrBlank()) {
                appendLine()
                appendLine("续写要求：$userInstruction")
            } else {
                appendLine()
                appendLine("请继续这个故事，保持文风和节奏一致。")
            }
            appendLine()
            appendLine("注意：本次续写需要生成${lengthOption.minChars}-${lengthOption.maxChars}字的内容。")
        }
    }

    private fun buildUserContentForTitle(
        content: String,
        characters: String,
        worldSettings: String
    ): String {
        return buildString {
            appendLine("小说内容：")
            appendLine(content.take(2000))
            appendLine()
            appendLine("角色设定：$characters")
            appendLine()
            appendLine("世界观设定：$worldSettings")
        }
    }

    fun buildRewritePrompt(
        originalText: String,
        style: RewriteStyle,
        characters: String,
        worldSettings: String
    ): List<AiMessage> {
        val systemPrompt = buildRewriteSystemPrompt(style, characters, worldSettings)
        val userContent = buildRewriteUserContent(originalText, characters, worldSettings, style)

        return listOf(
            AiMessage(role = "system", content = systemPrompt),
            AiMessage(role = "user", content = userContent)
        )
    }

    private fun buildRewriteSystemPrompt(
        style: RewriteStyle,
        characters: String,
        worldSettings: String
    ): String {
        val styleInstructions = when (style) {
            RewriteStyle.CLASSICAL -> """
                请将原文改写成古风文言风格：
                1. 使用文言虚词和古雅措辞
                2. 适当使用对仗和成语
                3. 保持原文的意境和情感
                4. 语句凝练，富有古典韵味
            """.trimIndent()

            RewriteStyle.MODERN -> """
                请将原文改写成现代白话风格：
                1. 语言平实自然，通俗易懂
                2. 句式流畅，符合现代阅读习惯
                3. 保留原文的核心含义
                4. 避免过度修饰
            """.trimIndent()

            RewriteStyle.CONCISE -> """
                请将原文改写成简洁干练的风格：
                1. 删除冗余修饰，精炼表达
                2. 直击要害，一针见血
                3. 保持关键信息不丢失
                4. 句子简短有力
            """.trimIndent()

            RewriteStyle.FLOWERY -> """
                请将原文改写成华丽优美的风格：
                1. 使用丰富的修辞手法
                2. 辞藻华丽，描写细腻
                3. 营造浓郁的文学氛围
                4. 适当增加意象和形容
            """.trimIndent()

            RewriteStyle.COLLOQUIAL -> """
                请将原文改写成口语化风格：
                1. 语言自然亲切，如同对话
                2. 使用口语词汇和表达
                3. 句式灵活，不拘一格
                4. 生动活泼，有生活气息
            """.trimIndent()

            RewriteStyle.LITERARY -> """
                请将原文改写成文艺清新风格：
                1. 语言优美，有诗意
                2. 意象清新，意境悠远
                3. 情感表达细腻含蓄
                4. 文艺气息浓厚
            """.trimIndent()
        }

        return """
            你是一位专业的小说文字编辑，擅长根据不同风格改写文本。
            $styleInstructions
            
            角色设定（供参考，保持一致）：
            ${if (characters.isNotBlank()) characters else "无"}
            
            世界观设定（供参考）：
            ${if (worldSettings.isNotBlank()) worldSettings else "无"}
            
            要求：
            1. 直接输出改写内容，不要任何解释
            2. 输出 3 个不同的改写版本
            3. 每个版本用 "${RewriteStyle.VERSION_DELIMITER}" 分隔
            4. 不要在版本前后添加序号或标记
        """.trimIndent()
    }

    private fun buildRewriteUserContent(
        originalText: String,
        characters: String,
        worldSettings: String,
        style: RewriteStyle
    ): String {
        return buildString {
            appendLine("请将以下原文改写成${style.label}风格，输出3个不同版本：")
            appendLine("---")
            appendLine(originalText)
            appendLine("---")
            appendLine("每个版本用 \"${RewriteStyle.VERSION_DELIMITER}\" 分隔。")
        }
    }
}
