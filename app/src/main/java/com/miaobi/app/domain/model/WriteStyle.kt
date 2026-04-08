package com.miaobi.app.domain.model

/**
 * 续写风格枚举（彩云小梦风格）
 */
enum class WriteStyle(
    val label: String,
    val description: String
) {
    STANDARD("标准", "通用续写，保持原文风格"),
    ROMANCE("言情", "言情小说风格，侧重情感描写"),
    XUANHUAN("玄幻", "玄幻修仙风格，奇幻元素"),
    PURE_LOVE("纯爱", "清新纯爱风格，甜蜜温馨");

    companion object {
        fun fromTag(tag: String): WriteStyle {
            return entries.find { it.name.equals(tag, ignoreCase = true) } ?: STANDARD
        }
    }
}
