package com.miaobi.app.ui.screens.writing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaobi.app.domain.model.*
import com.miaobi.app.domain.repository.*
import com.miaobi.app.domain.usecase.GenerateBranchesUseCase
import com.miaobi.app.domain.usecase.GenerateInspirationUseCase
import com.miaobi.app.domain.usecase.RewriteTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * =================================══════════════════════════════════════
 * WritingViewModel V2 - 参考 Claude Code 架构重构
 * =================================══════════════════════════════════════
 * 
 * 重构要点：
 * 1. State 扁平化 - 拆分为独立的状态域
 * 2. 统一 AI 任务状态机 - AiTaskState 状态追踪
 * 3. AiTaskManager - 统一 Job 管理
 * 4. Selector 模式 - 按需获取避免不必要重组
 * 
 * Claude Code 参考：
 * - Agent Loop: AsyncGenerator 流式模式
 * - State 管理: Zustand 扁平化
 * - Job 管理: trackWrite 串行化模式
 */

// ─── Event 定义（保持向后兼容）──────────────────────────────────────────────
sealed class WritingEventV2 {
    // Content
    data class UpdateContent(val content: String) : WritingEventV2()
    object SaveContent : WritingEventV2()
    
    // Editor UI
    object ToggleImmersiveMode : WritingEventV2()
    object ToggleToolbar : WritingEventV2()
    object ToggleVoMode : WritingEventV2()
    object Undo : WritingEventV2()
    object Redo : WritingEventV2()
    
    // AI Continuation
    object ToggleContinuationPanel : WritingEventV2()
    object GenerateContinuation : WritingEventV2()
    object CancelContinuation : WritingEventV2()
    data class UseContinuationSuggestion(val index: Int) : WritingEventV2()
    object RegenerateContinuation : WritingEventV2()
    data class UpdateUserPrompt(val prompt: String) : WritingEventV2()
    data class UpdateLengthOption(val option: LengthOption) : WritingEventV2()
    
    // Rewrite
    object ToggleRewriteStyleRow : WritingEventV2()
    object Polish : WritingEventV2()
    data class TriggerRewrite(val selectedText: String) : WritingEventV2()
    data class SelectRewriteStyle(val style: RewriteStyle) : WritingEventV2()
    object ExecuteRewrite : WritingEventV2()
    object CancelRewrite : WritingEventV2()
    data class SelectRewriteVersion(val index: Int) : WritingEventV2()
    object AcceptRewrite : WritingEventV2()
    object StartEditRewrite : WritingEventV2()
    data class UpdateEditingText(val text: String) : WritingEventV2()
    object ConfirmEditRewrite : WritingEventV2()
    object CancelEditRewrite : WritingEventV2()
    object RegenerateRewrite : WritingEventV2()
    object DismissRewrite : WritingEventV2()
    
    // Multi-Branch
    object ToggleMultiBranchSheet : WritingEventV2()
    data class UpdateBranchCount(val count: Int) : WritingEventV2()
    data class UpdateBranchStyle(val style: String) : WritingEventV2()
    data class UpdateBranchLength(val length: Int) : WritingEventV2()
    object GenerateBranches : WritingEventV2()
    object CancelBranches : WritingEventV2()
    data class SelectBranch(val index: Int) : WritingEventV2()
    object AcceptBranch : WritingEventV2()
    data class RegenerateBranch(val index: Int) : WritingEventV2()
    object DismissBranches : WritingEventV2()
    
    // Inspiration
    object ToggleInspirationSheet : WritingEventV2()
    data class FilterInspirationType(val type: InspirationType?) : WritingEventV2()
    object GenerateInspiration : WritingEventV2()
    object CancelInspiration : WritingEventV2()
    data class SelectInspirationOption(val index: Int) : WritingEventV2()
    data class AcceptInspiration(val option: InspirationOption) : WritingEventV2()
    data class ToggleInspirationFavorite(val index: Int) : WritingEventV2()
    object DismissInspiration : WritingEventV2()
    
    // Chapter
    object ToggleChapterList : WritingEventV2()
    object ShowAddChapterDialog : WritingEventV2()
    object HideAddChapterDialog : WritingEventV2()
    data class UpdateNewChapterTitle(val title: String) : WritingEventV2()
    object AddChapter : WritingEventV2()
    data class SelectChapter(val chapter: Chapter) : WritingEventV2()
    data class DeleteChapter(val chapterId: Long) : WritingEventV2()
    
    // Character
    object ToggleCharacterSheet : WritingEventV2()
    object ShowAddCharacterDialog : WritingEventV2()
    object HideAddCharacterDialog : WritingEventV2()
    data class UpdateNewCharacterName(val name: String) : WritingEventV2()
    data class UpdateNewCharacterDescription(val description: String) : WritingEventV2()
    object AddCharacter : WritingEventV2()
    data class DeleteCharacter(val characterId: Long) : WritingEventV2()
    
    // World Setting
    object ToggleWorldSettingSheet : WritingEventV2()
    object ShowAddWorldSettingDialog : WritingEventV2()
    object HideAddWorldSettingDialog : WritingEventV2()
    data class UpdateNewWorldSettingName(val name: String) : WritingEventV2()
    data class UpdateNewWorldSettingContent(val content: String) : WritingEventV2()
    object AddWorldSetting : WritingEventV2()
    data class DeleteWorldSetting(val settingId: Long) : WritingEventV2()
    
    // Draft
    object ToggleDraftHistory : WritingEventV2()
    data class SelectDraft(val draft: ChapterDraft) : WritingEventV2()
    data class DeleteDraft(val draftId: Long) : WritingEventV2()
    object ShowDraftVersions : WritingEventV2()
    object HideDraftVersions : WritingEventV2()
    data class SelectDraftForVersions(val draft: ChapterDraft) : WritingEventV2()
    data class RestoreVersion(val version: ChapterDraftVersion) : WritingEventV2()
    data class DeleteVersion(val versionId: Long) : WritingEventV2()
    
    // Error
    object ClearError : WritingEventV2()
    
    // Generated Content
    object AcceptGenerated : WritingEventV2()
    object DiscardGenerated : WritingEventV2()
}

// ─── 主 State ────────────────────────────────────────────────────────────────
data class WritingUiStateV2(
    // Story info (immutable after load)
    val story: Story? = null,
    val storyId: Long = -1L,
    
    // === 扁平化状态域 ===
    val contentState: ContentState = ContentState(),
    val editorUiState: EditorUiState = EditorUiState(),
    val storyDataState: StoryDataState = StoryDataState(),
    val sheetState: EditorSheetState = EditorSheetState(),
    val dialogState: DialogState = DialogState(),
    
    // === AI 任务状态 ===
    val continuationState: AiContinuationState = AiContinuationState(),
    val rewriteState: RewriteStateV2 = RewriteStateV2(),
    val multiBranchState: MultiBranchStateV2 = MultiBranchStateV2(),
    val inspirationState: InspirationStateV2 = InspirationStateV2(),
    
    // === 全局状态 ===
    val isLoading: Boolean = true,
    val error: String? = null,
    val generatedContent: String = "",  // 用于 Accept/Discard
)

// ─── Selectors（参考 Zustand Selector 模式）───────────────────────────────────
object WritingSelectors {
    /** 当前章节内容 */
    fun content(state: WritingUiStateV2) = state.contentState.content
    
    /** 是否可以撤销 */
    fun canUndo(state: WritingUiStateV2) = state.contentState.canUndo
    
    /** 是否可以重做 */
    fun canRedo(state: WritingUiStateV2) = state.contentState.canRedo
    
    /** 续写是否进行中 */
    fun isContinuationActive(state: WritingUiStateV2) = 
        state.continuationState.taskState is AiTaskState.Generating
    
    /** 改写是否进行中 */
    fun isRewriteActive(state: WritingUiStateV2) = state.rewriteState.isRewriting
    
    /** 多分支是否进行中 */
    fun isMultiBranchActive(state: WritingUiStateV2) = state.multiBranchState.isGenerating
    
    /** 灵感是否进行中 */
    fun isInspirationActive(state: WritingUiStateV2) = state.inspirationState.isGenerating
    
    /** 是否有任何 AI 任务进行中 */
    fun isAnyAiTaskActive(state: WritingUiStateV2) = 
        isContinuationActive(state) || isRewriteActive(state) || 
        isMultiBranchActive(state) || isInspirationActive(state)
    
    /** 当前章节 */
    fun currentChapter(state: WritingUiStateV2) = state.contentState.currentChapter
    
    /** 章节列表 */
    fun chapters(state: WritingUiStateV2) = state.storyDataState.chapters
    
    /** 角色列表 */
    fun characters(state: WritingUiStateV2) = state.storyDataState.characters
    
    /** 世界设定列表 */
    fun worldSettings(state: WritingUiStateV2) = state.storyDataState.worldSettings
}

// ─── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class WritingViewModelV2 @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storyRepository: StoryRepository,
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldSettingRepository: WorldSettingRepository,
    private val chapterDraftRepository: ChapterDraftRepository,
    private val aiRepository: AiRepository,
    private val rewriteTextUseCase: RewriteTextUseCase,
    private val generateBranchesUseCase: GenerateBranchesUseCase,
    private val generateInspirationUseCase: GenerateInspirationUseCase,
) : ViewModel() {
    
    private val storyId: Long = savedStateHandle.get<Long>("storyId") ?: -1L
    private val initialChapterId: Long = savedStateHandle.get<Long>("chapterId") ?: -1L
    
    // ─── 主状态流 ───────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(WritingUiStateV2(storyId = storyId))
    val uiState: StateFlow<WritingUiStateV2> = _uiState.asStateFlow()
    
    // ─── AI 任务管理器 ─────────────────────────────────────────────────────
    private val aiTaskManager = AiTaskManager(
        viewModelScope = viewModelScope,
        onStateUpdate = { type, state -> handleAiTaskStateUpdate(type, state) },
        onContinuationSuggestions = { suggestions -> 
            _uiState.update { it.copy(continuationState = it.continuationState.copy(suggestions = suggestions)) }
        }
    )
    
    // ─── 各类型 Job（简化管理）──────────────────────────────────────────────
    private var continuationJob: Job? = null
    private var rewriteJob: Job? = null
    private var multiBranchJob: Job? = null
    private var inspirationJob: Job? = null
    
    init {
        loadStory()
    }
    
    // ─── Event 处理（统一入口）──────────────────────────────────────────────
    fun onEvent(event: WritingEventV2) {
        when (event) {
            // Content
            is WritingEventV2.UpdateContent -> updateContent(event.content)
            WritingEventV2.SaveContent -> saveContent()
            WritingEventV2.AcceptGenerated -> acceptGenerated()
            WritingEventV2.DiscardGenerated -> discardGenerated()
            
            // Editor UI
            WritingEventV2.ToggleImmersiveMode -> toggleImmersiveMode()
            WritingEventV2.ToggleToolbar -> toggleToolbar()
            WritingEventV2.ToggleVoMode -> toggleVoMode()
            WritingEventV2.Undo -> undo()
            WritingEventV2.Redo -> redo()
            
            // Continuation
            WritingEventV2.ToggleContinuationPanel -> toggleContinuationPanel()
            WritingEventV2.GenerateContinuation -> generateContinuation()
            WritingEventV2.CancelContinuation -> cancelContinuation()
            is WritingEventV2.UseContinuationSuggestion -> useContinuationSuggestion(event.index)
            WritingEventV2.RegenerateContinuation -> regenerateContinuation()
            is WritingEventV2.UpdateUserPrompt -> updateUserPrompt(event.prompt)
            is WritingEventV2.UpdateLengthOption -> updateLengthOption(event.option)
            
            // Rewrite
            WritingEventV2.ToggleRewriteStyleRow -> toggleRewriteStyleRow()
            WritingEventV2.Polish -> polish()
            is WritingEventV2.TriggerRewrite -> triggerRewrite(event.selectedText)
            is WritingEventV2.SelectRewriteStyle -> selectRewriteStyle(event.style)
            WritingEventV2.ExecuteRewrite -> executeRewrite()
            WritingEventV2.CancelRewrite -> cancelRewrite()
            is WritingEventV2.SelectRewriteVersion -> selectRewriteVersion(event.index)
            WritingEventV2.AcceptRewrite -> acceptRewrite()
            WritingEventV2.StartEditRewrite -> startEditRewrite()
            is WritingEventV2.UpdateEditingText -> updateEditingText(event.text)
            WritingEventV2.ConfirmEditRewrite -> confirmEditRewrite()
            WritingEventV2.CancelEditRewrite -> cancelEditRewrite()
            WritingEventV2.RegenerateRewrite -> executeRewrite()
            WritingEventV2.DismissRewrite -> dismissRewrite()
            
            // Multi-Branch
            WritingEventV2.ToggleMultiBranchSheet -> toggleMultiBranchSheet()
            is WritingEventV2.UpdateBranchCount -> updateBranchCount(event.count)
            is WritingEventV2.UpdateBranchStyle -> updateBranchStyle(event.style)
            is WritingEventV2.UpdateBranchLength -> updateBranchLength(event.length)
            WritingEventV2.GenerateBranches -> generateBranches()
            WritingEventV2.CancelBranches -> cancelBranches()
            is WritingEventV2.SelectBranch -> selectBranch(event.index)
            WritingEventV2.AcceptBranch -> acceptBranch()
            is WritingEventV2.RegenerateBranch -> regenerateBranch(event.index)
            WritingEventV2.DismissBranches -> dismissBranches()
            
            // Inspiration
            WritingEventV2.ToggleInspirationSheet -> toggleInspirationSheet()
            is WritingEventV2.FilterInspirationType -> filterInspirationType(event.type)
            WritingEventV2.GenerateInspiration -> generateInspiration()
            WritingEventV2.CancelInspiration -> cancelInspiration()
            is WritingEventV2.SelectInspirationOption -> selectInspirationOption(event.index)
            is WritingEventV2.AcceptInspiration -> acceptInspiration(event.option)
            is WritingEventV2.ToggleInspirationFavorite -> toggleInspirationFavorite(event.index)
            WritingEventV2.DismissInspiration -> dismissInspiration()
            
            // Chapter
            WritingEventV2.ToggleChapterList -> toggleChapterList()
            WritingEventV2.ShowAddChapterDialog -> showAddChapterDialog()
            WritingEventV2.HideAddChapterDialog -> hideAddChapterDialog()
            is WritingEventV2.UpdateNewChapterTitle -> updateNewChapterTitle(event.title)
            WritingEventV2.AddChapter -> addChapter()
            is WritingEventV2.SelectChapter -> selectChapter(event.chapter)
            is WritingEventV2.DeleteChapter -> deleteChapter(event.chapterId)
            
            // Character
            WritingEventV2.ToggleCharacterSheet -> toggleCharacterSheet()
            WritingEventV2.ShowAddCharacterDialog -> showAddCharacterDialog()
            WritingEventV2.HideAddCharacterDialog -> hideAddCharacterDialog()
            is WritingEventV2.UpdateNewCharacterName -> updateNewCharacterName(event.name)
            is WritingEventV2.UpdateNewCharacterDescription -> updateNewCharacterDescription(event.description)
            WritingEventV2.AddCharacter -> addCharacter()
            is WritingEventV2.DeleteCharacter -> deleteCharacter(event.characterId)
            
            // World Setting
            WritingEventV2.ToggleWorldSettingSheet -> toggleWorldSettingSheet()
            WritingEventV2.ShowAddWorldSettingDialog -> showAddWorldSettingDialog()
            WritingEventV2.HideAddWorldSettingDialog -> hideAddWorldSettingDialog()
            is WritingEventV2.UpdateNewWorldSettingName -> updateNewWorldSettingName(event.name)
            is WritingEventV2.UpdateNewWorldSettingContent -> updateNewWorldSettingContent(event.content)
            WritingEventV2.AddWorldSetting -> addWorldSetting()
            is WritingEventV2.DeleteWorldSetting -> deleteWorldSetting(event.settingId)
            
            // Draft
            WritingEventV2.ToggleDraftHistory -> toggleDraftHistory()
            is WritingEventV2.SelectDraft -> selectDraft(event.draft)
            is WritingEventV2.DeleteDraft -> deleteDraft(event.draftId)
            WritingEventV2.ShowDraftVersions -> showDraftVersions()
            WritingEventV2.HideDraftVersions -> hideDraftVersions()
            is WritingEventV2.SelectDraftForVersions -> selectDraftForVersions(event.draft)
            is WritingEventV2.RestoreVersion -> restoreVersion(event.version)
            is WritingEventV2.DeleteVersion -> deleteVersion(event.versionId)
            
            // Error
            WritingEventV2.ClearError -> clearError()
        }
    }
    
    // ─── Content 操作 ───────────────────────────────────────────────────────
    private fun updateContent(newContent: String) {
        _uiState.update { state ->
            state.copy(
                contentState = state.contentState.pushUndo(state.contentState.content).copy(
                    content = newContent,
                    currentChapter = state.contentState.currentChapter?.copy(content = newContent)
                )
            )
        }
    }
    
    private fun saveContent() {
        val chapter = _uiState.value.contentState.currentChapter ?: return
        val content = _uiState.value.contentState.content
        
        viewModelScope.launch {
            chapterRepository.updateChapterContent(chapter.id, content)
        }
    }
    
    private fun acceptGenerated() {
        val newContent = _uiState.value.contentState.content + _uiState.value.generatedContent
        val updatedChapter = _uiState.value.contentState.currentChapter?.copy(content = newContent)
        
        _uiState.update { state ->
            state.copy(
                contentState = state.contentState.copy(content = newContent, currentChapter = updatedChapter),
                generatedContent = "",
                continuationState = state.continuationState.copy(userPrompt = "")
            )
        }
        saveContent()
    }
    
    private fun discardGenerated() {
        _uiState.update { it.copy(generatedContent = "") }
    }
    
    // ─── Editor UI 操作 ─────────────────────────────────────────────────────
    private fun toggleImmersiveMode() {
        _uiState.update { it.copy(editorUiState = it.editorUiState.copy(isImmersiveMode = !it.editorUiState.isImmersiveMode)) }
    }
    
    private fun toggleToolbar() {
        _uiState.update { it.copy(editorUiState = it.editorUiState.copy(showToolbar = !it.editorUiState.showToolbar)) }
    }
    
    private fun toggleVoMode() {
        _uiState.update { it.copy(editorUiState = it.editorUiState.copy(isVoMode = !it.editorUiState.isVoMode)) }
    }
    
    private fun undo() {
        _uiState.update { state ->
            val undone = state.contentState.undo()
            state.copy(
                contentState = undone.copy(currentChapter = undone.currentChapter?.copy(content = undone.content)),
            )
        }
    }
    
    private fun redo() {
        _uiState.update { state ->
            val redone = state.contentState.redo()
            state.copy(
                contentState = redone.copy(currentChapter = redone.currentChapter?.copy(content = redone.content)),
            )
        }
    }
    
    // ─── Continuation 操作 ──────────────────────────────────────────────────
    private fun toggleContinuationPanel() {
        _uiState.update { it.copy(sheetState = it.sheetState.copy(showContinuationPanel = !it.sheetState.showContinuationPanel)) }
    }
    
    private fun generateContinuation() {
        val chapter = _uiState.value.contentState.currentChapter ?: return
        
        val charactersText = _uiState.value.storyDataState.characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val worldSettingsText = _uiState.value.storyDataState.worldSettings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }
        
        val fullContent = _uiState.value.contentState.content.ifBlank { chapter.content }
        
        continuationJob?.cancel()
        continuationJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            
            _uiState.update {
                it.copy(
                    continuationState = it.continuationState.copy(
                        taskState = AiTaskState.Generating(),
                        suggestions = emptyList(),
                    ),
                    generatedContent = "",
                )
            }
            
            aiRepository.continueStory(
                prompt = _uiState.value.continuationState.userPrompt,
                characters = charactersText,
                worldSettings = worldSettingsText,
                historyContent = fullContent,
                lengthOption = _uiState.value.continuationState.lengthOption
            ).collect { response ->
                if (response.error != null) {
                    _uiState.update {
                        it.copy(continuationState = it.continuationState.copy(taskState = AiTaskState.Error(response.error)))
                    }
                } else if (response.isFinished) {
                    val suggestions = parseContinuationSuggestions(accumulated.toString())
                    _uiState.update {
                        it.copy(
                            continuationState = it.continuationState.copy(
                                taskState = AiTaskState.Completed(accumulated.toString(), suggestions),
                                suggestions = suggestions
                            ),
                            generatedContent = accumulated.toString()
                        )
                    }
                } else {
                    accumulated.append(response.content)
                    _uiState.update {
                        it.copy(
                            continuationState = it.continuationState.copy(
                                taskState = AiTaskState.Generating(accumulated.toString())
                            ),
                            generatedContent = accumulated.toString()
                        )
                    }
                }
            }
        }
    }
    
    private fun cancelContinuation() {
        continuationJob?.cancel()
        _uiState.update {
            it.copy(continuationState = it.continuationState.copy(taskState = AiTaskState.Idle))
        }
    }
    
    private fun useContinuationSuggestion(index: Int) {
        val suggestion = _uiState.value.continuationState.suggestions.getOrNull(index) ?: return
        val newContent = _uiState.value.contentState.content + suggestion.content
        val updatedChapter = _uiState.value.contentState.currentChapter?.copy(content = newContent)
        
        _uiState.update {
            it.copy(
                contentState = it.contentState.copy(content = newContent, currentChapter = updatedChapter),
                continuationState = it.continuationState.copy(suggestions = emptyList()),
                sheetState = it.sheetState.copy(showContinuationPanel = false)
            )
        }
        saveContent()
    }
    
    private fun regenerateContinuation() {
        generateContinuation()
    }
    
    private fun updateUserPrompt(prompt: String) {
        _uiState.update { it.copy(continuationState = it.continuationState.copy(userPrompt = prompt)) }
    }
    
    private fun updateLengthOption(option: LengthOption) {
        _uiState.update { it.copy(continuationState = it.continuationState.copy(lengthOption = option)) }
    }
    
    private fun parseContinuationSuggestions(content: String): List<ContinuationSuggestion> {
        if (content.isBlank()) return emptyList()
        
        val directionRegex = Regex("""===方向(\d+)===""")
        val parts = content.split(directionRegex)
        
        if (parts.size >= 4) {
            val directions = listOf("剧情升级", "情感互动", "意外转折")
            val suggestions = listOf(1, 2, 3).mapNotNull { index ->
                val partContent = parts.getOrNull(index)?.trim() ?: return@mapNotNull null
                if (partContent.isBlank()) return@mapNotNull null
                ContinuationSuggestion(
                    id = index - 1,
                    content = partContent,
                    wordCount = partContent.length,
                    isSelected = index == 1,
                    directionLabel = "方向$index：${directions[index - 1]}"
                )
            }
            if (suggestions.isNotEmpty()) return suggestions
        }
        
        val totalLength = content.length
        val partSize = totalLength / 3
        val directions = listOf("剧情升级", "情感互动", "意外转折")
        
        return listOf(0, 1, 2).map { index ->
            val start = index * partSize
            val end = if (index == 2) totalLength else (index + 1) * partSize
            val partContent = content.substring(start, end.coerceAtMost(totalLength)).trim()
            ContinuationSuggestion(
                id = index,
                content = partContent,
                wordCount = partContent.length,
                isSelected = index == 0,
                directionLabel = "方向${index + 1}：${directions[index]}"
            )
        }.filter { it.content.isNotBlank() }
    }
    
    // ─── Rewrite 操作 ───────────────────────────────────────────────────────
    private fun toggleRewriteStyleRow() {
        _uiState.update { 
            it.copy(sheetState = it.sheetState.copy(showRewriteSheet = !it.sheetState.showRewriteSheet))
        }
    }
    
    private fun polish() {
        val text = _uiState.value.contentState.content
        if (text.isNotBlank()) {
            triggerRewrite(text)
            executeRewrite()
        }
    }
    
    private fun triggerRewrite(selectedText: String) {
        _uiState.update {
            it.copy(rewriteState = it.rewriteState.copy(selectedText = selectedText).withClearedResult())
        }
    }
    
    private fun selectRewriteStyle(style: RewriteStyle) {
        _uiState.update {
            it.copy(rewriteState = it.rewriteState.copy(selectedStyle = style).withClearedResult())
        }
    }
    
    private fun executeRewrite() {
        val rewriteState = _uiState.value.rewriteState
        if (!rewriteState.canRewrite) return
        
        val charactersText = _uiState.value.storyDataState.characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val worldSettingsText = _uiState.value.storyDataState.worldSettings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }
        
        rewriteJob?.cancel()
        rewriteJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            
            _uiState.update {
                it.copy(rewriteState = it.rewriteState.copy(isRewriting = true, versions = emptyList()))
            }
            
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
                } else if (response.isFinished) {
                    val versions = parseRewriteVersions(accumulated.toString())
                    _uiState.update {
                        it.copy(rewriteState = it.rewriteState.copy(isRewriting = false, versions = versions))
                    }
                } else {
                    accumulated.append(response.content)
                }
            }
        }
    }
    
    private fun cancelRewrite() {
        rewriteJob?.cancel()
        _uiState.update {
            it.copy(rewriteState = it.rewriteState.copy(isRewriting = false))
        }
    }
    
    private fun selectRewriteVersion(index: Int) {
        _uiState.update {
            val updated = it.rewriteState.versions.mapIndexed { idx, v -> v.copy(isSelected = idx == index) }
            it.copy(rewriteState = it.rewriteState.copy(versions = updated, selectedVersionIndex = index))
        }
    }
    
    private fun acceptRewrite() {
        val rewriteState = _uiState.value.rewriteState
        val version = rewriteState.selectedVersion ?: return
        val originalText = rewriteState.selectedText
        val newContent = version.content
        
        val currentContent = _uiState.value.contentState.content
        val newFullContent = currentContent.replace(originalText, newContent)
        val updatedChapter = _uiState.value.contentState.currentChapter?.copy(content = newFullContent)
        
        _uiState.update {
            it.copy(
                contentState = it.contentState.copy(content = newFullContent, currentChapter = updatedChapter),
                rewriteState = RewriteStateV2()
            )
        }
        saveContent()
    }
    
    private fun startEditRewrite() {
        val version = _uiState.value.rewriteState.selectedVersion
        _uiState.update {
            it.copy(rewriteState = it.rewriteState.copy(isEditing = true, editingText = version?.content ?: ""))
        }
    }
    
    private fun updateEditingText(text: String) {
        _uiState.update { it.copy(rewriteState = it.rewriteState.copy(editingText = text)) }
    }
    
    private fun confirmEditRewrite() {
        val idx = _uiState.value.rewriteState.selectedVersionIndex
        _uiState.update {
            val updated = it.rewriteState.versions.mapIndexed { vidx, v ->
                if (vidx == idx) v.copy(content = it.rewriteState.editingText) else v
            }
            it.copy(rewriteState = it.rewriteState.copy(versions = updated, isEditing = false, editingText = ""))
        }
    }
    
    private fun cancelEditRewrite() {
        _uiState.update { it.copy(rewriteState = it.rewriteState.copy(isEditing = false, editingText = "")) }
    }
    
    private fun dismissRewrite() {
        rewriteJob?.cancel()
        _uiState.update { it.copy(rewriteState = RewriteStateV2()) }
    }
    
    private fun parseRewriteVersions(rawResponse: String): List<RewriteVersion> {
        val delimiter = "---版本分割线---"
        val parts = rawResponse.split(delimiter).map { it.trim() }.filter { it.isNotBlank() }
        return parts.take(3).mapIndexed { idx, content ->
            RewriteVersion(index = idx, content = content, isSelected = idx == 0)
        }
    }
    
    // ─── Multi-Branch 操作 ───────────────────────────────────────────────────
    private fun toggleMultiBranchSheet() {
        _uiState.update { it.copy(sheetState = it.sheetState.copy(showMultiBranchSheet = !it.sheetState.showMultiBranchSheet)) }
    }
    
    private fun updateBranchCount(count: Int) {
        _uiState.update { it.copy(multiBranchState = it.multiBranchState.copy(branchCount = count)) }
    }
    
    private fun updateBranchStyle(style: String) {
        _uiState.update { it.copy(multiBranchState = it.multiBranchState.copy(style = style)) }
    }
    
    private fun updateBranchLength(length: Int) {
        _uiState.update { it.copy(multiBranchState = it.multiBranchState.copy(length = length)) }
    }
    
    private fun generateBranches() {
        val chapter = _uiState.value.contentState.currentChapter ?: return
        
        val charactersText = _uiState.value.storyDataState.characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val worldSettingsText = _uiState.value.storyDataState.worldSettings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }
        
        multiBranchJob?.cancel()
        multiBranchJob = viewModelScope.launch {
            val fullContent = _uiState.value.contentState.content.ifBlank { chapter.content }
            
            _uiState.update {
                it.copy(
                    multiBranchState = it.multiBranchState.copy(
                        branches = (0 until it.multiBranchState.branchCount).map { i -> BranchOption(index = i) },
                        isGenerating = true
                    )
                )
            }
            
            generateBranchesUseCase(
                currentContent = fullContent,
                characters = charactersText,
                worldSettings = worldSettingsText,
                userInstruction = _uiState.value.continuationState.userPrompt.takeIf { p -> p.isNotBlank() },
                branchCount = _uiState.value.multiBranchState.branchCount,
                lengthOption = _uiState.value.continuationState.lengthOption
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
    
    private fun cancelBranches() {
        multiBranchJob?.cancel()
        _uiState.update {
            it.copy(multiBranchState = it.multiBranchState.copy(isGenerating = false))
        }
    }
    
    private fun selectBranch(index: Int) {
        _uiState.update {
            val updated = it.multiBranchState.branches.mapIndexed { idx, b -> b.copy(isSelected = idx == index) }
            it.copy(multiBranchState = it.multiBranchState.copy(branches = updated, selectedBranchIndex = index))
        }
    }
    
    private fun acceptBranch() {
        val selectedBranch = _uiState.value.multiBranchState.selectedBranch ?: return
        if (!selectedBranch.canAccept) return
        
        val newContent = _uiState.value.contentState.content + selectedBranch.content
        val updatedChapter = _uiState.value.contentState.currentChapter?.copy(content = newContent)
        
        _uiState.update {
            it.copy(
                contentState = it.contentState.copy(content = newContent, currentChapter = updatedChapter),
                sheetState = it.sheetState.copy(showMultiBranchSheet = false),
                multiBranchState = MultiBranchStateV2(
                    branchCount = it.multiBranchState.branchCount,
                    style = it.multiBranchState.style,
                    length = it.multiBranchState.length
                )
            )
        }
        saveContent()
    }
    
    private fun regenerateBranch(branchIndex: Int) {
        val chapter = _uiState.value.contentState.currentChapter ?: return
        
        val charactersText = _uiState.value.storyDataState.characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val worldSettingsText = _uiState.value.storyDataState.worldSettings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }
        
        viewModelScope.launch {
            val updatedBranches = _uiState.value.multiBranchState.branches.map {
                if (it.index == branchIndex) it.copy(isGenerating = true, content = "", error = null) else it
            }
            _uiState.update { it.copy(multiBranchState = it.multiBranchState.copy(branches = updatedBranches)) }
            
            val fullContent = _uiState.value.contentState.content.ifBlank { chapter.content }
            
            generateBranchesUseCase(
                currentContent = fullContent,
                characters = charactersText,
                worldSettings = worldSettingsText,
                userInstruction = _uiState.value.continuationState.userPrompt.takeIf { p -> p.isNotBlank() },
                branchCount = _uiState.value.multiBranchState.branchCount,
                lengthOption = _uiState.value.continuationState.lengthOption
            ).collect { branches ->
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
    
    private fun dismissBranches() {
        multiBranchJob?.cancel()
        _uiState.update {
            it.copy(
                sheetState = it.sheetState.copy(showMultiBranchSheet = false),
                multiBranchState = MultiBranchStateV2()
            )
        }
    }
    
    // ─── Inspiration 操作 ───────────────────────────────────────────────────
    private fun toggleInspirationSheet() {
        _uiState.update { it.copy(sheetState = it.sheetState.copy(showInspirationSheet = !it.sheetState.showInspirationSheet)) }
    }
    
    private fun filterInspirationType(type: InspirationType?) {
        _uiState.update { it.copy(inspirationState = it.inspirationState.copy(selectedType = type)) }
    }
    
    private fun generateInspiration() {
        val chapter = _uiState.value.contentState.currentChapter ?: return
        
        val charactersText = _uiState.value.storyDataState.characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val worldSettingsText = _uiState.value.storyDataState.worldSettings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }
        
        inspirationJob?.cancel()
        inspirationJob = viewModelScope.launch {
            val fullContent = _uiState.value.contentState.content.ifBlank { chapter.content }
            
            _uiState.update {
                it.copy(inspirationState = it.inspirationState.copy(options = emptyList(), isGenerating = true))
            }
            
            generateInspirationUseCase(
                currentContent = fullContent,
                characters = charactersText,
                worldSettings = worldSettingsText,
                count = 4,
                typeFilter = _uiState.value.inspirationState.selectedType
            ).collect { options ->
                _uiState.update { it.copy(inspirationState = it.inspirationState.copy(options = options)) }
            }
            
            _uiState.update {
                it.copy(inspirationState = it.inspirationState.copy(isGenerating = false))
            }
        }
    }
    
    private fun cancelInspiration() {
        inspirationJob?.cancel()
        _uiState.update {
            it.copy(inspirationState = it.inspirationState.copy(isGenerating = false))
        }
    }
    
    private fun selectInspirationOption(index: Int) {
        _uiState.update {
            val updated = it.inspirationState.options.mapIndexed { idx, opt -> opt.copy(isSelected = idx == index) }
            it.copy(inspirationState = it.inspirationState.copy(options = updated))
        }
    }
    
    private fun acceptInspiration(option: InspirationOption) {
        if (!option.canAccept) return
        
        val newContent = _uiState.value.contentState.content + "\n\n" + option.content
        val updatedChapter = _uiState.value.contentState.currentChapter?.copy(content = newContent)
        
        _uiState.update {
            it.copy(
                contentState = it.contentState.copy(content = newContent, currentChapter = updatedChapter),
                sheetState = it.sheetState.copy(showInspirationSheet = false),
                inspirationState = InspirationStateV2()
            )
        }
        saveContent()
    }
    
    private fun toggleInspirationFavorite(index: Int) {
        _uiState.update {
            val favorites = it.inspirationState.favorites.toMutableSet()
            if (favorites.contains(index)) favorites.remove(index) else favorites.add(index)
            it.copy(inspirationState = it.inspirationState.copy(favorites = favorites))
        }
    }
    
    private fun dismissInspiration() {
        inspirationJob?.cancel()
        _uiState.update {
            it.copy(
                sheetState = it.sheetState.copy(showInspirationSheet = false),
                inspirationState = InspirationStateV2()
            )
        }
    }
    
    // ─── 数据加载 ───────────────────────────────────────────────────────────
    private fun loadStory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            storyRepository.getStoryById(storyId).collect { story ->
                _uiState.update { it.copy(story = story) }
            }
        }
        
        viewModelScope.launch {
            chapterRepository.getChaptersByStoryId(storyId).collect { chapters ->
                _uiState.update { state ->
                    val selectedChapter = when {
                        initialChapterId > 0 -> chapters.find { it.id == initialChapterId }
                        state.contentState.currentChapter == null && chapters.isNotEmpty() -> chapters.first()
                        else -> state.contentState.currentChapter
                    }
                    state.copy(
                        storyDataState = state.storyDataState.copy(chapters = chapters),
                        contentState = state.contentState.copy(
                            currentChapter = selectedChapter,
                            content = selectedChapter?.content ?: ""
                        ),
                        isLoading = false
                    )
                }
                _uiState.value.contentState.currentChapter?.let { loadDrafts(it.id) }
            }
        }
        
        viewModelScope.launch {
            characterRepository.getCharactersByStoryId(storyId).collect { characters ->
                _uiState.update { it.copy(storyDataState = it.storyDataState.copy(characters = characters)) }
            }
        }
        
        viewModelScope.launch {
            worldSettingRepository.getWorldSettingsByStoryId(storyId).collect { settings ->
                _uiState.update { it.copy(storyDataState = it.storyDataState.copy(worldSettings = settings)) }
            }
        }
    }
    
    private fun loadDrafts(chapterId: Long) {
        viewModelScope.launch {
            chapterDraftRepository.getDraftsByChapterId(chapterId).collect { drafts ->
                _uiState.update { it.copy(storyDataState = it.storyDataState.copy(drafts = drafts)) }
            }
        }
    }
    
    private fun loadDraftVersions(draftId: Long) {
        viewModelScope.launch {
            chapterDraftRepository.getVersionsByDraftId(draftId).collect { versions ->
                _uiState.update {
                    it.copy(storyDataState = it.storyDataState.copy(draftVersions = versions))
                }
            }
        }
    }
    
    // ─── Sheet/Dialog 操作 ─────────────────────────────────────────────────
    private fun toggleChapterList() {
        _uiState.update { it.copy(sheetState = it.sheetState.copy(showChapterList = !it.sheetState.showChapterList)) }
    }
    
    private fun toggleCharacterSheet() {
        _uiState.update { it.copy(sheetState = it.sheetState.copy(showCharacterSheet = !it.sheetState.showCharacterSheet)) }
    }
    
    private fun toggleWorldSettingSheet() {
        _uiState.update { it.copy(sheetState = it.sheetState.copy(showWorldSettingSheet = !it.sheetState.showWorldSettingSheet)) }
    }
    
    private fun toggleDraftHistory() {
        _uiState.update { it.copy(sheetState = it.sheetState.copy(showDraftHistory = !it.sheetState.showDraftHistory)) }
    }
    
    private fun showDraftVersions() {
        _uiState.update { it.copy(sheetState = it.sheetState.copy(showDraftVersions = true)) }
    }
    
    private fun hideDraftVersions() {
        _uiState.update {
            it.copy(
                sheetState = it.sheetState.copy(showDraftVersions = false),
                storyDataState = it.storyDataState.copy(draftVersions = emptyList())
            )
        }
    }
    
    private fun showAddChapterDialog() {
        _uiState.update {
            it.copy(dialogState = it.dialogState.copy(showAddChapterDialog = true, newChapterTitle = ""))
        }
    }
    
    private fun hideAddChapterDialog() {
        _uiState.update {
            it.copy(dialogState = it.dialogState.copy(showAddChapterDialog = false, newChapterTitle = ""))
        }
    }
    
    private fun updateNewChapterTitle(title: String) {
        _uiState.update { it.copy(dialogState = it.dialogState.copy(newChapterTitle = title)) }
    }
    
    private fun showAddCharacterDialog() {
        _uiState.update {
            it.copy(dialogState = it.dialogState.copy(
                showAddCharacterDialog = true,
                newCharacterName = "",
                newCharacterDescription = ""
            ))
        }
    }
    
    private fun hideAddCharacterDialog() {
        _uiState.update { it.copy(dialogState = it.dialogState.copy(showAddCharacterDialog = false)) }
    }
    
    private fun updateNewCharacterName(name: String) {
        _uiState.update { it.copy(dialogState = it.dialogState.copy(newCharacterName = name)) }
    }
    
    private fun updateNewCharacterDescription(description: String) {
        _uiState.update { it.copy(dialogState = it.dialogState.copy(newCharacterDescription = description)) }
    }
    
    private fun showAddWorldSettingDialog() {
        _uiState.update {
            it.copy(dialogState = it.dialogState.copy(
                showAddWorldSettingDialog = true,
                newWorldSettingName = "",
                newWorldSettingContent = ""
            ))
        }
    }
    
    private fun hideAddWorldSettingDialog() {
        _uiState.update { it.copy(dialogState = it.dialogState.copy(showAddWorldSettingDialog = false)) }
    }
    
    private fun updateNewWorldSettingName(name: String) {
        _uiState.update { it.copy(dialogState = it.dialogState.copy(newWorldSettingName = name)) }
    }
    
    private fun updateNewWorldSettingContent(content: String) {
        _uiState.update { it.copy(dialogState = it.dialogState.copy(newWorldSettingContent = content)) }
    }
    
    // ─── CRUD 操作 ─────────────────────────────────────────────────────────
    private fun addChapter() {
        val title = _uiState.value.dialogState.newChapterTitle.ifBlank {
            "第${_uiState.value.storyDataState.chapters.size + 1}章"
        }
        val chapter = Chapter(
            storyId = storyId,
            title = title,
            content = "",
            orderIndex = _uiState.value.storyDataState.chapters.size
        )
        
        viewModelScope.launch {
            val chapterId = chapterRepository.insertChapter(chapter)
            val newChapter = chapter.copy(id = chapterId)
            _uiState.update {
                it.copy(
                    contentState = it.contentState.copy(currentChapter = newChapter, content = ""),
                    dialogState = it.dialogState.copy(showAddChapterDialog = false, newChapterTitle = "")
                )
            }
        }
    }
    
    private fun selectChapter(chapter: Chapter) {
        _uiState.update {
            it.copy(
                contentState = it.contentState.copy(currentChapter = chapter, content = chapter.content),
                sheetState = it.sheetState.copy(showChapterList = false)
            )
        }
        loadDrafts(chapter.id)
    }
    
    private fun deleteChapter(chapterId: Long) {
        viewModelScope.launch {
            chapterRepository.deleteChapter(chapterId)
            if (_uiState.value.contentState.currentChapter?.id == chapterId) {
                _uiState.update {
                    it.copy(
                        contentState = it.contentState.copy(
                            currentChapter = it.storyDataState.chapters.firstOrNull { c -> c.id != chapterId },
                            content = ""
                        ),
                        storyDataState = it.storyDataState.copy(
                            chapters = it.storyDataState.chapters.filter { c -> c.id != chapterId }
                        )
                    )
                }
            }
        }
    }
    
    private fun addCharacter() {
        val name = _uiState.value.dialogState.newCharacterName
        val description = _uiState.value.dialogState.newCharacterDescription
        if (name.isBlank()) return
        
        val character = Character(storyId = storyId, name = name, description = description)
        
        viewModelScope.launch {
            characterRepository.insertCharacter(character)
            _uiState.update {
                it.copy(dialogState = it.dialogState.copy(showAddCharacterDialog = false))
            }
        }
    }
    
    private fun deleteCharacter(characterId: Long) {
        viewModelScope.launch { characterRepository.deleteCharacter(characterId) }
    }
    
    private fun addWorldSetting() {
        val name = _uiState.value.dialogState.newWorldSettingName
        val content = _uiState.value.dialogState.newWorldSettingContent
        if (name.isBlank()) return
        
        val setting = WorldSetting(storyId = storyId, name = name, content = content)
        
        viewModelScope.launch {
            worldSettingRepository.insertWorldSetting(setting)
            _uiState.update {
                it.copy(dialogState = it.dialogState.copy(showAddWorldSettingDialog = false))
            }
        }
    }
    
    private fun deleteWorldSetting(settingId: Long) {
        viewModelScope.launch { worldSettingRepository.deleteWorldSetting(settingId) }
    }
    
    private fun selectDraft(draft: ChapterDraft) {
        val updatedChapter = _uiState.value.contentState.currentChapter?.copy(content = draft.content)
        _uiState.update {
            it.copy(
                contentState = it.contentState.copy(content = draft.content, currentChapter = updatedChapter),
                sheetState = it.sheetState.copy(showDraftHistory = false)
            )
        }
        saveContent()
    }
    
    private fun deleteDraft(draftId: Long) {
        viewModelScope.launch { chapterDraftRepository.deleteDraft(draftId) }
    }
    
    private fun selectDraftForVersions(draft: ChapterDraft) {
        _uiState.update {
            it.copy(
                storyDataState = it.storyDataState.copy(selectedDraftId = draft.id),
                sheetState = it.sheetState.copy(showDraftVersions = true)
            )
        }
        loadDraftVersions(draft.id)
    }
    
    private fun restoreVersion(version: ChapterDraftVersion) {
        val updatedChapter = _uiState.value.contentState.currentChapter?.copy(content = version.content)
        _uiState.update {
            it.copy(
                contentState = it.contentState.copy(content = version.content, currentChapter = updatedChapter),
                sheetState = it.sheetState.copy(showDraftVersions = false),
                storyDataState = it.storyDataState.copy(draftVersions = emptyList())
            )
        }
        saveContent()
    }
    
    private fun deleteVersion(versionId: Long) {
        _uiState.update {
            it.copy(storyDataState = it.storyDataState.copy(
                draftVersions = it.storyDataState.draftVersions.filter { v -> v.id != versionId }
            ))
        }
    }
    
    // ─── Error 操作 ─────────────────────────────────────────────────────────
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    // ─── AI 任务状态更新 ────────────────────────────────────────────────────
    private fun handleAiTaskStateUpdate(type: AiTaskType, state: AiTaskState) {
        when (type) {
            AiTaskType.CONTINUATION -> {
                _uiState.update { it.copy(continuationState = it.continuationState.copy(taskState = state)) }
            }
            AiTaskType.REWRITE -> {
                // Rewrite 有自己的状态管理
            }
            AiTaskType.MULTI_BRANCH -> {
                // Multi-Branch 有自己的状态管理
            }
            AiTaskType.INSPIRATION -> {
                // Inspiration 有自己的状态管理
            }
        }
    }
    
    // ─── 辅助属性 ───────────────────────────────────────────────────────────
    private val currentChapter get() = _uiState.value.contentState.currentChapter
    private val content get() = _uiState.value.contentState.content
}
