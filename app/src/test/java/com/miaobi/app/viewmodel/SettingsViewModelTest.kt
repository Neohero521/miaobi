package com.miaobi.app.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.miaobi.app.domain.model.AiConfig
import com.miaobi.app.ui.screens.settings.SettingsEvent
import com.miaobi.app.ui.screens.settings.SettingsViewModel
import com.miaobi.app.util.SettingsManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var settingsManager: SettingsManager
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testConfig = AiConfig(
        apiKey = "test-key",
        apiUrl = "https://api.test.com",
        modelName = "test-model"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsManager = mockk(relaxed = true)
        coEvery { settingsManager.aiConfig } returns flowOf(testConfig)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load reads config from manager`() = runTest {
        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("test-key", state.apiKey)
            assertEquals("https://api.test.com", state.apiUrl)
            assertEquals("test-model", state.modelName)
        }
    }

    @Test
    fun `UpdateApiKey updates api key`() = runTest {
        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.UpdateApiKey("new-key"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("new-key", state.apiKey)
        }
    }

    @Test
    fun `UpdateApiUrl updates api url`() = runTest {
        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.UpdateApiUrl("https://new.url"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("https://new.url", state.apiUrl)
        }
    }

    @Test
    fun `UpdateModelName updates model name`() = runTest {
        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.UpdateModelName("new-model"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("new-model", state.modelName)
        }
    }

    @Test
    fun `SaveSettings persists config`() = runTest {
        coEvery { settingsManager.updateAiConfig(any()) } returns Unit

        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.UpdateApiKey("updated-key"))
        viewModel.onEvent(SettingsEvent.SaveSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsManager.updateAiConfig(match { it.apiKey == "updated-key" }) }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSaving)
            assertTrue(state.saveSuccess)
        }
    }

    @Test
    fun `SaveSettings handles error`() = runTest {
        coEvery { settingsManager.updateAiConfig(any()) } throws Exception("保存失败")

        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.SaveSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSaving)
            assertTrue(state.error?.contains("保存失败") == true)
        }
    }

    @Test
    fun `ClearError clears error state`() = runTest {
        coEvery { settingsManager.updateAiConfig(any()) } throws Exception("错误")

        viewModel = SettingsViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.SaveSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.ClearError)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }
}


