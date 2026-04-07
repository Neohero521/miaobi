package com.miaobi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "story_templates")
data class StoryTemplateEntity(
    @PrimaryKey(autoGenerate = true)
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
