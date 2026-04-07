package com.miaobi.app.usecase

import app.cash.turbine.test
import com.miaobi.app.domain.model.AiMessage
import com.miaobi.app.domain.model.AiStreamResponse
import com.miaobi.app.domain.model.BranchOption
import com.miaobi.app.domain.model.LengthOption
import com.miaobi.app.domain.repository.AiRepository
import com.miaobi.app.util.PromptBuilder
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GenerateBranchesUseCaseTest {

    private lateinit var aiRepository: AiRepository

    @Before
    fun setup() {
        aiRepository = mockk(relaxed = true)
        mockkObject(PromptBuilder)
    }

    @After
    fun tearDown() {
        unmockkObject(PromptBuilder)
    }

    @Test
    fun `GenerateBranchesUseCase emits initial empty branches then final results`() = runTest {
        // Given: mock AI responses for 2 branches
        val responses1 = listOf(
            AiStreamResponse(content = "分支1内容", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )
        val responses2 = listOf(
            AiStreamResponse(content = "分支2内容", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )
        val summaryResponses = listOf(
            AiStreamResponse(content = "💥 冲突爆发", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )
        val summaryResponses2 = listOf(
            AiStreamResponse(content = "💖 情感互动", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )

        coEvery {
            aiRepository.continueStoryWithSystem(any(), any())
        } returns flowOf(*responses1.toTypedArray()) andThen
                flowOf(*responses2.toTypedArray()) andThen
                flowOf(*summaryResponses.toTypedArray()) andThen
                flowOf(*summaryResponses2.toTypedArray())

        coEvery {
            PromptBuilder.buildBranchContinuationPrompt(
                any(), any(), any(), any(), any(), any()
            )
        } returns listOf(AiMessage(role = "system", content = "sys"), AiMessage(role = "user", content = "usr"))

        coEvery {
            PromptBuilder.buildBranchSummaryPrompt(any())
        } returns listOf(AiMessage(role = "system", content = "sys"), AiMessage(role = "user", content = "usr"))

        val useCase = GenerateBranchesUseCase(aiRepository)

        // When
        useCase(
            currentContent = "当前正文",
            characters = "角色",
            worldSettings = "世界观",
            userInstruction = "续写",
            branchCount = 2,
            lengthOption = LengthOption.MEDIUM
        ).test {
            // Then: first emission is initial empty branches
            val initialBranches = awaitItem()
            assertEquals(2, initialBranches.size)
            assertTrue(initialBranches[0].isGenerating)
            assertTrue(initialBranches[1].isGenerating)

            // Final emission has content
            val finalBranches = awaitItem()
            assertEquals(2, finalBranches.size)
            assertFalse(finalBranches[0].isGenerating)
            assertFalse(finalBranches[1].isGenerating)

            awaitComplete()
        }
    }

    @Test
    fun `GenerateBranchesUseCase handles errors in branch generation`() = runTest {
        val errorResponse = AiStreamResponse(content = "", isFinished = true, error = "API Error")

        coEvery {
            aiRepository.continueStoryWithSystem(any(), any())
        } returns flowOf(errorResponse)

        coEvery {
            PromptBuilder.buildBranchContinuationPrompt(
                any(), any(), any(), any(), any(), any()
            )
        } returns listOf(AiMessage(role = "system", content = "sys"), AiMessage(role = "user", content = "usr"))

        coEvery {
            PromptBuilder.buildBranchSummaryPrompt(any())
        } returns listOf(AiMessage(role = "system", content = "sys"), AiMessage(role = "user", content = "usr"))

        val useCase = GenerateBranchesUseCase(aiRepository)

        useCase(
            currentContent = "正文",
            characters = "",
            worldSettings = "",
            userInstruction = null,
            branchCount = 2,
            lengthOption = LengthOption.MEDIUM
        ).test {
            val initialBranches = awaitItem()
            assertEquals(2, initialBranches.size)

            val finalBranches = awaitItem()
            assertTrue(finalBranches.any { it.error != null })

            awaitComplete()
        }
    }

    @Test
    fun `GenerateBranchesUseCase handles branch count 3`() = runTest {
        // Given: 3 branches
        coEvery {
            aiRepository.continueStoryWithSystem(any(), any())
        } returns flowOf(
            AiStreamResponse(content = "内容", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )

        coEvery {
            PromptBuilder.buildBranchContinuationPrompt(
                any(), any(), any(), any(), any(), any()
            )
        } returns listOf(AiMessage(role = "system", content = "sys"), AiMessage(role = "user", content = "usr"))

        coEvery {
            PromptBuilder.buildBranchSummaryPrompt(any())
        } returns listOf(AiMessage(role = "system", content = "sys"), AiMessage(role = "user", content = "usr"))

        val useCase = GenerateBranchesUseCase(aiRepository)

        useCase(
            currentContent = "正文",
            characters = "",
            worldSettings = "",
            userInstruction = null,
            branchCount = 3,
            lengthOption = LengthOption.SHORT
        ).test {
            val initialBranches = awaitItem()
            assertEquals(3, initialBranches.size)

            val finalBranches = awaitItem()
            assertEquals(3, finalBranches.size)

            awaitComplete()
        }
    }

    @Test
    fun `GenerateBranchesUseCase handles branch count 4`() = runTest {
        coEvery {
            aiRepository.continueStoryWithSystem(any(), any())
        } returns flowOf(
            AiStreamResponse(content = "内容", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )

        coEvery {
            PromptBuilder.buildBranchContinuationPrompt(
                any(), any(), any(), any(), any(), any()
            )
        } returns listOf(AiMessage(role = "system", content = "sys"), AiMessage(role = "user", content = "usr"))

        coEvery {
            PromptBuilder.buildBranchSummaryPrompt(any())
        } returns listOf(AiMessage(role = "system", content = "sys"), AiMessage(role = "user", content = "usr"))

        val useCase = GenerateBranchesUseCase(aiRepository)

        useCase(
            currentContent = "正文",
            characters = "",
            worldSettings = "",
            userInstruction = null,
            branchCount = 4,
            lengthOption = LengthOption.LONG
        ).test {
            val initialBranches = awaitItem()
            assertEquals(4, initialBranches.size)

            val finalBranches = awaitItem()
            assertEquals(4, finalBranches.size)

            awaitComplete()
        }
    }

    @Test
    fun `BranchOption canAccept is true when content is non-blank and not generating`() {
        val branch = BranchOption(
            index = 0,
            content = "some content",
            isGenerating = false,
            error = null
        )
        assertTrue(branch.canAccept)
    }

    @Test
    fun `BranchOption canAccept is false when still generating`() {
        val branch = BranchOption(
            index = 0,
            content = "",
            isGenerating = true,
            error = null
        )
        assertFalse(branch.canAccept)
    }

    @Test
    fun `BranchOption canAccept is false when error present`() {
        val branch = BranchOption(
            index = 0,
            content = "some content",
            isGenerating = false,
            error = "some error"
        )
        assertFalse(branch.canAccept)
    }

    @Test
    fun `BranchOption canAccept is false when content is blank`() {
        val branch = BranchOption(
            index = 0,
            content = "",
            isGenerating = false,
            error = null
        )
        assertFalse(branch.canAccept)
    }
}
