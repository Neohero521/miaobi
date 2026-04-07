package com.miaobi.app.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.miaobi.app.domain.model.Story
import com.miaobi.app.domain.model.StoryTemplate
import com.miaobi.app.domain.repository.CharacterRepository
import com.miaobi.app.domain.repository.StoryRepository
import com.miaobi.app.domain.repository.StoryTemplateRepository
import com.miaobi.app.domain.repository.WorldSettingRepository
import com.miaobi.app.ui.screens.bookshelf.BookshelfEvent
import com.miaobi.app.ui.screens.bookshelf.BookshelfViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookshelfViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var storyRepository: StoryRepository
    private lateinit var characterRepository: CharacterRepository
    private lateinit var worldSettingRepository: WorldSettingRepository
    private lateinit var storyTemplateRepository: StoryTemplateRepository
    private lateinit var viewModel: BookshelfViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testStories = listOf(
        Story(id = 1L, title = "故事一", description = "描述一"),
        Story(id = 2L, title = "故事二", description = "描述二")
    )

    private val testTemplates = listOf(
        StoryTemplate(id = 1L, title = "修仙模板", genre = "xianxia", summary = "修仙故事", promptTemplate = "续写"),
        StoryTemplate(id = 2L, title = "都市模板", genre = "urban", summary = "都市故事", promptTemplate = "续写")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        storyRepository = mockk(relaxed = true)
        characterRepository = mockk(relaxed = true)
        worldSettingRepository = mockk(relaxed = true)
        storyTemplateRepository = mockk(relaxed = true)

        coEvery { storyRepository.getAllStories() } returns flowOf(testStories)
        coEvery { storyTemplateRepository.getAllTemplates() } returns flowOf(testTemplates)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): BookshelfViewModel {
        return BookshelfViewModel(
            storyRepository = storyRepository,
            characterRepository = characterRepository,
            worldSettingRepository = worldSettingRepository,
            storyTemplateRepository = storyTemplateRepository
        )
    }

    @Test
    fun `initial state loads stories`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.stories.size)
            assertEquals("故事一", state.stories[0].title)
        }
    }

    @Test
    fun `initial state loads templates`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.templates.size)
            assertEquals("修仙模板", state.templates[0].title)
        }
    }

    @Test
    fun `ToggleCreateDialog toggles dialog and clears input`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BookshelfEvent.ToggleCreateDialog)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showCreateDialog)
            assertEquals("", state.newStoryTitle)
            assertEquals("", state.newStoryDescription)
        }

        viewModel.onEvent(BookshelfEvent.ToggleCreateDialog)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showCreateDialog)
        }
    }

    @Test
    fun `UpdateNewStoryTitle updates title`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BookshelfEvent.UpdateNewStoryTitle("新标题"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("新标题", state.newStoryTitle)
        }
    }

    @Test
    fun `UpdateNewStoryDescription updates description`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BookshelfEvent.UpdateNewStoryDescription("新描述"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("新描述", state.newStoryDescription)
        }
    }

    @Test
    fun `CreateStory inserts story and closes dialog`() = runTest {
        coEvery { storyRepository.insertStory(any()) } returns 1L

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BookshelfEvent.UpdateNewStoryTitle("新故事"))
        viewModel.onEvent(BookshelfEvent.UpdateNewStoryDescription("新描述"))
        viewModel.onEvent(BookshelfEvent.CreateStory("新故事", "新描述"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { storyRepository.insertStory(match { it.title == "新故事" }) }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showCreateDialog)
        }
    }

    @Test
    fun `DeleteStory deletes story by id`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BookshelfEvent.DeleteStory(1L))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { storyRepository.deleteStory(1L) }
    }

    // ===== Template tests =====

    @Test
    fun `ShowTemplateDialog shows template dialog`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BookshelfEvent.ShowTemplateDialog)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showTemplateDialog)
        }
    }

    @Test
    fun `HideTemplateDialog hides template dialog`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BookshelfEvent.ShowTemplateDialog)
        viewModel.onEvent(BookshelfEvent.HideTemplateDialog)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showTemplateDialog)
        }
    }

    @Test
    fun `SelectTemplate shows detail dialog`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BookshelfEvent.SelectTemplate(testTemplates[0]))

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showTemplateDialog)
            assertTrue(state.showTemplateDetailDialog)
            assertNotNull(state.selectedTemplate)
            assertEquals("修仙模板", state.selectedTemplate?.title)
        }
    }

    @Test
    fun `HideTemplateDetailDialog clears state`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BookshelfEvent.SelectTemplate(testTemplates[0]))
        viewModel.onEvent(BookshelfEvent.HideTemplateDetailDialog)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showTemplateDetailDialog)
            assertNull(state.selectedTemplate)
        }
    }

    @Test
    fun `ConfirmCreateFromTemplate creates story with template`() = runTest {
        coEvery { storyRepository.insertStory(any()) } returns 3L
        coEvery { characterRepository.insertCharacter(any()) } returns 1L
        coEvery { worldSettingRepository.insertWorldSetting(any()) } returns 1L

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BookshelfEvent.SelectTemplate(testTemplates[0]))
        viewModel.onEvent(BookshelfEvent.UpdateNewStoryTitle("我的修仙小说"))
        viewModel.onEvent(BookshelfEvent.ConfirmCreateFromTemplate)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { storyRepository.insertStory(match { it.title == "我的修仙小说" && it.templateType == "xianxia" }) }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showTemplateDialog)
            assertFalse(state.showTemplateDetailDialog)
            assertFalse(state.showCreateDialog)
            assertNull(state.selectedTemplate)
        }
    }
}
