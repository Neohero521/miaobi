package com.miaobi.app.ui.screens.writing

import com.miaobi.app.domain.model.*

/**
 * =================================══════════════════════════════════════
 * 扁平化 State - 参考 Claude Code Zustand 扁平化设计
 * =================================══════════════════════════════════════
 * 
 * Claude Code 的 Zustand Store 使用扁平化 State：
 * - 每个状态域独立，使用 Selector 按需获取
 * - 避免深层嵌套导致的不必要重组
 * - 易于测试和维护
 */

// ─── 内容编辑 State ──────────────────────────────────────────────────────────
data class ContentState(
    val content: String = "",
    val currentChapter: Chapter? = null,
    val undoStack: List<String> = emptyList(),
    val redoStack: List<String> = emptyList(),
    val maxUndoSize: Int = 50,
) {
    fun pushUndo(currentContent: String): ContentState {
        if (currentContent == content) return this
        return copy(
            undoStack = (undoStack + content).takeLast(maxUndoSize),
            redoStack = emptyList()
        )
    }
    
    fun undo(): ContentState {
        if (undoStack.isEmpty()) return this
        val previous = undoStack.last()
        return copy(
            content = previous,
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + content
        )
    }
    
    fun redo(): ContentState {
        if (redoStack.isEmpty()) return this
        val next = redoStack.last()
        return copy(
            content = next,
            undoStack = undoStack + content,
            redoStack = redoStack.dropLast(1)
        )
    }
    
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}

// ─── 编辑器 UI State ─────────────────────────────────────────────────────────
data class EditorUiState(
    val isImmersiveMode: Boolean = false,
    val showToolbar: Boolean = true,
    val isVoMode: Boolean = true,  // Voice / Original 模式切换
)

// ─── 故事数据 State ──────────────────────────────────────────────────────────
data class StoryDataState(
    val chapters: List<Chapter> = emptyList(),
    val characters: List<Character> = emptyList(),
    val worldSettings: List<WorldSetting> = emptyList(),
    val drafts: List<ChapterDraft> = emptyList(),
    val draftVersions: List<ChapterDraftVersion> = emptyList(),
    val selectedDraftId: Long? = null,
)

// ─── 抽屉/Sheet 显示 State ───────────────────────────────────────────────────
data class EditorSheetState(
    val showChapterList: Boolean = false,
    val showCharacterSheet: Boolean = false,
    val showWorldSettingSheet: Boolean = false,
    val showDraftHistory: Boolean = false,
    val showDraftVersions: Boolean = false,
    val showContinuationPanel: Boolean = false,
    val showRewriteSheet: Boolean = false,
    val showMultiBranchSheet: Boolean = false,
    val showInspirationSheet: Boolean = false,
)

// ─── 对话框 State ────────────────────────────────────────────────────────────
data class DialogState(
    val showAddChapterDialog: Boolean = false,
    val newChapterTitle: String = "",
    val showAddCharacterDialog: Boolean = false,
    val newCharacterName: String = "",
    val newCharacterDescription: String = "",
    val showAddWorldSettingDialog: Boolean = false,
    val newWorldSettingName: String = "",
    val newWorldSettingContent: String = "",
)

// ─── 统一 AI 任务状态机 ───────────────────────────────────────────────────────
/**
 * 参考 Claude Code AsyncGenerator Agent Loop 的状态机设计
 * 
 * Claude Code 的 AI 生成流程：
 * IDLE → AWAITING_INPUT → GENERATING → COMPLETED/ERROR
 * 
 * 我们借鉴这个模式，但针对 App 场景调整：
 * IDLE → GENERATING(流式累积) → COMPLETED(展示结果) / ERROR
 */
sealed class AiTaskState {
    /** 空闲状态，无进行中的任务 */
    object Idle : AiTaskState()
    
    /** 生成中，流式累积内容 */
    data class Generating(
        val accumulated: String = "",
        val stage: GenerationStage = GenerationStage.GENERATING,
    ) : AiTaskState()
    
    /** 生成完成，可以展示结果 */
    data class Completed(
        val result: String,
        val suggestions: List<ContinuationSuggestion> = emptyList(),
    ) : AiTaskState()
    
    /** 生成失败 */
    data class Error(val message: String) : AiTaskState()
    
    val isActive: Boolean get() = this is Generating
    val isIdle: Boolean get() = this is Idle
    val isCompleted: Boolean get() = this is Completed
    val isError: Boolean get() = this is Error
}

enum class GenerationStage {
    /** 准备上下文（组装 prompt） */
    PREPARING,
    
    /** 正在生成（流式接收中） */
    GENERATING,
    
    /** 解析结果（分割成多个建议） */
    PARSING,
    
    /** 完成（已保存到状态） */
    FINALIZING;
    
    val displayText: String get() = when (this) {
        PREPARING -> "准备中..."
        GENERATING -> "生成中..."
        PARSING -> "解析中..."
        FINALIZING -> "完成"
    }
}

/**
 * AI 续写专用状态
 * 组合了 AiTaskState 和续写特有的配置
 */
data class AiContinuationState(
    val taskState: AiTaskState = AiTaskState.Idle,
    val suggestions: List<ContinuationSuggestion> = emptyList(),
    val userPrompt: String = "",
    val lengthOption: LengthOption = LengthOption.MEDIUM,
) {
    val isGenerating: Boolean get() = taskState is AiTaskState.Generating
    val isEmpty: Boolean get() = taskState is AiTaskState.Idle && suggestions.isEmpty()
}

/**
 * RewriteState V2 - 使用 domain.model.RewriteVersion
 */
data class RewriteStateV2(
    val selectedText: String = "",
    val selectedStyle: RewriteStyle = RewriteStyle.MODERN,
    val versions: List<RewriteVersion> = emptyList(),
    val selectedVersionIndex: Int = 0,
    val isRewriting: Boolean = false,
    val isEditing: Boolean = false,
    val editingText: String = "",
    val error: String? = null,
) {
    val canRewrite: Boolean get() = selectedText.isNotBlank() && !isRewriting
    val selectedVersion: RewriteVersion? get() = versions.getOrNull(selectedVersionIndex)
    
    fun withClearedResult() = copy(
        versions = emptyList(),
        selectedVersionIndex = 0,
        isEditing = false,
        editingText = "",
        error = null
    )
}

/**
 * 多分支 State V2 - 使用 domain.model.BranchOption
 */
data class MultiBranchStateV2(
    val branchCount: Int = 3,
    val style: String = "标准",
    val length: Int = 1000,
    val branches: List<BranchOption> = emptyList(),
    val selectedBranchIndex: Int = 0,
    val isGenerating: Boolean = false,
    val error: String? = null,
) {
    val selectedBranch: BranchOption? get() = branches.getOrNull(selectedBranchIndex)
}

/**
 * 灵感 State V2 - 使用 domain.model.InspirationOption
 */
data class InspirationStateV2(
    val selectedType: InspirationType? = null,
    val options: List<InspirationOption> = emptyList(),
    val favorites: Set<Int> = emptySet(),
    val isGenerating: Boolean = false,
    val error: String? = null,
)
