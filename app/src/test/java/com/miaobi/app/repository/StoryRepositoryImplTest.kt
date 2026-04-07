package com.miaobi.app.repository

import com.miaobi.app.data.local.dao.StoryDao
import com.miaobi.app.data.local.entity.StoryEntity
import com.miaobi.app.data.repository.StoryRepositoryImpl
import com.miaobi.app.domain.model.Story
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StoryRepositoryImplTest {

    private lateinit var storyDao: StoryDao
    private lateinit var repository: StoryRepositoryImpl

    private val testEntity = StoryEntity(
        id = 1L,
        title = "测试故事",
        description = "描述",
        templateType = "free",
        coverImage = null,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Before
    fun setup() {
        storyDao = mockk(relaxed = true)
        repository = StoryRepositoryImpl(storyDao)
    }

    @Test
    fun `getAllStories returns mapped domain objects`() = runTest {
        coEvery { storyDao.getAllStories() } returns flowOf(listOf(testEntity))

        val result = repository.getAllStories().first()

        assertEquals(1, result.size)
        assertEquals("测试故事", result[0].title)
    }

    @Test
    fun `getStoryById returns mapped story`() = runTest {
        coEvery { storyDao.getStoryByIdFlow(1L) } returns flowOf(testEntity)

        val result = repository.getStoryById(1L).first()

        assertNotNull(result)
        assertEquals("测试故事", result?.title)
    }

    @Test
    fun `insertStory inserts entity and returns id`() = runTest {
        coEvery { storyDao.insertStory(any()) } returns 1L
        val slot = slot<StoryEntity>()

        val story = Story(title = "新书", description = "新描述")
        val result = repository.insertStory(story)

        assertEquals(1L, result)
        coVerify { storyDao.insertStory(capture(slot)) }
        assertEquals("新书", slot.captured.title)
    }

    @Test
    fun `updateStory delegates to dao`() = runTest {
        val story = Story(
            id = 1L,
            title = "更新后",
            description = "更新描述",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        repository.updateStory(story)

        coVerify { storyDao.updateStory(match { it.title == "更新后" }) }
    }

    @Test
    fun `deleteStory calls dao with correct id`() = runTest {
        repository.deleteStory(1L)

        coVerify { storyDao.deleteStoryById(1L) }
    }

    @Test
    fun `getStoryCount returns count from dao`() = runTest {
        coEvery { storyDao.getStoryCount() } returns 5

        val result = repository.getStoryCount()

        assertEquals(5, result)
    }
}
