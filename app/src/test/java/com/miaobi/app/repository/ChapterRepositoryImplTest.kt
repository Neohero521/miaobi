package com.miaobi.app.repository

import com.miaobi.app.data.local.dao.ChapterDao
import com.miaobi.app.data.local.entity.ChapterEntity
import com.miaobi.app.data.repository.ChapterRepositoryImpl
import com.miaobi.app.domain.model.Chapter
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

class ChapterRepositoryImplTest {

    private lateinit var chapterDao: ChapterDao
    private lateinit var repository: ChapterRepositoryImpl

    private val testEntity = ChapterEntity(
        id = 1L,
        storyId = 1L,
        title = "第一章",
        content = "内容",
        orderIndex = 0,
        wordCount = 1,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Before
    fun setup() {
        chapterDao = mockk(relaxed = true)
        repository = ChapterRepositoryImpl(chapterDao)
    }

    @Test
    fun `getChaptersByStoryId returns mapped chapters`() = runTest {
        coEvery { chapterDao.getChaptersByStoryId(1L) } returns flowOf(listOf(testEntity))

        val result = repository.getChaptersByStoryId(1L).first()

        assertEquals(1, result.size)
        assertEquals("第一章", result[0].title)
    }

    @Test
    fun `getChapterById returns chapter when exists`() = runTest {
        coEvery { chapterDao.getChapterByIdFlow(1L) } returns flowOf(testEntity)

        val result = repository.getChapterById(1L).first()

        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `insertChapter calculates order index correctly`() = runTest {
        coEvery { chapterDao.getMaxOrderIndex(1L) } returns 2
        coEvery { chapterDao.insertChapter(any()) } returns 3L
        val slot = slot<ChapterEntity>()

        val chapter = Chapter(
            storyId = 1L,
            title = "新章节",
            content = "",
            orderIndex = 0
        )
        repository.insertChapter(chapter)

        coVerify { chapterDao.insertChapter(capture(slot)) }
        assertEquals(3, slot.captured.orderIndex) // maxIndex + 1 = 2 + 1 = 3
    }

    @Test
    fun `updateChapter delegates to dao`() = runTest {
        val chapter = Chapter(
            id = 1L,
            storyId = 1L,
            title = "更新后",
            content = "新内容",
            orderIndex = 0
        )

        repository.updateChapter(chapter)

        coVerify { chapterDao.updateChapter(match { it.title == "更新后" }) }
    }

    @Test
    fun `deleteChapter delegates to dao`() = runTest {
        repository.deleteChapter(1L)

        coVerify { chapterDao.deleteChapterById(1L) }
    }

    @Test
    fun `updateChapterContent updates content and word count`() = runTest {
        repository.updateChapterContent(1L, "这是一段测试内容")

        coVerify { chapterDao.updateContent(1L, "这是一段测试内容") }
        coVerify { chapterDao.updateWordCount(1L, 5) } // 5 words
    }
}
