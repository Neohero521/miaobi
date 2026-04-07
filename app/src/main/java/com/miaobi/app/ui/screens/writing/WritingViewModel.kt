package com.miaobi.app.ui.screens.writing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaobi.app.domain.model.*
import com.miaobi.app.domain.model.ContinuationSuggestion
import com.miaobi.app.domain.model.MultiBranchState
import com.miaobi.app.domain.model.RewriteState
import com.miaobi.app.domain.repository.*
import com.miaobi.app.domain.usecase.GenerateBranchesUseCase
import com.miaobi.app.domain.usecase.GenerateInspirationUseCase
import com.miaobi.app.domain.usecase.RewriteTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WritingUiState(
    // === 新增：沉浸模式与工具栏 ===
    val isImmersiveMode: Boolean = false,
    val showToolbar: Boolean = true,

    // === 新增：底部抽屉 ===
    val showContinuationPanel: Boolean = false,
    val continuationSuggestions: List<ContinuationSuggestion> = emptyList(),

    // === 新增：Rewrite 风格 Tab ===
    val showRewriteStyleRow: Boolean = false,
    val selectedStyle: RewriteStyle = RewriteStyle.MODERN,

    // === 新增：V/O 模式 ===
    val isVoMode: Boolean = true,

    // === 新增：Undo/Redo 栈 ===
    val undoStack: List<String> = emptyList(),
    val redoStack: List<String> = emptyList(),

    // === 现有字段（保留）===
    val story: Story? = null,
    val currentChapter: Chapter? = null,
    val chapters: List<Chapter> = emptyList(),
    val characters: List<Character> = emptyList(),
    val worldSettings: List<WorldSetting> = emptyList(),
    val drafts: List<ChapterDraft> = emptyList(),
    val draftVersions: List<ChapterDraftVersion> = emptyList(),
    val selectedDraftId: Long? = null,
    val content: String = "",
    val userPrompt: String = "",
    val lengthOption: LengthOption = LengthOption.MEDIUM,
    val isGenerating: Boolean = false,
    val generatedContent: String = "",
    val error: String? = null,
    val isLoading: Boolean = true,
    val showChapterList: Boolean = false,
    val showCharacterSheet: Boolean = false,
    val showWorldSettingSheet: Boolean = false,
    val showDraftHistory: Boolean = false,
    val showDraftVersions: Boolean = false,
    val rewriteState: RewriteState = RewriteState(),
    val showAddChapterDialog: Boolean = false,
    val newChapterTitle: String = "",
    val showAddCharacterDialog: Boolean = false,
    val newCharacterName: String = "",
    val newCharacterDescription: String = "",
    val showAddWorldSettingDialog: Boolean = false,
    val newWorldSettingName: String = "",
    val newWorldSettingContent: String = "",
    val aiConfigValid: Boolean = true,
    val multiBranchState: MultiBranchState = MultiBranchState(),
    val showMultiBranchSheet: Boolean = false,
    val inspirationState: InspirationState = InspirationState(),
    val showInspirationSheet: Boolean = false
)

sealed class WritingEvent {
    // === 新增：沉浸模式与工具栏 ===
    object ToggleImmersiveMode : WritingEvent()
    object ToggleToolbar : WritingEvent()
    object Undo : WritingEvent()
    object Redo : WritingEvent()

    // === 新增：AI 续写抽屉 ===
    object ToggleContinuationPanel : WritingEvent()
    data class UseContinuationSuggestion(val index: Int) : WritingEvent()
    object RegenerateSuggestions : WritingEvent()

    // === 新增：Rewrite 风格 ===
    object ToggleRewriteStyleRow : WritingEvent()
    object Polish : WritingEvent()
    data class SelectStyle(val style: RewriteStyle) : WritingEvent()

    // === 新增：V/O 模式 ===
    object ToggleVoMode : WritingEvent()

    // Content
    data class UpdateContent(val content: String) : WritingEvent()
    data class UpdatePrompt(val prompt: String) : WritingEvent()
    data class UpdateLengthOption(val lengthOption: LengthOption) : WritingEvent()
    object SaveContent : WritingEvent()
    object GenerateContinue : WritingEvent()
    object CancelGeneration : WritingEvent()
    object AcceptGenerated : WritingEvent()
    object DiscardGenerated : WritingEvent()

    // Chapter
    object ToggleChapterList : WritingEvent()
    object ShowAddChapterDialog : WritingEvent()
    object HideAddChapterDialog : WritingEvent()
    data class UpdateNewChapterTitle(val title: String) : WritingEvent()
    object AddChapter : WritingEvent()
    data class SelectChapter(val chapter: Chapter) : WritingEvent()
    data class DeleteChapter(val chapterId: Long) : WritingEvent()

    // Characters
    object ToggleCharacterSheet : WritingEvent()
    object ShowAddCharacterDialog : WritingEvent()
    object HideAddCharacterDialog : WritingEvent()
    data class UpdateNewCharacterName(val name: String) : WritingEvent()
    data class UpdateNewCharacterDescription(val description: String) : WritingEvent()
    object AddCharacter : WritingEvent()
    data class DeleteCharacter(val characterId: Long) : WritingEvent()

    // World Settings
    object ToggleWorldSettingSheet : WritingEvent()
    object ShowAddWorldSettingDialog : WritingEvent()
    object HideAddWorldSettingDialog : WritingEvent()
    data class UpdateNewWorldSettingName(val name: String) : WritingEvent()
    data class UpdateNewWorldSettingContent(val content: String) : WritingEvent()
    object AddWorldSetting : WritingEvent()
    data class DeleteWorldSetting(val settingId: Long) : WritingEvent()

    // Drafts
    object ToggleDraftHistory : WritingEvent()
    data class SelectDraft(val draft: ChapterDraft) : WritingEvent()
    data class DeleteDraft(val draftId: Long) : WritingEvent()

    // Draft Versions
    object ShowDraftVersions : WritingEvent()
    object HideDraftVersions : WritingEvent()
    data class SelectDraftForVersions(val draft: ChapterDraft) : WritingEvent()
    data class RestoreVersion(val version: ChapterDraftVersion) : WritingEvent()
    data class DeleteVersion(val versionId: Long) : WritingEvent()

    // UI
    object ClearError : WritingEvent()

    // Rewrite
    data class TriggerRewrite(val selectedText: String) : WritingEvent()
    data class SelectRewriteStyle(val style: RewriteStyle) : WritingEvent()
    object ExecuteRewrite : WritingEvent()
    object CancelRewrite : WritingEvent()
    data class SelectRewriteVersion(val index: Int) : WritingEvent()
    object AcceptRewrite : WritingEvent()
    object StartEditRewrite : WritingEvent()
    data class UpdateEditingText(val text: String) : WritingEvent()
    object ConfirmEditRewrite : WritingEvent()
    object CancelEditRewrite : WritingEvent()
    object RegenerateRewrite : WritingEvent()
    object DismissRewrite : WritingEvent()

    // Multi-branch
    object ToggleMultiBranchSheet : WritingEvent()
    data class UpdateBranchCount(val count: Int) : WritingEvent()
    object GenerateBranches : WritingEvent()
    object CancelBranches : WritingEvent()
    data class SelectBranch(val index: Int) : WritingEvent()
    object AcceptBranch : WritingEvent()
    data class RegenerateBranch(val index: Int) : WritingEvent()
    object DismissBranches : WritingEvent()

    // Inspiration
    object ToggleInspirationSheet : WritingEvent()
    data class FilterInspirationType(val type: InspirationType?) : WritingEvent()
    object GenerateInspiration : WritingEvent()
    object CancelInspiration : WritingEvent()
    data class SelectInspirationOption(val index: Int) : WritingEvent()
    data class AcceptInspiration(val option: InspirationOption) : WritingEvent()
    data class ToggleInspirationFavorite(val index: Int) : WritingEvent()
    object DismissInspiration : WritingEvent()

}

@HiltViewModel
class WritingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storyRepository: StoryRepository,
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldSettingRepository: WorldSettingRepository,
    private val chapterDraftRepository: ChapterDraftRepository,
    private val aiRepository: AiRepository,
    private val rewriteTextUseCase: RewriteTextUseCase,
    private val generateBranchesUseCase: GenerateBranchesUseCase,
    private val generateInspirationUseCase: GenerateInspirationUseCase
) : ViewModel() {

    private val storyId: Long = savedStateHandle.get<Long>("storyId") ?: -1L
    private val initialChapterId: Long = savedStateHandle.get<Long>("chapterId") ?: -1L

    private val _uiState = MutableStateFlow(WritingUiState())
    val uiState: StateFlow<WritingUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null
    private var rewriteJob: Job? = null
    private var multiBranchJob: Job? = null
    private var inspirationJob: Job? = null
    init {
        loadStory()
    }

    private fun loadStory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load story
            storyRepository.getStoryById(storyId).collect { story ->
                _uiState.update { it.copy(story = story) }
            }
        }

        viewModelScope.launch {
            // Load chapters
            chapterRepository.getChaptersByStoryId(storyId).collect { chapters ->
                _uiState.update { state ->
                    val updatedChapters = chapters
                    // Select initial chapter if provided, otherwise first chapter
                    val selectedChapter = when {
                        initialChapterId > 0 -> chapters.find { it.id == initialChapterId }
                        state.currentChapter == null && chapters.isNotEmpty() -> chapters.first()
                        else -> state.currentChapter
                    }
                    state.copy(
                        chapters = updatedChapters,
                        currentChapter = selectedChapter,
                        content = selectedChapter?.content ?: "",
                        isLoading = false
                    )
                }
                // Load drafts for current chapter
                _uiState.value.currentChapter?.let { chapter ->
                    loadDrafts(chapter.id)
                }
            }
        }

        viewModelScope.launch {
            // Load characters
            characterRepository.getCharactersByStoryId(storyId).collect { characters ->
                _uiState.update { it.copy(characters = characters) }
            }
        }

        viewModelScope.launch {
            // Load world settings
            worldSettingRepository.getWorldSettingsByStoryId(storyId).collect { settings ->
                _uiState.update { it.copy(worldSettings = settings) }
            }
        }
    }

    private fun loadDrafts(chapterId: Long) {
        viewModelScope.launch {
            chapterDraftRepository.getDraftsByChapterId(chapterId).collect { drafts ->
                _uiState.update { it.copy(drafts = drafts) }
            }
        }
    }

    private fun loadDraftVersions(draftId: Long) {
        viewModelScope.launch {
            chapterDraftRepository.getVersionsByDraftId(draftId).collect { versions ->
                _uiState.update { it.copy(draftVersions = versions) }
            }
        }
    }

    fun onEvent(event: WritingEvent) {
        when (event) {
            // === 新增：沉浸模式与工具栏 ===
            WritingEvent.ToggleImmersiveMode -> {
                _uiState.update { it.copy(isImmersiveMode = !it.isImmersiveMode) }
            }

            WritingEvent.ToggleToolbar -> {
                _uiState.update { it.copy(showToolbar = !it.showToolbar) }
            }

            WritingEvent.Undo -> {
                val stack = _uiState.value.undoStack
                if (stack.isNotEmpty()) {
                    val previous = stack.last()
                    _uiState.update {
                        it.copy(
                            content = previous,
                            undoStack = stack.dropLast(1),
                            redoStack = it.redoStack + it.content
                        )
                    }
                }
            }

            WritingEvent.Redo -> {
                val stack = _uiState.value.redoStack
                if (stack.isNotEmpty()) {
                    val next = stack.last()
                    _uiState.update {
                        it.copy(
                            content = next,
                            redoStack = stack.dropLast(1),
                            undoStack = it.undoStack + it.content
                        )
                    }
                }
            }

            // === 新增：AI 续写抽屉 ===
            WritingEvent.ToggleContinuationPanel -> {
                _uiState.update { it.copy(showContinuationPanel = !it.showContinuationPanel) }
            }

            is WritingEvent.UseContinuationSuggestion -> {
                val suggestion = _uiState.value.continuationSuggestions.getOrNull(event.index)
                if (suggestion != null) {
                    val newContent = _uiState.value.content + suggestion.content
                    _uiState.update { state ->
                        state.copy(
                            content = newContent,
                            currentChapter = state.currentChapter?.copy(content = newContent),
                            continuationSuggestions = emptyList(),
                            showContinuationPanel = false
                        )
                    }
                    saveContent()
                }
            }

            WritingEvent.RegenerateSuggestions -> {
                generateContinuation()
            }

            // === 新增：Rewrite 风格 Tab ===
            WritingEvent.ToggleRewriteStyleRow -> {
                _uiState.update { it.copy(showRewriteStyleRow = !it.showRewriteStyleRow) }
            }

            is WritingEvent.SelectStyle -> {
                _uiState.update { it.copy(selectedStyle = event.style) }
            }

            WritingEvent.Polish -> {
                // 润色：以全文为 selectedText 执行改写（不替换原文）
                val text = _uiState.value.content
                if (text.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            rewriteState = it.rewriteState.copy(
                                selectedText = text,
                                versions = emptyList(),
                                selectedVersionIndex = 0,
                                isEditing = false,
                                editingText = "",
                                error = null
                            )
                        )
                    }
                    executeRewrite()
                }
            }

            // === 新增：V/O 模式 ===
            WritingEvent.ToggleVoMode -> {
                _uiState.update { it.copy(isVoMode = !it.isVoMode) }
            }

            is WritingEvent.UpdateContent -> {
                _uiState.update { state ->
                    val maxUndo = 50
                    val newUndoStack = if (state.content != event.content) {
                        (state.undoStack + state.content).takeLast(maxUndo)
                    } else state.undoStack
                    // 同步更新 currentChapter，确保 saveContent() 使用正确的 content
                    val updatedChapter = state.currentChapter?.copy(content = event.content)
                    state.copy(
                        content = event.content,
                        currentChapter = updatedChapter,
                        undoStack = newUndoStack,
                        redoStack = emptyList() // 清空 redo 栈
                    )
                }
            }

            is WritingEvent.UpdatePrompt -> {
                _uiState.update { it.copy(userPrompt = event.prompt) }
            }

            is WritingEvent.UpdateLengthOption -> {
                _uiState.update { it.copy(lengthOption = event.lengthOption) }
            }

            WritingEvent.SaveContent -> saveContent()

            WritingEvent.GenerateContinue -> generateContinuation()

            WritingEvent.CancelGeneration -> {
                generationJob?.cancel()
                _uiState.update { it.copy(isGenerating = false, generatedContent = "") }
            }

            WritingEvent.AcceptGenerated -> {
                acceptGenerated()
            }

            WritingEvent.DiscardGenerated -> {
                _uiState.update { it.copy(generatedContent = "") }
            }

            // Chapter
            WritingEvent.ToggleChapterList -> {
                _uiState.update { it.copy(showChapterList = !it.showChapterList) }
            }

            WritingEvent.ShowAddChapterDialog -> {
                _uiState.update { it.copy(showAddChapterDialog = true, newChapterTitle = "") }
            }

            WritingEvent.HideAddChapterDialog -> {
                _uiState.update { it.copy(showAddChapterDialog = false, newChapterTitle = "") }
            }

            is WritingEvent.UpdateNewChapterTitle -> {
                _uiState.update { it.copy(newChapterTitle = event.title) }
            }

            WritingEvent.AddChapter -> addChapter()

            is WritingEvent.SelectChapter -> selectChapter(event.chapter)

            is WritingEvent.DeleteChapter -> deleteChapter(event.chapterId)

            // Characters
            WritingEvent.ToggleCharacterSheet -> {
                _uiState.update { it.copy(showCharacterSheet = !it.showCharacterSheet) }
            }

            WritingEvent.ShowAddCharacterDialog -> {
                _uiState.update {
                    it.copy(
                        showAddCharacterDialog = true,
                        newCharacterName = "",
                        newCharacterDescription = ""
                    )
                }
            }

            WritingEvent.HideAddCharacterDialog -> {
                _uiState.update { it.copy(showAddCharacterDialog = false) }
            }

            is WritingEvent.UpdateNewCharacterName -> {
                _uiState.update { it.copy(newCharacterName = event.name) }
            }

            is WritingEvent.UpdateNewCharacterDescription -> {
                _uiState.update { it.copy(newCharacterDescription = event.description) }
            }

            WritingEvent.AddCharacter -> addCharacter()

            is WritingEvent.DeleteCharacter -> deleteCharacter(event.characterId)

            // World Settings
            WritingEvent.ToggleWorldSettingSheet -> {
                _uiState.update { it.copy(showWorldSettingSheet = !it.showWorldSettingSheet) }
            }

            WritingEvent.ShowAddWorldSettingDialog -> {
                _uiState.update {
                    it.copy(
                        showAddWorldSettingDialog = true,
                        newWorldSettingName = "",
                        newWorldSettingContent = ""
                    )
                }
            }

            WritingEvent.HideAddWorldSettingDialog -> {
                _uiState.update { it.copy(showAddWorldSettingDialog = false) }
            }

            is WritingEvent.UpdateNewWorldSettingName -> {
                _uiState.update { it.copy(newWorldSettingName = event.name) }
            }

            is WritingEvent.UpdateNewWorldSettingContent -> {
                _uiState.update { it.copy(newWorldSettingContent = event.content) }
            }

            WritingEvent.AddWorldSetting -> addWorldSetting()

            is WritingEvent.DeleteWorldSetting -> deleteWorldSetting(event.settingId)

            // Drafts
            WritingEvent.ToggleDraftHistory -> {
                _uiState.update { it.copy(showDraftHistory = !it.showDraftHistory) }
            }

            is WritingEvent.SelectDraft -> selectDraft(event.draft)

            is WritingEvent.DeleteDraft -> deleteDraft(event.draftId)

            // Draft Versions
            WritingEvent.ShowDraftVersions -> {
                _uiState.update { it.copy(showDraftVersions = !it.showDraftVersions) }
            }

            WritingEvent.HideDraftVersions -> {
                _uiState.update { it.copy(showDraftVersions = false, draftVersions = emptyList()) }
            }

            is WritingEvent.SelectDraftForVersions -> {
                _uiState.update { it.copy(selectedDraftId = event.draft.id, showDraftVersions = true) }
                loadDraftVersions(event.draft.id)
            }

            is WritingEvent.RestoreVersion -> restoreVersion(event.version)

            is WritingEvent.DeleteVersion -> deleteVersion(event.versionId)

            // Rewrite
            is WritingEvent.TriggerRewrite -> {
                _uiState.update {
                    it.copy(
                        rewriteState = it.rewriteState.copy(
                            selectedText = event.selectedText,
                            versions = emptyList(),
                            selectedVersionIndex = 0,
                            isEditing = false,
                            editingText = "",
                            error = null
                        )
                    )
                }
            }

            is WritingEvent.SelectRewriteStyle -> {
                _uiState.update {
                    it.copy(
                        rewriteState = it.rewriteState.copy(
                            selectedStyle = event.style,
                            versions = emptyList(),
                            selectedVersionIndex = 0
                        )
                    )
                }
            }

            WritingEvent.ExecuteRewrite -> executeRewrite()

            WritingEvent.CancelRewrite -> {
                rewriteJob?.cancel()
                _uiState.update {
                    it.copy(rewriteState = it.rewriteState.copy(isRewriting = false, error = null))
                }
            }

            is WritingEvent.SelectRewriteVersion -> {
                _uiState.update {
                    val updated = it.rewriteState.versions.mapIndexed { idx, v ->
                        v.copy(isSelected = idx == event.index)
                    }
                    it.copy(
                        rewriteState = it.rewriteState.copy(
                            versions = updated,
                            selectedVersionIndex = event.index
                        )
                    )
                }
            }

            WritingEvent.AcceptRewrite -> acceptRewrite()

            WritingEvent.StartEditRewrite -> {
                val version = _uiState.value.rewriteState.selectedVersion
                _uiState.update {
                    it.copy(
                        rewriteState = it.rewriteState.copy(
                            isEditing = true,
                            editingText = version?.content ?: ""
                        )
                    )
                }
            }

            is WritingEvent.UpdateEditingText -> {
                _uiState.update {
                    it.copy(rewriteState = it.rewriteState.copy(editingText = event.text))
                }
            }

            WritingEvent.ConfirmEditRewrite -> {
                // Update selected version with edited text
                val idx = _uiState.value.rewriteState.selectedVersionIndex
                _uiState.update {
                    val updated = it.rewriteState.versions.mapIndexed { vidx, v ->
                        if (vidx == idx) v.copy(content = it.rewriteState.editingText) else v
                    }
                    it.copy(
                        rewriteState = it.rewriteState.copy(
                            versions = updated,
                            isEditing = false,
                            editingText = ""
                        )
                    )
                }
            }

            WritingEvent.CancelEditRewrite -> {
                _uiState.update {
                    it.copy(
                        rewriteState = it.rewriteState.copy(
                            isEditing = false,
                            editingText = ""
                        )
                    )
                }
            }

            WritingEvent.RegenerateRewrite -> executeRewrite()

            WritingEvent.DismissRewrite -> {
                rewriteJob?.cancel()
                _uiState.update {
                    it.copy(rewriteState = RewriteState())
                }
            }

            WritingEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }

            // Multi-branch
            WritingEvent.ToggleMultiBranchSheet -> {
                _uiState.update { it.copy(showMultiBranchSheet = !it.showMultiBranchSheet) }
            }

            is WritingEvent.UpdateBranchCount -> {
                _uiState.update {
                    it.copy(multiBranchState = it.multiBranchState.copy(branchCount = event.count))
                }
            }

            WritingEvent.GenerateBranches -> generateBranches()

            WritingEvent.CancelBranches -> {
                multiBranchJob?.cancel()
                _uiState.update {
                    it.copy(
                        multiBranchState = MultiBranchState(branchCount = it.multiBranchState.branchCount),
                        isGenerating = false
                    )
                }
            }

            is WritingEvent.SelectBranch -> {
                _uiState.update {
                    val updated = it.multiBranchState.branches.mapIndexed { idx, b ->
                        b.copy(isSelected = idx == event.index)
                    }
                    it.copy(
                        multiBranchState = it.multiBranchState.copy(
                            branches = updated,
                            selectedBranchIndex = event.index
                        )
                    )
                }
            }

            WritingEvent.AcceptBranch -> acceptBranch()

            is WritingEvent.RegenerateBranch -> regenerateSingleBranch(event.index)

            WritingEvent.DismissBranches -> {
                multiBranchJob?.cancel()
                _uiState.update {
                    it.copy(
                        showMultiBranchSheet = false,
                        multiBranchState = MultiBranchState(branchCount = it.multiBranchState.branchCount)
                    )
                }
            }

            // Inspiration
            WritingEvent.ToggleInspirationSheet -> {
                _uiState.update { it.copy(showInspirationSheet = !it.showInspirationSheet) }
            }

            is WritingEvent.FilterInspirationType -> {
                _uiState.update {
                    it.copy(inspirationState = it.inspirationState.copy(selectedType = event.type))
                }
            }

            WritingEvent.GenerateInspiration -> generateInspiration()

            WritingEvent.CancelInspiration -> {
                inspirationJob?.cancel()
                _uiState.update {
                    it.copy(
                        inspirationState = InspirationState(),
                        isGenerating = false
                    )
                }
            }

            is WritingEvent.SelectInspirationOption -> {
                _uiState.update {
                    val updated = it.inspirationState.options.mapIndexed { idx, opt ->
                        opt.copy(isSelected = idx == event.index)
                    }
                    it.copy(inspirationState = it.inspirationState.copy(options = updated))
                }
            }

            is WritingEvent.AcceptInspiration -> acceptInspiration(event.option)

            is WritingEvent.ToggleInspirationFavorite -> {
                _uiState.update {
                    val favorites = it.inspirationState.favorites.toMutableSet()
                    if (favorites.contains(event.index)) {
                        favorites.remove(event.index)
                    } else {
                        favorites.add(event.index)
                    }
                    it.copy(inspirationState = it.inspirationState.copy(favorites = favorites))
                }
            }

            WritingEvent.DismissInspiration -> {
                inspirationJob?.cancel()
                _uiState.update {
                    it.copy(
                        showInspirationSheet = false,
                        inspirationState = InspirationState()
                    )
                }
            }
        }
    }

    private fun saveContent() {
        val chapter = _uiState.value.currentChapter ?: return
        val content = _uiState.value.content

        viewModelScope.launch {
            chapterRepository.updateChapterContent(chapter.id, content)
        }
    }

    private fun generateContinuation() {
        val chapter = _uiState.value.currentChapter
        if (chapter == null) {
            _uiState.update { it.copy(error = "请先选择或创建一个章节") }
            return
        }

        val charactersText = _uiState.value.characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val worldSettingsText = _uiState.value.worldSettings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }

        generationJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatedContent = "",
                    error = null,
                    showContinuationPanel = true,
                    continuationSuggestions = emptyList()
                )
            }

            val fullContent = if (chapter.content.isNotBlank()) {
                chapter.content
            } else {
                _uiState.value.content
            }

            val accumulated = StringBuilder()
            aiRepository.continueStory(
                prompt = _uiState.value.userPrompt,
                characters = charactersText,
                worldSettings = worldSettingsText,
                historyContent = fullContent,
                lengthOption = _uiState.value.lengthOption
            ).collect { response ->
                if (response.error != null) {
                    _uiState.update {
                        it.copy(isGenerating = false, error = response.error)
                    }
                } else if (response.isFinished) {
                    // Split accumulated content into 3 suggestions
                    val suggestions = splitIntoSuggestions(accumulated.toString())
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            continuationSuggestions = suggestions,
                            generatedContent = accumulated.toString()
                        )
                    }
                } else {
                    accumulated.append(response.content)
                    // Progress bar is intentionally hidden — we show "生成中…" in the UI
                    // since we cannot know total length without Content-Length header.
                    _uiState.update {
                        it.copy(generatedContent = accumulated.toString())
                    }
                }
            }
        }
    }

    /**
     * 将累积的续写内容拆分为 3 条独立方向的建议
     * 优先解析 AI 返回的 `===方向X===` 分隔符；若无分隔符则均分内容。
     */
    private fun splitIntoSuggestions(content: String): List<ContinuationSuggestion> {
        if (content.isBlank()) return emptyList()

        // 尝试按方向标记分割
        val directionRegex = Regex("""===方向(\d+)===""")
        val parts = content.split(directionRegex)

        // parts[0] 是方向1之前的内容，parts[1]是方向1的内容，parts[2]是方向2的内容...
        // 正则 split 的结果是：["", "方向1内容", "方向2内容", "方向3内容"]
        if (parts.size >= 4) {
            // parts[0] 为空串（方向标记前的空内容），从 parts[1] 开始是实际内容
            val suggestions = listOf(1, 2, 3).mapNotNull { index ->
                val partContent = parts.getOrNull(index)?.trim() ?: return@mapNotNull null
                if (partContent.isBlank()) return@mapNotNull null
                ContinuationSuggestion(
                    id = index - 1,
                    content = partContent,
                    wordCount = partContent.length,
                    isSelected = index == 1
                )
            }
            if (suggestions.isNotEmpty()) return suggestions
        }

        // 回退：均分内容为3段（保持向后兼容）
        val totalLength = content.length
        val partSize = totalLength / 3

        return listOf(0, 1, 2).map { index ->
            val start = index * partSize
            val end = if (index == 2) totalLength else (index + 1) * partSize
            val partContent = content.substring(start, end.coerceAtMost(totalLength)).trim()
            ContinuationSuggestion(
                id = index,
                content = partContent,
                wordCount = partContent.length,
                isSelected = index == 0
            )
        }.filter { it.content.isNotBlank() }
    }

    private fun acceptGenerated() {
        val newContent = _uiState.value.content + _uiState.value.generatedContent
        val updatedChapter = _uiState.value.currentChapter?.copy(content = newContent)
        _uiState.update {
            it.copy(
                content = newContent,
                currentChapter = updatedChapter,
                generatedContent = "",
                userPrompt = ""
            )
        }
        saveContent()
    }

    private fun executeRewrite() {
        val rewriteState = _uiState.value.rewriteState
        if (!rewriteState.canRewrite) return

        val charactersText = _uiState.value.characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val worldSettingsText = _uiState.value.worldSettings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }

        rewriteJob?.cancel()
        rewriteJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    rewriteState = it.rewriteState.copy(
                        isRewriting = true,
                        versions = emptyList(),
                        selectedVersionIndex = 0,
                        error = null
                    )
                )
            }

            val fullResponse = StringBuilder()
            val delimiter = RewriteStyle.VERSION_DELIMITER

            rewriteTextUseCase(
                originalText = rewriteState.selectedText,
                style = rewriteState.selectedStyle,
                characters = charactersText,
                worldSettings = worldSettingsText
            ).collect { response ->
                if (response.error != null) {
                    _uiState.update {
                        it.copy(rewriteState = it.rewriteState.copy(isRewriting = false, error = response.error))
                    }
                    return@collect
                }

                if (response.isFinished) {
                    // Parse all versions from accumulated response
                    val versions = parseRewriteVersions(fullResponse.toString(), delimiter)
                    _uiState.update {
                        it.copy(
                            rewriteState = it.rewriteState.copy(
                                isRewriting = false,
                                versions = versions
                            )
                        )
                    }
                } else {
                    fullResponse.append(response.content)
                }
            }
        }
    }

    private fun parseRewriteVersions(rawResponse: String, delimiter: String): List<RewriteVersion> {
        val parts = rawResponse.split(delimiter).map { it.trim() }.filter { it.isNotBlank() }
        return parts.take(3).mapIndexed { idx, content ->
            RewriteVersion(index = idx, content = content, isSelected = idx == 0)
        }
    }

    private fun acceptRewrite() {
        val rewriteState = _uiState.value.rewriteState
        val version = rewriteState.selectedVersion ?: return
        val originalText = rewriteState.selectedText
        val newContent = version.content

        // Replace selected text with the chosen rewrite version in the content
        val currentContent = _uiState.value.content
        val newFullContent = currentContent.replace(originalText, newContent)
        val updatedChapter = _uiState.value.currentChapter?.copy(content = newFullContent)

        _uiState.update {
            it.copy(
                content = newFullContent,
                currentChapter = updatedChapter,
                rewriteState = RewriteState()
            )
        }
        saveContent()
    }

    private fun generateBranches() {
        val chapter = _uiState.value.currentChapter
        if (chapter == null) {
            _uiState.update { it.copy(multiBranchState = it.multiBranchState.copy(error = "请先选择或创建一个章节")) }
            return
        }

        val charactersText = _uiState.value.characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val worldSettingsText = _uiState.value.worldSettings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }

        multiBranchJob?.cancel()
        multiBranchJob = viewModelScope.launch {
            val branchCount = _uiState.value.multiBranchState.branchCount
            val fullContent = _uiState.value.content.ifBlank { chapter.content }

            _uiState.update {
                it.copy(
                    multiBranchState = it.multiBranchState.copy(
                        branches = (0 until branchCount).map { i -> BranchOption(index = i) },
                        isGenerating = true,
                        error = null
                    )
                )
            }

            generateBranchesUseCase(
                currentContent = fullContent,
                characters = charactersText,
                worldSettings = worldSettingsText,
                userInstruction = _uiState.value.userPrompt.takeIf { p -> p.isNotBlank() },
                branchCount = branchCount,
                lengthOption = _uiState.value.lengthOption
            ).collect { branches ->
                _uiState.update {
                    it.copy(multiBranchState = it.multiBranchState.copy(branches = branches))
                }
            }

            _uiState.update {
                it.copy(multiBranchState = it.multiBranchState.copy(isGenerating = false))
            }
        }
    }

    private fun regenerateSingleBranch(branchIndex: Int) {
        val chapter = _uiState.value.currentChapter ?: return
        val charactersText = _uiState.value.characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val worldSettingsText = _uiState.value.worldSettings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }

        viewModelScope.launch {
            // Mark the specific branch as generating
            val updatedBranches = _uiState.value.multiBranchState.branches.map {
                if (it.index == branchIndex) it.copy(isGenerating = true, content = "", error = null)
                else it
            }
            _uiState.update { it.copy(multiBranchState = it.multiBranchState.copy(branches = updatedBranches)) }

            val fullContent = _uiState.value.content.ifBlank { chapter.content }
            val branchCount = _uiState.value.multiBranchState.branchCount

            generateBranchesUseCase(
                currentContent = fullContent,
                characters = charactersText,
                worldSettings = worldSettingsText,
                userInstruction = _uiState.value.userPrompt.takeIf { p -> p.isNotBlank() },
                branchCount = branchCount,
                lengthOption = _uiState.value.lengthOption
            ).collect { branches ->
                // Only update the specific branch
                val currentBranches = _uiState.value.multiBranchState.branches.toMutableList()
                val newBranch = branches.find { it.index == branchIndex }
                if (newBranch != null) {
                    val idx = currentBranches.indexOfFirst { it.index == branchIndex }
                    if (idx >= 0) {
                        currentBranches[idx] = newBranch.copy(isSelected = currentBranches[idx].isSelected)
                    }
                }
                _uiState.update { it.copy(multiBranchState = it.multiBranchState.copy(branches = currentBranches.toList())) }
            }
        }
    }

    private fun acceptBranch() {
        val selectedBranch = _uiState.value.multiBranchState.selectedBranch ?: return
        if (!selectedBranch.canAccept) return

        val newContent = _uiState.value.content + selectedBranch.content
        val updatedChapter = _uiState.value.currentChapter?.copy(content = newContent)

        _uiState.update {
            it.copy(
                content = newContent,
                currentChapter = updatedChapter,
                showMultiBranchSheet = false,
                multiBranchState = MultiBranchState(branchCount = it.multiBranchState.branchCount),
                userPrompt = ""
            )
        }
        saveContent()
    }

    private fun generateInspiration() {
        val chapter = _uiState.value.currentChapter
        if (chapter == null) {
            _uiState.update { it.copy(inspirationState = it.inspirationState.copy(error = "请先选择或创建一个章节")) }
            return
        }

        val charactersText = _uiState.value.characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val worldSettingsText = _uiState.value.worldSettings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }

        inspirationJob?.cancel()
        inspirationJob = viewModelScope.launch {
            val typeFilter = _uiState.value.inspirationState.selectedType
            val fullContent = _uiState.value.content.ifBlank { chapter.content }

            _uiState.update {
                it.copy(
                    inspirationState = it.inspirationState.copy(
                        options = emptyList(),
                        isGenerating = true,
                        error = null
                    )
                )
            }

            generateInspirationUseCase(
                currentContent = fullContent,
                characters = charactersText,
                worldSettings = worldSettingsText,
                count = 4,
                typeFilter = typeFilter
            ).collect { options ->
                _uiState.update {
                    it.copy(inspirationState = it.inspirationState.copy(options = options))
                }
            }

            _uiState.update {
                it.copy(inspirationState = it.inspirationState.copy(isGenerating = false))
            }
        }
    }

    private fun acceptInspiration(option: InspirationOption) {
        if (!option.canAccept) return

        val newContent = _uiState.value.content + "\n\n" + option.content
        val updatedChapter = _uiState.value.currentChapter?.copy(content = newContent)
        _uiState.update {
            it.copy(
                content = newContent,
                currentChapter = updatedChapter,
                showInspirationSheet = false,
                inspirationState = InspirationState()
            )
        }
        saveContent()
    }

    private fun addChapter() {
        val title = _uiState.value.newChapterTitle.ifBlank { "第${_uiState.value.chapters.size + 1}章" }
        val chapter = Chapter(
            storyId = storyId,
            title = title,
            content = "",
            orderIndex = _uiState.value.chapters.size
        )

        viewModelScope.launch {
            val chapterId = chapterRepository.insertChapter(chapter)
            val newChapter = chapter.copy(id = chapterId)
            _uiState.update {
                it.copy(
                    currentChapter = newChapter,
                    content = "",
                    showAddChapterDialog = false,
                    newChapterTitle = ""
                )
            }
        }
    }

    private fun selectChapter(chapter: Chapter) {
        _uiState.update {
            it.copy(
                currentChapter = chapter,
                content = chapter.content,
                showChapterList = false
            )
        }
        loadDrafts(chapter.id)
    }

    private fun deleteChapter(chapterId: Long) {
        viewModelScope.launch {
            chapterRepository.deleteChapter(chapterId)
            if (_uiState.value.currentChapter?.id == chapterId) {
                _uiState.update {
                    it.copy(
                        currentChapter = it.chapters.firstOrNull { c -> c.id != chapterId },
                        content = "",
                        chapters = it.chapters.filter { c -> c.id != chapterId }
                    )
                }
            }
        }
    }

    private fun addCharacter() {
        val name = _uiState.value.newCharacterName
        val description = _uiState.value.newCharacterDescription
        if (name.isBlank()) return

        val character = Character(
            storyId = storyId,
            name = name,
            description = description
        )

        viewModelScope.launch {
            characterRepository.insertCharacter(character)
            _uiState.update {
                it.copy(
                    showAddCharacterDialog = false,
                    newCharacterName = "",
                    newCharacterDescription = ""
                )
            }
        }
    }

    private fun deleteCharacter(characterId: Long) {
        viewModelScope.launch {
            characterRepository.deleteCharacter(characterId)
        }
    }

    private fun addWorldSetting() {
        val name = _uiState.value.newWorldSettingName
        val content = _uiState.value.newWorldSettingContent
        if (name.isBlank()) return

        val setting = WorldSetting(
            storyId = storyId,
            name = name,
            content = content
        )

        viewModelScope.launch {
            worldSettingRepository.insertWorldSetting(setting)
            _uiState.update {
                it.copy(
                    showAddWorldSettingDialog = false,
                    newWorldSettingName = "",
                    newWorldSettingContent = ""
                )
            }
        }
    }

    private fun deleteWorldSetting(settingId: Long) {
        viewModelScope.launch {
            worldSettingRepository.deleteWorldSetting(settingId)
        }
    }

    private fun selectDraft(draft: ChapterDraft) {
        val updatedChapter = _uiState.value.currentChapter?.copy(content = draft.content)
        _uiState.update {
            it.copy(
                content = draft.content,
                currentChapter = updatedChapter,
                showDraftHistory = false
            )
        }
        saveContent()
    }

    private fun deleteDraft(draftId: Long) {
        viewModelScope.launch {
            chapterDraftRepository.deleteDraft(draftId)
        }
    }

    private fun restoreVersion(version: ChapterDraftVersion) {
        val updatedChapter = _uiState.value.currentChapter?.copy(content = version.content)
        _uiState.update {
            it.copy(
                content = version.content,
                currentChapter = updatedChapter,
                showDraftVersions = false,
                draftVersions = emptyList()
            )
        }
        saveContent()
    }

    private fun deleteVersion(versionId: Long) {
        viewModelScope.launch {
            // Versions are deleted through cascade from draft or directly
            // For now we don't have a direct delete, but drafts will clean up versions
            _uiState.update {
                it.copy(draftVersions = it.draftVersions.filter { v -> v.id != versionId })
            }
        }
    }
}
