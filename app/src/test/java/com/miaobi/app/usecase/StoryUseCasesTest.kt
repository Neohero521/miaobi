package com.miaobi.app.usecase

import app.cash.turbine.test
import com.miaobi.app.domain.model.Story
import com.miaobi.app.domain.repository.StoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StoryUseCasesTest {

    private lateinit var storyRepository: StoryRepository

    private val testStory = Story(
        id = 1L,
        title = "测试故事",
        description = "这是一个测试故事",
        templateType = "free",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Before
    fun setup() {
        storyRepository = mockk(relaxed = true)
    }

    @Test
    fun `GetAllStoriesUseCase returns flow of stories`() = runTest {
        val stories = listOf(testStory)
        coEvery { storyRepository.getAllStories() } returns flowOf(stories)

        val useCase = GetAllStoriesUseCase(storyRepository)

        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("测试故事", result[0].title)
            awaitComplete()
        }
    }

    @Test
    fun `GetStoryByIdUseCase returns story when exists`() = runTest {
        coEvery { storyRepository.getStoryById(1L) } returns flowOf(testStory)

        val useCase = GetStoryByIdUseCase(storyRepository)

        useCase(1L).test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("测试故事", result?.title)
            awaitComplete()
        }
    }

    @Test
    fun `GetStoryByIdUseCase returns null when not exists`() = runTest {
        coEvery { storyRepository.getStoryById(999L) } returns flowOf(null)

        val useCase = GetStoryByIdUseCase(storyRepository)

        useCase(999L).test {
            val result = awaitItem()
            assertNull(result)
            awaitComplete()
        }
    }

    @Test
    fun `CreateStoryUseCase creates story with correct title`() = runTest {
        coEvery { storyRepository.insertStory(any()) } returns 1L

        val useCase = CreateStoryUseCase(storyRepository)

        val result = useCase("我的新书", "描述内容")

        assertEquals(1L, result)
        coVerify { storyRepository.insertStory(match { it.title == "我的新书" }) }
    }

    @Test
    fun `CreateStoryUseCase uses default title when blank`() = runTest {
        coEvery { storyRepository.insertStory(any()) } returns 1L

        val useCase = CreateStoryUseCase(storyRepository)

        useCase("", "描述内容")

        coVerify { storyRepository.insertStory(match { it.title == "无标题故事" }) }
    }

    @Test
    fun `UpdateStoryUseCase updates story with new timestamp`() = runTest {
        coEvery { storyRepository.updateStory(any()) } returns Unit

        val useCase = UpdateStoryUseCase(storyRepository)

        useCase(testStory)

        coVerify { storyRepository.updateStory(match { it.updatedAt > testStory.updatedAt }) }
    }

    @Test
    fun `DeleteStoryUseCase deletes story by id`() = runTest {
        coEvery { storyRepository.deleteStory(any()) } returns Unit

        val useCase = DeleteStoryUseCase(storyRepository)

        useCase(1L)

        coVerify { storyRepository.deleteStory(1L) }
    }
}
