package com.miaobi.app.domain.model

data class WorldSetting(
    val id: Long = 0,
    val storyId: Long,
    val name: String,
    val content: String,
    val category: String = "general",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
