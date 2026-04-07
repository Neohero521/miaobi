package com.miaobi.app.domain.model

data class Story(
    val id: Long = 0,
    val title: String,
    val description: String,
    val templateType: String = "free",
    val coverImage: String? = null,
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
