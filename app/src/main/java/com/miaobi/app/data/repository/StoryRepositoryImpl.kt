package com.miaobi.app.data.repository

import com.miaobi.app.data.local.dao.StoryDao
import com.miaobi.app.domain.model.Story
import com.miaobi.app.domain.repository.StoryRepository
import com.miaobi.app.util.toDomain
import com.miaobi.app.util.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepositoryImpl @Inject constructor(
    private val storyDao: StoryDao
) : StoryRepository {

    override fun getAllStories(): Flow<List<Story>> {
        return storyDao.getAllStories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getStoryById(storyId: Long): Flow<Story?> {
        return storyDao.getStoryByIdFlow(storyId).map { it?.toDomain() }
    }

    override suspend fun insertStory(story: Story): Long {
        return storyDao.insertStory(story.toEntity())
    }

    override suspend fun updateStory(story: Story) {
        storyDao.updateStory(story.toEntity())
    }

    override suspend fun deleteStory(storyId: Long) {
        storyDao.deleteStoryById(storyId)
    }

    override suspend fun getStoryCount(): Int {
        return storyDao.getStoryCount()
    }
}
