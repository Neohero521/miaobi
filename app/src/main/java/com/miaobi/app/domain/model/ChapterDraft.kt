package com.miaobi.app.domain.model

data class ChapterDraft(
    val id: Long = 0,
    val chapterId: Long,
    val content: String,
    val wordCount: Int = 0,
    val version: Int = 1,
    val isCurrent: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChapterDraftVersion(
    val id: Long = 0,
    val draftId: Long,
    val version: Int,
    val content: String,
    val wordCount: Int = 0,
    val diffSummary: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
