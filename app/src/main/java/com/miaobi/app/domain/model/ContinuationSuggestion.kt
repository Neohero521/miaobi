package com.miaobi.app.domain.model

/**
 * AI 续写建议（用于 AiContinuationPanel）
 */
data class ContinuationSuggestion(
    val id: Int,
    val content: String,
    val wordCount: Int,
    val isSelected: Boolean = false
)
