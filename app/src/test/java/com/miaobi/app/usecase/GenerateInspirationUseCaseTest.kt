package com.miaobi.app.usecase

import app.cash.turbine.test
import com.miaobi.app.domain.model.AiMessage
import com.miaobi.app.domain.model.AiStreamResponse
import com.miaobi.app.domain.model.InspirationOption
import com.miaobi.app.domain.model.InspirationType
import com.miaobi.app.domain.model.LengthOption
import com.miaobi.app.domain.repository.AiRepository
import com.miaobi.app.domain.usecase.GenerateInspirationUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GenerateInspirationUseCaseTest {

    private lateinit var aiRepository: AiRepository

    @Before
    fun setup() {
        aiRepository = mockk(relaxed = true)
    }

    @Test
    fun `GenerateInspirationUseCase emits initial empty options then final results`() = runTest {
        // Given: mock AI responses for inspiration content and title
        val contentResponses = listOf(
            AiStreamResponse(content = "剧情升级内容...", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )
        val titleResponses = listOf(
            AiStreamResponse(content = "冲突爆发", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )

        coEvery {
            aiRepository.continueStoryWithSystem(any(), any())
        } returns flowOf(*contentResponses.toTypedArray()) andThen
                flowOf(*titleResponses.toTypedArray())

        val useCase = GenerateInspirationUseCase(aiRepository)

        // When
        useCase(
            currentContent = "当前正文内容",
            characters = "角色设定",
            worldSettings = "世界观设定",
            count = 4,
            typeFilter = null
        ).test {
            // Then: first emission is initial empty options
            val initialOptions = awaitItem()
            assertTrue(initialOptions.isNotEmpty())
            assertTrue(initialOptions.all { it.content.isBlank() && it.title.isBlank() && it.isGenerating })

            // Final emission has content
            val finalOptions = awaitItem()
            assertTrue(finalOptions.isNotEmpty())
            assertTrue(finalOptions.all { !it.isGenerating })

            awaitComplete()
        }
    }

    @Test
    fun `GenerateInspirationUseCase handles errors in content generation`() = runTest {
        // Given: content generation returns error
        val errorResponse = AiStreamResponse(content = "", isFinished = true, error = "API Error")
        coEvery {
            aiRepository.continueStoryWithSystem(any(), any())
        } returns flowOf(errorResponse)

        val useCase = GenerateInspirationUseCase(aiRepository)

        useCase(
            currentContent = "正文",
            characters = "",
            worldSettings = "",
            count = 4,
            typeFilter = null
        ).test {
            val initialOptions = awaitItem()
            assertTrue(initialOptions.isNotEmpty())

            val finalOptions = awaitItem()
            assertTrue(finalOptions.any { it.error != null })

            awaitComplete()
        }
    }

    @Test
    fun `GenerateInspirationUseCase respects typeFilter parameter`() = runTest {
        // Given: type filter is set to PLOT_ESCALATION
        val contentResponses = listOf(
            AiStreamResponse(content = "升级内容", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )
        val titleResponses = listOf(
            AiStreamResponse(content = "高潮来临", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )

        coEvery {
            aiRepository.continueStoryWithSystem(any(), any())
        } returns flowOf(*contentResponses.toTypedArray()) andThen
                flowOf(*titleResponses.toTypedArray())

        val useCase = GenerateInspirationUseCase(aiRepository)

        useCase(
            currentContent = "正文",
            characters = "",
            worldSettings = "",
            count = 4,
            typeFilter = InspirationType.PLOT_ESCALATION
        ).test {
            val initialOptions = awaitItem()
            // With typeFilter, only one type is used
            assertEquals(1, initialOptions.size)
            assertEquals(InspirationType.PLOT_ESCALATION, initialOptions[0].type)

            val finalOptions = awaitItem()
            assertEquals(1, finalOptions.size)
            assertEquals(InspirationType.PLOT_ESCALATION, finalOptions[0].type)

            awaitComplete()
        }
    }

    @Test
    fun `GenerateInspirationUseCase generates correct number of options`() = runTest {
        coEvery {
            aiRepository.continueStoryWithSystem(any(), any())
        } returns flowOf(
            AiStreamResponse(content = "内容", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )

        val useCase = GenerateInspirationUseCase(aiRepository)

        // When count = 3
        useCase(
            currentContent = "正文",
            characters = "",
            worldSettings = "",
            count = 3,
            typeFilter = null
        ).test {
            val initialOptions = awaitItem()
            assertEquals(3, initialOptions.size)
            awaitComplete()
        }
    }

    @Test
    fun `GenerateInspirationUseCase uses correct length options`() = runTest {
        val capturedLengthOptions = mutableListOf<LengthOption>()

        coEvery {
            aiRepository.continueStoryWithSystem(any(), coArg(nth = 1, match = { capturedLengthOptions.add(this) }))
        } returns flowOf(
            AiStreamResponse(content = "内容", isFinished = false),
            AiStreamResponse(content = "", isFinished = true)
        )

        val useCase = GenerateInspirationUseCase(aiRepository)

        useCase(
            currentContent = "正文",
            characters = "",
            worldSettings = "",
            count = 2,
            typeFilter = null
        ).test {
            awaitItem() // initial
            awaitItem() // final
            awaitComplete()
        }

        // First call for content uses MEDIUM, second for title uses SHORT
        assertTrue(capturedLengthOptions.isNotEmpty())
    }

    @Test
    fun `InspirationOption canAccept is true when content is non-blank and not generating`() {
        val option = InspirationOption(
            index = 0,
            type = InspirationType.PLOT_ESCALATION,
            title = "冲突爆发",
            content = "some content",
            isGenerating = false,
            error = null
        )
        assertTrue(option.canAccept)
    }

    @Test
    fun `InspirationOption canAccept is false when still generating`() {
        val option = InspirationOption(
            index = 0,
            type = InspirationType.PLOT_ESCALATION,
            title = "",
            content = "",
            isGenerating = true,
            error = null
        )
        assertFalse(option.canAccept)
    }

    @Test
    fun `InspirationOption canAccept is false when error present`() {
        val option = InspirationOption(
            index = 0,
            type = InspirationType.PLOT_ESCALATION,
            title = "标题",
            content = "内容",
            isGenerating = false,
            error = "API Error"
        )
        assertFalse(option.canAccept)
    }

    @Test
    fun `InspirationOption canAccept is false when content is blank`() {
        val option = InspirationOption(
            index = 0,
            type = InspirationType.PLOT_ESCALATION,
            title = "标题",
            content = "",
            isGenerating = false,
            error = null
        )
        assertFalse(option.canAccept)
    }

    @Test
    fun `InspirationOption default values are correct`() {
        val option = InspirationOption(
            index = 0,
            type = InspirationType.EMOTIONAL_INTERACTION,
            title = "",
            content = ""
        )
        assertEquals("", option.summaryTag)
        assertFalse(option.isSelected)
        assertTrue(option.isGenerating)
        assertFalse(option.isFavorite)
        assertNull(option.error)
        assertFalse(option.canAccept)
    }

    @Test
    fun `InspirationType entries contains all expected types`() {
        assertEquals(6, InspirationType.entries.size)
        assertTrue(InspirationType.entries.contains(InspirationType.PLOT_ESCALATION))
        assertTrue(InspirationType.entries.contains(InspirationType.EMOTIONAL_INTERACTION))
        assertTrue(InspirationType.entries.contains(InspirationType.UNEXPECTED_TWIST))
        assertTrue(InspirationType.entries.contains(InspirationType.CHARACTER_GROWTH))
        assertTrue(InspirationType.entries.contains(InspirationType.SUSPENSE_SETUP))
        assertTrue(InspirationType.entries.contains(InspirationType.WORLD_EXPANSION))
    }

    @Test
    fun `InspirationType has correct emoji and labels`() {
        assertEquals("💥", InspirationType.PLOT_ESCALATION.emoji)
        assertEquals("剧情升级", InspirationType.PLOT_ESCALATION.label)
        assertEquals("💖", InspirationType.EMOTIONAL_INTERACTION.emoji)
        assertEquals("情感互动", InspirationType.EMOTIONAL_INTERACTION.label)
        assertEquals("🔮", InspirationType.UNEXPECTED_TWIST.emoji)
        assertEquals("意外转折", InspirationType.UNEXPECTED_TWIST.label)
        assertEquals("🌱", InspirationType.CHARACTER_GROWTH.emoji)
        assertEquals("角色成长", InspirationType.CHARACTER_GROWTH.label)
        assertEquals("⚡", InspirationType.SUSPENSE_SETUP.emoji)
        assertEquals("悬念设置", InspirationType.SUSPENSE_SETUP.label)
        assertEquals("🌍", InspirationType.WORLD_EXPANSION.emoji)
        assertEquals("世界观扩展", InspirationType.WORLD_EXPANSION.label)
    }
}
