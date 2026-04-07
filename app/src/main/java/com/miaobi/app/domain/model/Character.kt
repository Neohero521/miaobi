package com.miaobi.app.domain.model

data class Character(
    val id: Long = 0,
    val storyId: Long,
    val name: String,
    val description: String,
    val avatar: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
