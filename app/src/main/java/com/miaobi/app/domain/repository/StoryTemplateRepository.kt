package com.miaobi.app.domain.repository

import com.miaobi.app.domain.model.StoryTemplate
import kotlinx.coroutines.flow.Flow

interface StoryTemplateRepository {
    fun getAllTemplates(): Flow<List<StoryTemplate>>
    fun getTemplatesByGenre(genre: String): Flow<List<StoryTemplate>>
    suspend fun getTemplateById(templateId: Long): StoryTemplate?
    suspend fun insertTemplate(template: StoryTemplate): Long
    suspend fun insertTemplates(templates: List<StoryTemplate>)
    suspend fun deleteTemplate(template: StoryTemplate)
    suspend fun getTemplateCount(): Int
}
