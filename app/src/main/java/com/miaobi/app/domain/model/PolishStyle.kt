package com.miaobi.app.domain.model

/**
 * 文本润色风格枚举
 */
enum class PolishStyle(
    val label: String,
    val description: String
) {
    SIMPLE_TO_COMPLEX("简洁→华丽", "将文本改写得更加华丽复杂，辞藻优美"),
    COMPLEX_TO_SIMPLE("复杂→简洁", "将文本改写得更加简洁明了，删繁就简"),
    CASUAL_TO_FORMAL("口语→正式", "将文本改写得更加正式规范"),
    FORMAL_TO_CASUAL("正式→口语", "将文本改写得更加口语化，自然亲切"),
    LITERARY("文艺", "文艺清新，诗意盎然"),
    FLOWERY("华丽", "辞藻华丽，优美动人");

    companion object {
        fun fromTag(tag: String): PolishStyle {
            return entries.find { it.name.equals(tag, ignoreCase = true) } ?: LITERARY
        }
    }
}
