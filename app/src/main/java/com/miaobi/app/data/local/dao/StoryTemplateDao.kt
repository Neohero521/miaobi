package com.miaobi.app.data.local.dao

import androidx.room.*
import com.miaobi.app.data.local.entity.StoryTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryTemplateDao {
    @Query("SELECT * FROM story_templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<StoryTemplateEntity>>

    @Query("SELECT * FROM story_templates WHERE genre = :genre ORDER BY createdAt DESC")
    fun getTemplatesByGenre(genre: String): Flow<List<StoryTemplateEntity>>

    @Query("SELECT * FROM story_templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: Long): StoryTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: StoryTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<StoryTemplateEntity>)

    @Delete
    suspend fun deleteTemplate(template: StoryTemplateEntity)

    @Query("SELECT COUNT(*) FROM story_templates")
    suspend fun getTemplateCount(): Int
}
