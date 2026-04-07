package com.miaobi.app.data.repository

import com.miaobi.app.data.local.dao.StoryTemplateDao
import com.miaobi.app.domain.model.StoryTemplate
import com.miaobi.app.domain.repository.StoryTemplateRepository
import com.miaobi.app.util.toDomain
import com.miaobi.app.util.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryTemplateRepositoryImpl @Inject constructor(
    private val storyTemplateDao: StoryTemplateDao
) : StoryTemplateRepository {

    override fun getAllTemplates(): Flow<List<StoryTemplate>> {
        return storyTemplateDao.getAllTemplates().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTemplatesByGenre(genre: String): Flow<List<StoryTemplate>> {
        return storyTemplateDao.getTemplatesByGenre(genre).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTemplateById(templateId: Long): StoryTemplate? {
        return storyTemplateDao.getTemplateById(templateId)?.toDomain()
    }

    override suspend fun insertTemplate(template: StoryTemplate): Long {
        return storyTemplateDao.insertTemplate(template.toEntity())
    }

    override suspend fun insertTemplates(templates: List<StoryTemplate>) {
        storyTemplateDao.insertTemplates(templates.map { it.toEntity() })
    }

    override suspend fun deleteTemplate(template: StoryTemplate) {
        storyTemplateDao.deleteTemplate(template.toEntity())
    }

    override suspend fun getTemplateCount(): Int {
        return storyTemplateDao.getTemplateCount()
    }
}
