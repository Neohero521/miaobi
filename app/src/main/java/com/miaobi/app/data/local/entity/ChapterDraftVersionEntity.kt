package com.miaobi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapter_draft_versions",
    foreignKeys = [
        ForeignKey(
            entity = ChapterDraftEntity::class,
            parentColumns = ["id"],
            childColumns = ["draftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("draftId")]
)
data class ChapterDraftVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val draftId: Long,
    val version: Int,
    val content: String,
    val wordCount: Int = 0,
    val diffSummary: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
