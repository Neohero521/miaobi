package com.miaobi.app.domain.model

data class StoryTemplate(
    val id: Long = 0,
    val title: String,
    val genre: String,
    val summary: String,
    val charactersJson: String? = null,
    val worldSettingsJson: String? = null,
    val promptTemplate: String,
    val coverImage: String? = null,
    val isBuiltIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
