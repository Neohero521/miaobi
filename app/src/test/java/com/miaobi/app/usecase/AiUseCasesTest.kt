package com.miaobi.app.usecase

import app.cash.turbine.test
import com.miaobi.app.domain.model.AiStreamResponse
import com.miaobi.app.domain.repository.AiRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AiUseCasesTest {

    private lateinit var aiRepository: AiRepository

    @Before
    fun setup() {
        aiRepository = mockk(relaxed = true)
    }

    @Test
    fun `ContinueStoryUseCase calls repository with correct params`() = runTest {
        val responses = listOf(
            AiStreamResponse(content = "继续", isFinished = false),
            AiStreamResponse(content = "故事", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )
        coEvery {
            aiRepository.continueStory(any(), any(), any(), any())
        } returns flowOf(*responses.toTypedArray())

        val useCase = ContinueStoryUseCase(aiRepository)

        useCase("续写", "角色", "设定", "历史").test {
            val items = awaitAll()
            assertEquals(3, items.size)
            assertEquals("继续", items[0].content)
            assertTrue(items[2].isFinished)
            awaitComplete()
        }
    }

    @Test
    fun `ContinueStoryUseCase handles error response`() = runTest {
        val errorResponse = AiStreamResponse(content = "", isFinished = true, error = "API错误")
        coEvery {
            aiRepository.continueStory(any(), any(), any(), any())
        } returns flowOf(errorResponse)

        val useCase = ContinueStoryUseCase(aiRepository)

        useCase("续写", "角色", "设定", "历史").test {
            val result = awaitItem()
            assertEquals("API错误", result.error)
            awaitComplete()
        }
    }

    @Test
    fun `GenerateTitleUseCase returns generated title`() = runTest {
        val response = AiStreamResponse(content = "新书名", isFinished = true)
        coEvery {
            aiRepository.generateTitle(any(), any(), any())
        } returns flowOf(response)

        val useCase = GenerateTitleUseCase(aiRepository)

        useCase("内容", "角色", "设定").test {
            val result = awaitItem()
            assertEquals("新书名", result.content)
            awaitComplete()
        }
    }

    @Test
    fun `SaveChapterDraftUseCase saves draft and returns id`() = runTest {
        coEvery { aiRepository.saveDraft(any(), any()) } returns 5L

        val useCase = SaveChapterDraftUseCase(aiRepository)

        val result = useCase(chapterId = 1L, content = "草稿内容")

        assertEquals(5L, result)
        coEvery { aiRepository.saveDraft(1L, "草稿内容") }
    }
}
