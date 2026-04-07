package com.miaobi.app.usecase

import app.cash.turbine.test
import com.miaobi.app.domain.model.Chapter
import com.miaobi.app.domain.repository.ChapterRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChapterUseCasesTest {

    private lateinit var chapterRepository: ChapterRepository

    private val testChapter = Chapter(
        id = 1L,
        storyId = 1L,
        title = "第一章",
        content = "故事开始了...",
        orderIndex = 0,
        wordCount = 4,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Before
    fun setup() {
        chapterRepository = mockk(relaxed = true)
    }

    @Test
    fun `GetChaptersByStoryIdUseCase returns chapters for story`() = runTest {
        coEvery { chapterRepository.getChaptersByStoryId(1L) } returns flowOf(listOf(testChapter))

        val useCase = GetChaptersByStoryIdUseCase(chapterRepository)

        useCase(1L).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("第一章", result[0].title)
            awaitComplete()
        }
    }

    @Test
    fun `GetChapterByIdUseCase returns chapter when exists`() = runTest {
        coEvery { chapterRepository.getChapterById(1L) } returns flowOf(testChapter)

        val useCase = GetChapterByIdUseCase(chapterRepository)

        useCase(1L).test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("第一章", result?.title)
            awaitComplete()
        }
    }

    @Test
    fun `CreateChapterUseCase creates chapter with default title`() = runTest {
        coEvery { chapterRepository.insertChapter(any()) } returns 2L

        val useCase = CreateChapterUseCase(chapterRepository)

        val result = useCase(storyId = 1L, title = "", content = "内容")

        assertEquals(2L, result)
        coVerify { chapterRepository.insertChapter(match { it.title == "新章节" }) }
    }

    @Test
    fun `CreateChapterUseCase uses provided title`() = runTest {
        coEvery { chapterRepository.insertChapter(any()) } returns 2L

        val useCase = CreateChapterUseCase(chapterRepository)

        useCase(storyId = 1L, title = "我的章节", content = "内容")

        coVerify { chapterRepository.insertChapter(match { it.title == "我的章节" }) }
    }

    @Test
    fun `UpdateChapterContentUseCase updates chapter content`() = runTest {
        coEvery { chapterRepository.updateChapterContent(any(), any()) } returns Unit

        val useCase = UpdateChapterContentUseCase(chapterRepository)

        useCase(1L, "新内容")

        coVerify { chapterRepository.updateChapterContent(1L, "新内容") }
    }

    @Test
    fun `DeleteChapterUseCase deletes chapter by id`() = runTest {
        coEvery { chapterRepository.deleteChapter(any()) } returns Unit

        val useCase = DeleteChapterUseCase(chapterRepository)

        useCase(1L)

        coVerify { chapterRepository.deleteChapter(1L) }
    }
}
