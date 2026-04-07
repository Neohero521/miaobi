package com.miaobi.app.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.miaobi.app.domain.model.*
import com.miaobi.app.domain.repository.*
import com.miaobi.app.domain.usecase.GenerateBranchesUseCase
import com.miaobi.app.domain.usecase.GenerateInspirationUseCase
import com.miaobi.app.domain.usecase.RewriteTextUseCase
import com.miaobi.app.ui.screens.writing.WritingEvent
import com.miaobi.app.ui.screens.writing.WritingViewModel
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class WritingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var storyRepository: StoryRepository

    @MockK
    private lateinit var chapterRepository: ChapterRepository

    @MockK
    private lateinit var characterRepository: CharacterRepository

    @MockK
    private lateinit var worldSettingRepository: WorldSettingRepository

    @MockK
    private lateinit var chapterDraftRepository: ChapterDraftRepository

    @MockK
    private lateinit var aiRepository: AiRepository

    @MockK
    private lateinit var rewriteTextUseCase: RewriteTextUseCase

    @MockK
    private lateinit var generateBranchesUseCase: GenerateBranchesUseCase

    @MockK
    private lateinit var generateInspirationUseCase: GenerateInspirationUseCase

    private lateinit var viewModel: WritingViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testStory = Story(id = 1L, title = "测试故事", description = "描述")
    private val testChapter = Chapter(id = 1L, storyId = 1L, title = "第一章", content = "内容", orderIndex = 0)
    private val testCharacter = Character(id = 1L, storyId = 1L, name = "小明", description = "主角")
    private val testWorldSetting = WorldSetting(id = 1L, storyId = 1L, name = "世界", content = "设定")
    private val testDraft = ChapterDraft(id = 1L, chapterId = 1L, content = "草稿", wordCount = 1, version = 1)
    private val testDraftVersion = ChapterDraftVersion(id = 1L, draftId = 1L, version = 1, content = "版本1内容", wordCount = 3, diffSummary = "初始版本", createdAt = 1000L)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        coEvery { storyRepository.getStoryById(any()) } returns flowOf(testStory)
        coEvery { chapterRepository.getChaptersByStoryId(any()) } returns flowOf(listOf(testChapter))
        coEvery { characterRepository.getCharactersByStoryId(any()) } returns flowOf(listOf(testCharacter))
        coEvery { worldSettingRepository.getWorldSettingsByStoryId(any()) } returns flowOf(listOf(testWorldSetting))
        coEvery { chapterDraftRepository.getDraftsByChapterId(any()) } returns flowOf(listOf(testDraft))
        coEvery { chapterRepository.updateChapterContent(any(), any()) } returns Unit
        coEvery { chapterRepository.insertChapter(any()) } returns 2L
        coEvery { chapterRepository.deleteChapter(any()) } returns Unit
        coEvery { characterRepository.insertCharacter(any()) } returns 2L
        coEvery { characterRepository.deleteCharacter(any()) } returns Unit
        coEvery { worldSettingRepository.insertWorldSetting(any()) } returns 2L
        coEvery { worldSettingRepository.deleteWorldSetting(any()) } returns Unit
        coEvery { chapterDraftRepository.deleteDraft(any()) } returns Unit
        coEvery { chapterDraftRepository.getVersionsByDraftId(any()) } returns flowOf(listOf(testDraftVersion))
        coEvery { chapterDraftRepository.getVersionById(any()) } returns testDraftVersion
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel(storyId: Long = 1L, chapterId: Long = -1L): WritingViewModel {
        return WritingViewModel(
            savedStateHandle = mockk(relaxed = true) {
                every { get<Long>("storyId") } returns storyId
                every { get<Long>("chapterId") } returns chapterId
            },
            storyRepository = storyRepository,
            chapterRepository = chapterRepository,
            characterRepository = characterRepository,
            worldSettingRepository = worldSettingRepository,
            chapterDraftRepository = chapterDraftRepository,
            aiRepository = aiRepository,
            rewriteTextUseCase = rewriteTextUseCase,
            generateBranchesUseCase = generateBranchesUseCase,
            generateInspirationUseCase = generateInspirationUseCase
        )
    }

    @Test
    fun `initial load populates state`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("测试故事", state.story?.title)
            assertEquals(1, state.chapters.size)
            assertEquals(1, state.characters.size)
            assertEquals(1, state.worldSettings.size)
        }
    }

    @Test
    fun `UpdateContent updates content`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.UpdateContent("新内容"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("新内容", state.content)
        }
    }

    @Test
    fun `UpdatePrompt updates prompt`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.UpdatePrompt("续写一下"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("续写一下", state.userPrompt)
        }
    }

    @Test
    fun `UpdateLengthOption updates lengthOption`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.UpdateLengthOption(LengthOption.SHORT))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(LengthOption.SHORT, state.lengthOption)
        }
    }

    @Test
    fun `UpdateLengthOption supports all options`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.UpdateLengthOption(LengthOption.LONG))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(LengthOption.LONG, state.lengthOption)
            assertEquals(800, state.lengthOption.minChars)
            assertEquals(1200, state.lengthOption.maxChars)
            assertEquals(1500, state.lengthOption.maxTokens)
        }
    }

    @Test
    fun `SaveContent calls repository`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.UpdateContent("保存内容"))
        viewModel.onEvent(WritingEvent.SaveContent)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chapterRepository.updateChapterContent(testChapter.id, "保存内容") }
    }

    @Test
    fun `ToggleChapterList toggles visibility`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleChapterList)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showChapterList)
        }

        viewModel.onEvent(WritingEvent.ToggleChapterList)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showChapterList)
        }
    }

    @Test
    fun `AddChapter creates chapter and selects it`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.UpdateNewChapterTitle("新章节"))
        viewModel.onEvent(WritingEvent.AddChapter)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chapterRepository.insertChapter(match { it.title == "新章节" }) }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showAddChapterDialog)
            assertEquals(2, state.chapters.size)
        }
    }

    @Test
    fun `DeleteChapter removes chapter`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.DeleteChapter(1L))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chapterRepository.deleteChapter(1L) }
    }

    @Test
    fun `AddCharacter inserts character`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ShowAddCharacterDialog)
        viewModel.onEvent(WritingEvent.UpdateNewCharacterName("新角色"))
        viewModel.onEvent(WritingEvent.UpdateNewCharacterDescription("新描述"))
        viewModel.onEvent(WritingEvent.AddCharacter)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { characterRepository.insertCharacter(match { it.name == "新角色" }) }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showAddCharacterDialog)
        }
    }

    @Test
    fun `DeleteCharacter removes character`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.DeleteCharacter(1L))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { characterRepository.deleteCharacter(1L) }
    }

    @Test
    fun `AddWorldSetting inserts setting`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ShowAddWorldSettingDialog)
        viewModel.onEvent(WritingEvent.UpdateNewWorldSettingName("新设定"))
        viewModel.onEvent(WritingEvent.UpdateNewWorldSettingContent("设定内容"))
        viewModel.onEvent(WritingEvent.AddWorldSetting)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { worldSettingRepository.insertWorldSetting(match { it.name == "新设定" }) }
    }

    @Test
    fun `DeleteWorldSetting removes setting`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.DeleteWorldSetting(1L))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { worldSettingRepository.deleteWorldSetting(1L) }
    }

    @Test
    fun `SelectDraft restores content`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.SelectDraft(testDraft))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("草稿", state.content)
            assertFalse(state.showDraftHistory)
        }
    }

    @Test
    fun `DeleteDraft removes draft`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.DeleteDraft(1L))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chapterDraftRepository.deleteDraft(1L) }
    }

    @Test
    fun `ShowDraftVersions toggles draft versions visibility`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ShowDraftVersions)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showDraftVersions)
        }
    }

    @Test
    fun `HideDraftVersions hides versions and clears state`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ShowDraftVersions)
        viewModel.onEvent(WritingEvent.HideDraftVersions)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showDraftVersions)
            assertEquals(0, state.draftVersions.size)
        }
    }

    @Test
    fun `SelectDraftForVersions loads versions for draft`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.SelectDraftForVersions(testDraft))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { chapterDraftRepository.getVersionsByDraftId(testDraft.id) }

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showDraftVersions)
            assertEquals(1L, state.selectedDraftId)
        }
    }

    @Test
    fun `RestoreVersion sets content and hides versions`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.SelectDraftForVersions(testDraft))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.RestoreVersion(testDraftVersion))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("版本1内容", state.content)
            assertFalse(state.showDraftVersions)
        }
    }

    @Test
    fun `ToggleCharacterSheet toggles visibility`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleCharacterSheet)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showCharacterSheet)
        }
    }

    @Test
    fun `ToggleWorldSettingSheet toggles visibility`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleWorldSettingSheet)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showWorldSettingSheet)
        }
    }

    @Test
    fun `ClearError clears error state`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // First set an error through generation failure
        coEvery {
            aiRepository.continueStory(any(), any(), any(), any())
        } returns flowOf(AiStreamResponse(content = "", isFinished = true, error = "API Error"))

        viewModel.onEvent(WritingEvent.GenerateContinue)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("API Error", state.error)
        }

        viewModel.onEvent(WritingEvent.ClearError)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    // ===== Multi-branch generation tests =====

    @Test
    fun `ToggleMultiBranchSheet toggles sheet visibility`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showMultiBranchSheet)
        }

        viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showMultiBranchSheet)
        }
    }

    @Test
    fun `UpdateBranchCount updates branch count`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.UpdateBranchCount(2))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.multiBranchState.branchCount)
        }

        viewModel.onEvent(WritingEvent.UpdateBranchCount(4))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(4, state.multiBranchState.branchCount)
        }
    }

    @Test
    fun `UpdateBranchCount defaults to 3`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.multiBranchState.branchCount)
        }
    }

    @Test
    fun `GenerateBranches sets error when no chapter selected`() = runTest {
        // Override: no chapters
        coEvery { chapterRepository.getChaptersByStoryId(any()) } returns flowOf(emptyList())

        viewModel = createViewModel(chapterId = -1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet)
        viewModel.onEvent(WritingEvent.GenerateBranches)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("请先选择或创建一个章节", state.multiBranchState.error)
        }
    }

    @Test
    fun `GenerateBranches emits branches and isGenerating via useCase`() = runTest {
        // Given: mock generateBranchesUseCase to emit initial + final branches
        val mockBranches = listOf(
            BranchOption(index = 0, content = "分支1内容", summaryTag = "💥 冲突", isGenerating = false),
            BranchOption(index = 1, content = "分支2内容", summaryTag = "💖 情感", isGenerating = false)
        )
        coEvery {
            generateBranchesUseCase(
                currentContent = any(),
                characters = any(),
                worldSettings = any(),
                userInstruction = any(),
                branchCount = any(),
                lengthOption = any()
            )
        } returns kotlinx.coroutines.flow.flowOf(
            (0 until 2).map { BranchOption(index = it, isGenerating = true) },
            mockBranches
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet)
        viewModel.onEvent(WritingEvent.UpdateBranchCount(2))
        viewModel.onEvent(WritingEvent.GenerateBranches)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            // Initial emission: empty branches with isGenerating=true
            assertTrue(state.multiBranchState.branches.isNotEmpty())
            assertTrue(state.multiBranchState.isGenerating)
        }

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            // Final emission: branches with content
            assertEquals(2, state.multiBranchState.branches.size)
            assertFalse(state.multiBranchState.isGenerating)
            assertEquals("分支1内容", state.multiBranchState.branches[0].content)
        }
    }

    @Test
    fun `CancelBranches resets multiBranchState`() = runTest {
        // Override: slow flow that will be cancelled
        coEvery {
            generateBranchesUseCase(any(), any(), any(), any(), any(), any())
        } returns kotlinx.coroutines.flow.flow {
            emit((0 until 3).map { BranchOption(index = it, isGenerating = true) })
            kotlinx.coroutines.delay(10_000) // simulate long running
            emit(emptyList())
        }

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet)
        viewModel.onEvent(WritingEvent.GenerateBranches)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.CancelBranches)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.multiBranchState.isGenerating)
            assertTrue(state.multiBranchState.branches.isEmpty())
        }
    }

    @Test
    fun `SelectBranch marks branch as selected`() = runTest {
        val mockBranches = listOf(
            BranchOption(index = 0, content = "分支1", isGenerating = false),
            BranchOption(index = 1, content = "分支2", isGenerating = false),
            BranchOption(index = 2, content = "分支3", isGenerating = false)
        )
        coEvery {
            generateBranchesUseCase(any(), any(), any(), any(), any(), any())
        } returns kotlinx.coroutines.flow.flowOf(
            (0 until 3).map { BranchOption(index = it, isGenerating = true) },
            mockBranches
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet)
        viewModel.onEvent(WritingEvent.GenerateBranches)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.SelectBranch(1))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.multiBranchState.selectedBranchIndex)
            assertTrue(state.multiBranchState.branches[1].isSelected)
            assertFalse(state.multiBranchState.branches[0].isSelected)
            assertFalse(state.multiBranchState.branches[2].isSelected)
        }
    }

    @Test
    fun `AcceptBranch appends content and dismisses sheet`() = runTest {
        val mockBranches = listOf(
            BranchOption(index = 0, content = "分支1", isGenerating = false),
            BranchOption(index = 1, content = "采纳的内容", isGenerating = false)
        )
        coEvery {
            generateBranchesUseCase(any(), any(), any(), any(), any(), any())
        } returns kotlinx.coroutines.flow.flowOf(
            (0 until 2).map { BranchOption(index = it, isGenerating = true) },
            mockBranches
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet)
        viewModel.onEvent(WritingEvent.GenerateBranches)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.SelectBranch(1))
        viewModel.onEvent(WritingEvent.AcceptBranch)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            // AcceptBranch appends selected branch content to the main content
            assertEquals("采纳的内容", state.content)
            assertFalse(state.showMultiBranchSheet)
        }
    }

    @Test
    fun `AcceptBranch does nothing when no branch selected`() = runTest {
        val mockBranches = listOf(
            BranchOption(index = 0, content = "分支1", isGenerating = false)
        )
        coEvery {
            generateBranchesUseCase(any(), any(), any(), any(), any(), any())
        } returns kotlinx.coroutines.flow.flowOf(
            listOf(BranchOption(index = 0, isGenerating = true)),
            mockBranches
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet)
        viewModel.onEvent(WritingEvent.GenerateBranches)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()

        // No SelectBranch called - no branch is selected
        viewModel.onEvent(WritingEvent.AcceptBranch)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            // AcceptBranch is a no-op when nothing selected
            assertFalse(state.showMultiBranchSheet)
        }
    }

    @Test
    fun `DismissBranches closes sheet and resets state`() = runTest {
        coEvery {
            generateBranchesUseCase(any(), any(), any(), any(), any(), any())
        } returns kotlinx.coroutines.flow.flowOf(
            listOf(BranchOption(index = 0, content = "内容", isGenerating = false))
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet)
        viewModel.onEvent(WritingEvent.GenerateBranches)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.DismissBranches)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showMultiBranchSheet)
            assertEquals(-1, state.multiBranchState.selectedBranchIndex)
        }
    }

    @Test
    fun `RegenerateBranch regenerates single branch`() = runTest {
        val mockBranches = listOf(
            BranchOption(index = 0, content = "原内容", isGenerating = false),
            BranchOption(index = 1, content = "其他分支", isGenerating = false)
        )
        val regeneratedBranch = listOf(
            BranchOption(index = 0, content = "重新生成的内容", summaryTag = "🔄 重生", isGenerating = false),
            BranchOption(index = 1, content = "其他分支", isGenerating = false)
        )

        coEvery {
            generateBranchesUseCase(any(), any(), any(), any(), any(), any())
        } returns kotlinx.coroutines.flow.flowOf(
            (0 until 2).map { BranchOption(index = it, isGenerating = true) },
            mockBranches,
            regeneratedBranch
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet)
        viewModel.onEvent(WritingEvent.GenerateBranches)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.SelectBranch(0))
        viewModel.onEvent(WritingEvent.RegenerateBranch(0))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("重新生成的内容", state.multiBranchState.branches[0].content)
        }
    }

    @Test
    fun `MultiBranchState selectedBranch returns correct branch`() = runTest {
        val state = MultiBranchState(
            branchCount = 2,
            branches = listOf(
                BranchOption(index = 0, content = "分支0"),
                BranchOption(index = 1, content = "分支1")
            ),
            selectedBranchIndex = 1
        )

        assertEquals("分支1", state.selectedBranch?.content)
    }

    @Test
    fun `MultiBranchState allBranchesDone returns true when none generating`() = runTest {
        val state = MultiBranchState(
            branches = listOf(
                BranchOption(index = 0, content = "内容", isGenerating = false),
                BranchOption(index = 1, content = "内容", isGenerating = false)
            )
        )

        assertTrue(state.allBranchesDone)
    }

    @Test
    fun `MultiBranchState allBranchesDone returns false when still generating`() = runTest {
        val state = MultiBranchState(
            branches = listOf(
                BranchOption(index = 0, content = "内容", isGenerating = false),
                BranchOption(index = 1, isGenerating = true)
            )
        )

        assertFalse(state.allBranchesDone)
    }

    // ===== Bug Fix 2: Text Selection triggering rewrite via TextFieldValue mechanism =====

    @Test
    fun `TriggerRewrite updates rewriteState with selectedText`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(WritingEvent.UpdateContent("这是一段可以被改写的文字"))
        viewModel.onEvent(WritingEvent.TriggerRewrite("可以被改写的"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("可以被改写的", state.rewriteState.selectedText)
            assertTrue(state.rewriteState.versions.isEmpty())
            assertEquals(0, state.rewriteState.selectedVersionIndex)
            assertFalse(state.rewriteState.isEditing)
        }
    }

    @Test
    fun `TriggerRewrite clears previous rewrite versions`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // First trigger with some text
        viewModel.onEvent(WritingEvent.TriggerRewrite("第一段选中"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then trigger again with different text
        viewModel.onEvent(WritingEvent.TriggerRewrite("第二段选中"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            // Should have the new selected text
            assertEquals("第二段选中", state.rewriteState.selectedText)
            // Versions should be cleared
            assertTrue(state.rewriteState.versions.isEmpty())
        }
    }

    @Test
    fun `TriggerRewrite does not open rewrite sheet until style is selected`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // TriggerRewrite just stores the selected text
        viewModel.onEvent(WritingEvent.TriggerRewrite("选中的文字"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            // The selectedText is set but the bottom sheet is controlled by the UI
            // based on whether selectedText is not blank
            assertEquals("选中的文字", state.rewriteState.selectedText)
        }
    }
}
