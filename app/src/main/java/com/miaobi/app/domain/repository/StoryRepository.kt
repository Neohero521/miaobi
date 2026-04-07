package com.miaobi.app.domain.repository

import com.miaobi.app.domain.model.Story
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun getAllStories(): Flow<List<Story>>
    fun getStoryById(storyId: Long): Flow<Story?>
    suspend fun insertStory(story: Story): Long
    suspend fun updateStory(story: Story)
    suspend fun deleteStory(storyId: Long)
    suspend fun getStoryCount(): Int
}
