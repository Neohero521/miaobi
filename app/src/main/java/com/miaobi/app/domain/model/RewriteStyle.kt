package com.miaobi.app.domain.model

/**
 * 润色改写的风格枚举
 */
enum class RewriteStyle(
    val label: String,
    val description: String,
    val emoji: String
) {
    CLASSICAL(
        label = "古风",
        description = "文言古韵，典雅含蓄",
        emoji = "🏯"
    ),
    MODERN(
        label = "现代",
        description = "白话流畅，通俗易懂",
        emoji = "🏙️"
    ),
    CONCISE(
        label = "简洁",
        description = "简洁干练，一针见血",
        emoji = "⚡"
    ),
    FLOWERY(
        label = "华丽",
        description = "辞藻华丽，优美动人",
        emoji = "🌸"
    ),
    COLLOQUIAL(
        label = "口语化",
        description = "自然口语，生动亲切",
        emoji = "💬"
    ),
    LITERARY(
        label = "文艺",
        description = "文艺清新，诗意盎然",
        emoji = "📜"
    );

    companion object {
        const val VERSION_DELIMITER = "---VERSION_DELIMITER---"
    }
}

/**
 * 改写版本（来自 AI 的单次输出，经客户端解析拆分后得到）
 */
data class RewriteVersion(
    val index: Int,
    val content: String,
    val isSelected: Boolean = false
)

/**
 * 润色改写的完整状态
 */
data class RewriteState(
    val selectedText: String = "",
    val selectedStyle: RewriteStyle = RewriteStyle.MODERN,
    val isRewriting: Boolean = false,
    val versions: List<RewriteVersion> = emptyList(),
    val selectedVersionIndex: Int = 0,
    val isEditing: Boolean = false,
    val editingText: String = "",
    val error: String? = null
) {
    val selectedVersion: RewriteVersion?
        get() = versions.getOrNull(selectedVersionIndex)

    val canRewrite: Boolean
        get() = selectedText.isNotBlank()

    val canAccept: Boolean
        get() = versions.isNotEmpty()
}
