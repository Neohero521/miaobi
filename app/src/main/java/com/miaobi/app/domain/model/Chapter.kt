package com.miaobi.app.domain.model

data class Chapter(
    val id: Long = 0,
    val storyId: Long,
    val title: String,
    val content: String,
    val orderIndex: Int,
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
