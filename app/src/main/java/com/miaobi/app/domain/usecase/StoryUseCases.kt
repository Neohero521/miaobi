package com.miaobi.app.domain.usecase

import com.miaobi.app.domain.model.Story
import com.miaobi.app.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllStoriesUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    operator fun invoke(): Flow<List<Story>> {
        return storyRepository.getAllStories()
    }
}

class GetStoryByIdUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    operator fun invoke(storyId: Long): Flow<Story?> {
        return storyRepository.getStoryById(storyId)
    }
}

class CreateStoryUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    suspend operator fun invoke(title: String, description: String): Long {
        val story = Story(
            title = title.ifBlank { "无标题故事" },
            description = description
        )
        return storyRepository.insertStory(story)
    }
}

class UpdateStoryUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    suspend operator fun invoke(story: Story) {
        storyRepository.updateStory(story.copy(updatedAt = System.currentTimeMillis()))
    }
}

class DeleteStoryUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    suspend operator fun invoke(storyId: Long) {
        storyRepository.deleteStory(storyId)
    }
}
