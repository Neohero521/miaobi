package com.miaobi.app.ui.screens.writing

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.miaobi.app.domain.model.Chapter
import com.miaobi.app.domain.model.ChapterDraft
import com.miaobi.app.domain.model.ChapterDraftVersion
import com.miaobi.app.domain.model.Character
import com.miaobi.app.domain.model.WorldSetting
import com.miaobi.app.ui.components.ChapterDrawer
import com.miaobi.app.ui.components.SaveStatusIndicator
import com.miaobi.app.ui.components.TextSelectionToolbar
import com.miaobi.app.ui.components.TypewriterGeneratedText
import com.miaobi.app.ui.components.TypingIndicator
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    viewModel: WritingViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ── Sheet states ──────────────────────────────────────────────────────────
    val rewriteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val multiBranchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val inspirationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val continuationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val showRewriteSheet = uiState.rewriteState.selectedText.isNotBlank()

    // ── Text selection toolbar state ──────────────────────────────────────────
    var selectionToolbarVisible by remember { mutableStateOf(false) }
    var selectedTextForToolbar by remember { mutableStateOf("") }

    // ── AI 菜单状态 ────────────────────────────────────────────────────────
    var showAiMenu by remember { mutableStateOf(false) }
    var selectedWriteType by remember { mutableStateOf(WriteType.CONTINUE) }

    // ── Auto-save timer (every 30 seconds) ────────────────────────────────────
    var lastAutoSaveTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isSaving by remember { mutableStateOf(false) }
    var lastSaveSuccess by remember { mutableStateOf(true) }
    var pendingSaveContent by remember { mutableStateOf<String?>(null) }

    // 防抖自动保存：内容变化后 3 秒无变化才保存
    LaunchedEffect(uiState.content) {
        pendingSaveContent = uiState.content
        kotlinx.coroutines.delay(3000) // 3秒防抖
        if (pendingSaveContent == uiState.content && uiState.content.isNotBlank()) {
            val now = System.currentTimeMillis()
            if (now - lastAutoSaveTime >= 30_000L) {
                lastAutoSaveTime = now
                isSaving = true
                viewModel.onEvent(WritingEvent.SaveContent)
                kotlinx.coroutines.delay(1500)
                isSaving = false
            }
        }
    }

    // ── Continuation panel ────────────────────────────────────────────────────
    if (uiState.showContinuationPanel) {
        AiContinuationPanel(
            suggestions = uiState.continuationSuggestions,
            isGenerating = uiState.isGenerating,
            onUse = { viewModel.onEvent(WritingEvent.UseContinuationSuggestion(it)) },
            onRegenerate = { viewModel.onEvent(WritingEvent.RegenerateSuggestions) },
            onDismiss = { viewModel.onEvent(WritingEvent.ToggleContinuationPanel) },
            sheetState = continuationSheetState
        )
    }

    Scaffold(
        topBar = {
            WritingTopBar(
                storyTitle = uiState.story?.title ?: "写小说",
                chapterTitle = uiState.currentChapter?.title,
                isImmersiveMode = uiState.isImmersiveMode,
                isVoMode = uiState.isVoMode,
                onBack = onBack,
                onToggleImmersive = { viewModel.onEvent(WritingEvent.ToggleImmersiveMode) },
                onToggleVoMode = { viewModel.onEvent(WritingEvent.ToggleVoMode) },
                onChapterList = { viewModel.onEvent(WritingEvent.ToggleChapterList) },
                onCharacter = { viewModel.onEvent(WritingEvent.ToggleCharacterSheet) },
                onWorldSetting = { viewModel.onEvent(WritingEvent.ToggleWorldSettingSheet) },
                onInspiration = { viewModel.onEvent(WritingEvent.ToggleInspirationSheet) },
                onMultiBranch = { viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet) },
                onExport = {
                    uiState.currentChapter?.let { chapter ->
                        exportToTxt(context, chapter.title, uiState.content)
                    }
                },
                wordCount = uiState.content.length,
                isSaving = isSaving,
                lastSaveTime = uiState.currentChapter?.updatedAt,
                hasUnsavedChanges = uiState.content.isNotBlank(),
                saveError = uiState.error,
                lengthOption = uiState.lengthOption,
                onLengthOptionChange = { viewModel.onEvent(WritingEvent.UpdateLengthOption(it)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Rewrite style tab (slides in when needed) ───────────────────────
            AnimatedVisibility(
                visible = uiState.showRewriteStyleRow,
                enter = slideInVertically(tween(200)) + fadeIn(tween(200)),
                exit = slideOutVertically(tween(200)) + fadeOut(tween(200))
            ) {
                RewriteStyleTabRow(
                    selectedStyle = uiState.selectedStyle,
                    onStyleSelected = { viewModel.onEvent(WritingEvent.SelectStyle(it)) },
                    enabled = !uiState.rewriteState.isRewriting
                )
            }

            // ── Main writing area ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
            ) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    uiState.currentChapter == null -> NoChapterView(
                        onAddChapter = { viewModel.onEvent(WritingEvent.ShowAddChapterDialog) }
                    )
                    else -> WritingContent(
                        content = uiState.content,
                        onContentChange = { viewModel.onEvent(WritingEvent.UpdateContent(it)) },
                        onTextSelected = { selected ->
                            if (selected.isNotBlank()) {
                                selectedTextForToolbar = selected
                                selectionToolbarVisible = true
                            }
                        }
                    )
                }

                // ── Text Selection Floating Toolbar ───────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextSelectionToolbar(
                        visible = selectionToolbarVisible,
                        selectedText = selectedTextForToolbar,
                        onRewrite = {
                            viewModel.onEvent(WritingEvent.TriggerRewrite(selectedTextForToolbar))
                            viewModel.onEvent(WritingEvent.ToggleRewriteStyleRow)
                            selectionToolbarVisible = false
                        },
                        onPolish = {
                            viewModel.onEvent(WritingEvent.Polish)
                            selectionToolbarVisible = false
                        },
                        onDismiss = { selectionToolbarVisible = false }
                    )
                }

                // ── 彩云小梦风格底部栏 ─────────────────────────────────────────
                BottomInputBar(
                    isGenerating = uiState.isGenerating,
                    prompt = uiState.userPrompt,
                    onPromptChange = { viewModel.onEvent(WritingEvent.UpdatePrompt(it)) },
                    onGenerate = {
                        if (uiState.isGenerating) {
                            viewModel.onEvent(WritingEvent.CancelGeneration)
                        } else {
                            viewModel.onEvent(WritingEvent.GenerateContinue)
                        }
                    },
                    onExpandClick = { showAiMenu = !showAiMenu },
                    isExpanded = showAiMenu,
                    onDismissExpand = { showAiMenu = false },
                    showRewriteStyleRow = uiState.showRewriteStyleRow,
                    selectedStyle = uiState.selectedStyle,
                    onRewrite = {
                        if (uiState.content.isNotBlank()) {
                            viewModel.onEvent(WritingEvent.TriggerRewrite(uiState.content))
                            viewModel.onEvent(WritingEvent.ToggleRewriteStyleRow)
                        }
                        showAiMenu = false
                    },
                    onExpand = {
                        viewModel.onEvent(WritingEvent.UpdatePrompt("扩写："))
                        viewModel.onEvent(WritingEvent.GenerateContinue)
                    },
                    onShrink = {
                        viewModel.onEvent(WritingEvent.UpdatePrompt("缩写："))
                        viewModel.onEvent(WritingEvent.GenerateContinue)
                    },
                    selectedWriteType = selectedWriteType,
                    onWriteTypeSelected = { selectedWriteType = it },
                    selectedWriteStyle = uiState.selectedWriteStyle,
                    onWriteStyleSelected = { viewModel.onEvent(WritingEvent.SelectWriteStyle(it)) },
                    canUndo = uiState.undoStack.isNotEmpty(),
                    canRedo = uiState.redoStack.isNotEmpty(),
                    onUndo = { viewModel.onEvent(WritingEvent.Undo) },
                    onRedo = { viewModel.onEvent(WritingEvent.Redo) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // ── Error Toast ───────────────────────────────────────────────────────────
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.onEvent(WritingEvent.ClearError)
        }
    }

    // ── All Bottom Sheets ─────────────────────────────────────────────────────
    // ── Chapter Drawer (left-side) ─────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        ChapterDrawer(
            isOpen = uiState.showChapterList,
            chapters = uiState.chapters,
            currentChapter = uiState.currentChapter,
            onSelectChapter = { chapter ->
                viewModel.onEvent(WritingEvent.SelectChapter(chapter))
                viewModel.onEvent(WritingEvent.ToggleChapterList)
            },
            onDeleteChapter = { viewModel.onEvent(WritingEvent.DeleteChapter(it)) },
            onAddChapter = { viewModel.onEvent(WritingEvent.ShowAddChapterDialog) },
            onDismiss = { viewModel.onEvent(WritingEvent.ToggleChapterList) },
            modifier = Modifier.align(Alignment.TopStart)
        )
    }

    if (uiState.showCharacterSheet) CharacterSheet(
        characters = uiState.characters,
        onAddCharacter = { viewModel.onEvent(WritingEvent.ShowAddCharacterDialog) },
        onDeleteCharacter = { viewModel.onEvent(WritingEvent.DeleteCharacter(it)) },
        onDismiss = { viewModel.onEvent(WritingEvent.ToggleCharacterSheet) }
    )

    if (uiState.showWorldSettingSheet) WorldSettingSheet(
        worldSettings = uiState.worldSettings,
        onAddWorldSetting = { viewModel.onEvent(WritingEvent.ShowAddWorldSettingDialog) },
        onDeleteWorldSetting = { viewModel.onEvent(WritingEvent.DeleteWorldSetting(it)) },
        onDismiss = { viewModel.onEvent(WritingEvent.ToggleWorldSettingSheet) }
    )

    if (uiState.showDraftHistory) DraftHistorySheet(
        drafts = uiState.drafts,
        onSelectDraft = { viewModel.onEvent(WritingEvent.SelectDraft(it)) },
        onDeleteDraft = { viewModel.onEvent(WritingEvent.DeleteDraft(it)) },
        onViewVersions = { viewModel.onEvent(WritingEvent.SelectDraftForVersions(it)) },
        onDismiss = { viewModel.onEvent(WritingEvent.ToggleDraftHistory) }
    )

    if (uiState.showDraftVersions) DraftVersionsSheet(
        versions = uiState.draftVersions,
        onRestoreVersion = { viewModel.onEvent(WritingEvent.RestoreVersion(it)) },
        onDeleteVersion = { viewModel.onEvent(WritingEvent.DeleteVersion(it)) },
        onDismiss = { viewModel.onEvent(WritingEvent.HideDraftVersions) }
    )

    if (uiState.showAddChapterDialog) AddChapterDialog(
        title = uiState.newChapterTitle,
        onTitleChange = { viewModel.onEvent(WritingEvent.UpdateNewChapterTitle(it)) },
        onConfirm = { viewModel.onEvent(WritingEvent.AddChapter) },
        onDismiss = { viewModel.onEvent(WritingEvent.HideAddChapterDialog) }
    )

    if (uiState.showAddCharacterDialog) AddCharacterDialog(
        name = uiState.newCharacterName, description = uiState.newCharacterDescription,
        onNameChange = { viewModel.onEvent(WritingEvent.UpdateNewCharacterName(it)) },
        onDescriptionChange = { viewModel.onEvent(WritingEvent.UpdateNewCharacterDescription(it)) },
        onConfirm = { viewModel.onEvent(WritingEvent.AddCharacter) },
        onDismiss = { viewModel.onEvent(WritingEvent.HideAddCharacterDialog) }
    )

    if (uiState.showAddWorldSettingDialog) AddWorldSettingDialog(
        name = uiState.newWorldSettingName, content = uiState.newWorldSettingContent,
        onNameChange = { viewModel.onEvent(WritingEvent.UpdateNewWorldSettingName(it)) },
        onContentChange = { viewModel.onEvent(WritingEvent.UpdateNewWorldSettingContent(it)) },
        onConfirm = { viewModel.onEvent(WritingEvent.AddWorldSetting) },
        onDismiss = { viewModel.onEvent(WritingEvent.HideAddWorldSettingDialog) }
    )

    if (showRewriteSheet) RewriteBottomSheet(
        rewriteState = uiState.rewriteState,
        onStyleSelected = { viewModel.onEvent(WritingEvent.SelectRewriteStyle(it)) },
        onRewrite = { viewModel.onEvent(WritingEvent.ExecuteRewrite) },
        onCancel = { viewModel.onEvent(WritingEvent.CancelRewrite) },
        onVersionSelected = { viewModel.onEvent(WritingEvent.SelectRewriteVersion(it)) },
        onAccept = { viewModel.onEvent(WritingEvent.AcceptRewrite) },
        onEdit = { viewModel.onEvent(WritingEvent.StartEditRewrite) },
        onUpdateEditing = { viewModel.onEvent(WritingEvent.UpdateEditingText(it)) },
        onConfirmEdit = { viewModel.onEvent(WritingEvent.ConfirmEditRewrite) },
        onCancelEdit = { viewModel.onEvent(WritingEvent.CancelEditRewrite) },
        onRegenerate = { viewModel.onEvent(WritingEvent.RegenerateRewrite) },
        onDismiss = {
            viewModel.onEvent(WritingEvent.DismissRewrite)
            viewModel.onEvent(WritingEvent.ToggleRewriteStyleRow)
        },
        sheetState = rewriteSheetState
    )

    if (uiState.showMultiBranchSheet) MultiBranchBottomSheet(
        multiBranchState = uiState.multiBranchState,
        onCountChanged = { viewModel.onEvent(WritingEvent.UpdateBranchCount(it)) },
        onStyleChanged = { viewModel.onEvent(WritingEvent.UpdateBranchStyle(it)) },
        onLengthChanged = { viewModel.onEvent(WritingEvent.UpdateBranchLength(it)) },
        onGenerate = { viewModel.onEvent(WritingEvent.GenerateBranches) },
        onCancel = { viewModel.onEvent(WritingEvent.CancelBranches) },
        onBranchSelected = { viewModel.onEvent(WritingEvent.SelectBranch(it)) },
        onAccept = { viewModel.onEvent(WritingEvent.AcceptBranch) },
        onRegenerateBranch = { viewModel.onEvent(WritingEvent.RegenerateBranch(it)) },
        onDismiss = { viewModel.onEvent(WritingEvent.DismissBranches) },
        sheetState = multiBranchSheetState
    )

    if (uiState.showInspirationSheet) InspirationBottomSheet(
        inspirationState = uiState.inspirationState,
        onTypeFilterChanged = { viewModel.onEvent(WritingEvent.FilterInspirationType(it)) },
        onGenerate = { viewModel.onEvent(WritingEvent.GenerateInspiration) },
        onCancel = { viewModel.onEvent(WritingEvent.CancelInspiration) },
        onOptionSelected = { viewModel.onEvent(WritingEvent.SelectInspirationOption(it)) },
        onAccept = { viewModel.onEvent(WritingEvent.AcceptInspiration(it)) },
        onToggleFavorite = { viewModel.onEvent(WritingEvent.ToggleInspirationFavorite(it)) },
        onViewDetail = { },
        onRegenerate = { viewModel.onEvent(WritingEvent.GenerateInspiration) },
        onDismiss = { viewModel.onEvent(WritingEvent.DismissInspiration) },
        sheetState = inspirationSheetState
    )
}

// ─── TopAppBar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WritingTopBar(
    storyTitle: String,
    chapterTitle: String?,
    isImmersiveMode: Boolean,
    isVoMode: Boolean,
    onBack: () -> Unit,
    onToggleImmersive: () -> Unit,
    onToggleVoMode: () -> Unit,
    onChapterList: () -> Unit,
    onCharacter: () -> Unit,
    onWorldSetting: () -> Unit,
    onInspiration: () -> Unit,
    onMultiBranch: () -> Unit,
    onExport: () -> Unit,
    wordCount: Int,
    isSaving: Boolean,
    lastSaveTime: Long?,
    hasUnsavedChanges: Boolean,
    saveError: String?,
    // 新增：字数选择
    lengthOption: com.miaobi.app.domain.model.LengthOption,
    onLengthOptionChange: (com.miaobi.app.domain.model.LengthOption) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !isImmersiveMode,
        enter = slideInVertically(tween(200)) + fadeIn(tween(200)),
        exit = slideOutVertically(tween(200)) + fadeOut(tween(200))
    ) {
        TopAppBar(
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chapterTitle ?: storyTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            if (chapterTitle != null) {
                                Text(
                                    text = "$storyTitle  ·  ${wordCount}字",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            actions = {
                // 设置按钮 - 字数选择
                Box {
                    IconButton(onClick = { settingsExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = settingsExpanded,
                        onDismissRequest = { settingsExpanded = false }
                    ) {
                        Text(
                            text = "续写字数",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        com.miaobi.app.domain.model.LengthOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onLengthOptionChange(option)
                                    settingsExpanded = false
                                },
                                trailingIcon = {
                                    if (option == lengthOption) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                        Divider()
                        DropdownMenuItem(
                            text = { Text("章节列表") },
                            onClick = { onChapterList(); settingsExpanded = false },
                            leadingIcon = { Icon(Icons.Default.List, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("导出 TXT") },
                            onClick = { onExport(); settingsExpanded = false },
                            leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

// ─── WritingContent ────────────────────────────────────────────────────────────

@Composable
private fun WritingContent(
    content: String,
    onContentChange: (String) -> Unit,
    onTextSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var textFieldValue by remember(content) {
        mutableStateOf(TextFieldValue(content))
    }
    var lastSelection by remember { mutableStateOf<TextRange?>(null) }
    var pendingSelection by remember { mutableStateOf<String?>(null) }

    // Keep callback reference stable
    val onTextSelectedRef by rememberUpdatedState(onTextSelected)

    // 软键盘高度状态，用于调整底部留白
    var keyboardHeight by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Sync external content changes into TextFieldValue
    LaunchedEffect(content) {
        if (textFieldValue.text != content) {
            textFieldValue = TextFieldValue(content)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding() // 软键盘弹出时自动调整底部padding
            .navigationBarsPadding() // 适配导航栏
    ) {
        SelectionContainer {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val prevSel = lastSelection
                    val newSel = newValue.selection
                    val isPureSelection = prevSel != null &&
                        newSel != prevSel &&
                        newValue.text == textFieldValue.text

                    textFieldValue = newValue
                    lastSelection = newSel

                    if (newValue.text != content) {
                        onContentChange(newValue.text)
                    }

                    if (isPureSelection && newSel.min != newSel.max) {
                        val selected = newValue.text.substring(newSel.min, newSel.max)
                        if (selected.isNotBlank() && selected.length <= 2000) {
                            pendingSelection = selected
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = 48.dp), // 确保最后一行不被键盘遮挡
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 16.sp,
                    lineHeight = 32.sp,
                    letterSpacing = 0.2.sp
                ),
                cursorBrush = SolidColor(Color(0xFFFF6B6B)),
                decorationBox = { innerTextField ->
                    Box {
                        if (textFieldValue.text.isEmpty()) {
                            Column {
                                Text(
                                    text = "在此处开始写作...",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 16.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "点击屏幕下方的菱形按钮使用AI续写功能",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                        innerTextField()
                    }
                },
                onTextLayout = { layoutResult ->
                    // 光标位置自动滚动：确保光标所在行不被键盘遮挡
                    val cursorPos = textFieldValue.selection.min
                    if (cursorPos >= 0 && layoutResult.lineCount > 0) {
                        val cursorLine = layoutResult.getLineForOffset(cursorPos)
                        val lineBottom = layoutResult.getLineBottom(cursorLine)
                        val scrollOffset = scrollState.value.toInt()

                        // 如果光标行被遮挡（可视区域下方100dp内），则滚动
                        val bottomThreshold = scrollOffset + 500 // 500px ≈ 250dp
                        if (lineBottom > bottomThreshold) {
                            val targetScroll = (lineBottom - 500).toInt()
                            coroutineScope.launch {
                                scrollState.animateScrollTo(
                                    targetScroll.coerceIn(0, scrollState.maxValue)
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    // Fire selection callback outside of onValueChange to avoid composition issues
    LaunchedEffect(pendingSelection) {
        pendingSelection?.let { text ->
            onTextSelectedRef(text)
            pendingSelection = null
        }
    }
}

// ─── No Chapter View ──────────────────────────────────────────────────────────

@Composable
private fun NoChapterView(onAddChapter: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "暂无章节",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onAddChapter) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("创建章节")
        }
    }
}

// ─── Length Selector ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LengthSelector(
    selectedOption: com.miaobi.app.domain.model.LengthOption,
    onOptionSelected: (com.miaobi.app.domain.model.LengthOption) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        com.miaobi.app.domain.model.LengthOption.entries.forEach { option ->
            FilterChip(
                selected = option == selectedOption,
                onClick = { if (enabled) onOptionSelected(option) },
                label = { Text(option.label) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── AI Generation Bar ─────────────────────────────────────────────────────────

@Composable
private fun AiGenerationBar(
    isGenerating: Boolean,
    prompt: String,
    onPromptChange: (String) -> Unit,
    lengthOption: com.miaobi.app.domain.model.LengthOption,
    onLengthOptionChange: (com.miaobi.app.domain.model.LengthOption) -> Unit,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
    onSaveDraft: () -> Unit,
    onShowHistory: () -> Unit,
    onMultiBranch: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Prompt input
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("AI 续写方向（可选）", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                singleLine = true,
                enabled = !isGenerating,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            // Length selector + action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LengthSelector(
                    selectedOption = lengthOption,
                    onOptionSelected = onLengthOptionChange,
                    enabled = !isGenerating
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // History
                ActionChip(
                    icon = Icons.Default.History,
                    label = "历史",
                    onClick = onShowHistory,
                    enabled = !isGenerating,
                    modifier = Modifier.weight(1f)
                )
                // Multi-branch
                ActionChip(
                    icon = Icons.Default.AccountTree,
                    label = "多分支",
                    onClick = onMultiBranch,
                    enabled = !isGenerating,
                    modifier = Modifier.weight(1f)
                )
                // Save
                ActionChip(
                    icon = Icons.Default.Save,
                    label = "保存",
                    onClick = onSaveDraft,
                    enabled = !isGenerating,
                    modifier = Modifier.weight(1f)
                )
                // Generate / Cancel
                if (isGenerating) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("停止")
                    }
                } else {
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier.weight(1.5f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("续写")
                    }
                }
            }

            // Generating indicator
            AnimatedVisibility(visible = isGenerating) {
                TypingIndicator()
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

// ─── Chapter List Sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterListSheet(
    chapters: List<Chapter>,
    currentChapter: Chapter?,
    onSelectChapter: (Chapter) -> Unit,
    onDeleteChapter: (Long) -> Unit,
    onAddChapter: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("章节列表", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onAddChapter) {
                    Icon(Icons.Default.Add, contentDescription = "添加章节")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(chapters) { chapter ->
                    ListItem(
                        headlineContent = { Text(chapter.title) },
                        supportingContent = { Text("${chapter.wordCount} 字") },
                        trailingContent = {
                            if (chapter.id == currentChapter?.id) {
                                Icon(Icons.Default.Check, contentDescription = "当前",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.clickable { onSelectChapter(chapter) }
                    )
                    Divider()
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Character Sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterSheet(
    characters: List<Character>,
    onAddCharacter: () -> Unit,
    onDeleteCharacter: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("角色设定", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onAddCharacter) {
                    Icon(Icons.Default.Add, contentDescription = "添加角色")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (characters.isEmpty()) {
                Text("暂无角色，点击添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                LazyColumn {
                    items(characters) { character ->
                        CharacterItem(character = character, onDelete = { onDeleteCharacter(character.id) })
                        Divider()
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CharacterItem(character: Character, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(character.name) },
        supportingContent = { Text(character.description, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    )
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除角色「${character.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

// ─── World Setting Sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorldSettingSheet(
    worldSettings: List<WorldSetting>,
    onAddWorldSetting: () -> Unit,
    onDeleteWorldSetting: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("世界观设定", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onAddWorldSetting) {
                    Icon(Icons.Default.Add, contentDescription = "添加设定")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (worldSettings.isEmpty()) {
                Text("暂无世界观设定，点击添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                LazyColumn {
                    items(worldSettings) { setting ->
                        WorldSettingItem(setting = setting, onDelete = { onDeleteWorldSetting(setting.id) })
                        Divider()
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WorldSettingItem(setting: WorldSetting, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(setting.name) },
        supportingContent = { Text(setting.content, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    )
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${setting.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

// ─── Draft History Sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftHistorySheet(
    drafts: List<ChapterDraft>,
    onSelectDraft: (ChapterDraft) -> Unit,
    onDeleteDraft: (Long) -> Unit,
    onViewVersions: (ChapterDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("历史版本", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            if (drafts.isEmpty()) {
                Text("暂无历史版本",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                LazyColumn {
                    items(drafts) { draft ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        var expanded by remember { mutableStateOf(false) }
                        ListItem(
                            headlineContent = { Text("版本 ${draft.version}") },
                            supportingContent = { Text("${dateFormat.format(Date(draft.createdAt))} · ${draft.wordCount} 字") },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { onSelectDraft(draft) }) { Text("使用") }
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                                    }
                                    IconButton(onClick = { showDeleteConfirm = true }) {
                                        Icon(Icons.Default.Delete, null)
                                    }
                                }
                            }
                        )
                        if (expanded) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(text = draft.content, style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    maxLines = 10, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { onViewVersions(draft) }, modifier = Modifier.padding(horizontal = 16.dp)) {
                                Icon(Icons.Default.History, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("查看所有历史版本")
                            }
                        }
                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("确认删除") },
                                text = { Text("确定要删除版本 ${draft.version} 吗？") },
                                confirmButton = {
                                    TextButton(onClick = { onDeleteDraft(draft.id); showDeleteConfirm = false }) {
                                        Text("删除", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
                                }
                            )
                        }
                        Divider()
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Draft Versions Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftVersionsSheet(
    versions: List<ChapterDraftVersion>,
    onRestoreVersion: (ChapterDraftVersion) -> Unit,
    onDeleteVersion: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("续写历史版本", style = MaterialTheme.typography.titleLarge)
                Text("共 ${versions.size} 个版本",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (versions.isEmpty()) {
                Text("暂无历史版本",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                LazyColumn {
                    items(versions) { version ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        var expanded by remember { mutableStateOf(false) }
                        ListItem(
                            headlineContent = { Text("版本 ${version.version}") },
                            supportingContent = { Text("${dateFormat.format(Date(version.createdAt))} · ${version.wordCount} 字") },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                                    }
                                    TextButton(onClick = { onRestoreVersion(version) }) { Text("恢复") }
                                    IconButton(onClick = { showDeleteConfirm = true }) {
                                        Icon(Icons.Default.Delete, null)
                                    }
                                }
                            }
                        )
                        if (expanded) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(text = version.content, style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    maxLines = 20, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("确认删除") },
                                text = { Text("确定要删除版本 ${version.version} 吗？此操作不可恢复。") },
                                confirmButton = {
                                    TextButton(onClick = { onDeleteVersion(version.id); showDeleteConfirm = false }) {
                                        Text("删除", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
                                }
                            )
                        }
                        Divider()
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun AddChapterDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建章节") },
        text = {
            OutlinedTextField(
                value = title, onValueChange = onTitleChange,
                label = { Text("章节标题") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AddCharacterDialog(
    name: String, description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加角色") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = onNameChange,
                    label = { Text("角色名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = onDescriptionChange,
                    label = { Text("角色描述") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = name.isNotBlank()) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AddWorldSettingDialog(
    name: String, content: String,
    onNameChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加世界观设定") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = onNameChange,
                    label = { Text("设定名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = content, onValueChange = onContentChange,
                    label = { Text("设定内容") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = name.isNotBlank()) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ─── Export ─────────────────────────────────────────────────────────────────────

private fun exportToTxt(context: Context, title: String, content: String) {
    try {
        val fileName = "${title}.txt"
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "导出小说"))
    } catch (e: Exception) {
        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
