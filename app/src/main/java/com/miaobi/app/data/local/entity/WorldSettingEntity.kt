package com.miaobi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "world_settings",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storyId")]
)
data class WorldSettingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storyId: Long,
    val name: String,
    val content: String,
    val category: String = "general", // general, location, history, culture, magic, tech
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
