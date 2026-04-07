package com.miaobi.app.domain.model

/**
 * 灵感类型枚举（6种剧情发展方向）
 */
enum class InspirationType(
    val label: String,
    val emoji: String,
    val description: String
) {
    PLOT_ESCALATION(
        label = "剧情升级",
        emoji = "💥",
        description = "冲突爆发，高潮迭起，节奏加快"
    ),
    EMOTIONAL_INTERACTION(
        label = "情感互动",
        emoji = "💖",
        description = "角色情感互动，深化人物关系"
    ),
    UNEXPECTED_TWIST(
        label = "意外转折",
        emoji = "🔮",
        description = "引入转折或新角色，制造悬念"
    ),
    CHARACTER_GROWTH(
        label = "角色成长",
        emoji = "🌱",
        description = "探索内心独白，丰富人物塑造"
    ),
    SUSPENSE_SETUP(
        label = "悬念设置",
        emoji = "⚡",
        description = "埋下伏笔，设置悬念，吸引读者"
    ),
    WORLD_EXPANSION(
        label = "世界观扩展",
        emoji = "🌍",
        description = "展开世界观设定，增加故事深度"
    )
}
