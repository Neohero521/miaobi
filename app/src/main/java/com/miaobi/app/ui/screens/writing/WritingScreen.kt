package com.miaobi.app.ui.screens.writing

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.miaobi.app.domain.model.Chapter
import com.miaobi.app.domain.model.ChapterDraft
import com.miaobi.app.domain.model.ChapterDraftVersion
import com.miaobi.app.domain.model.Character
import com.miaobi.app.domain.model.WorldSetting
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    viewModel: WritingViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Sheet states
    val rewriteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val multiBranchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val inspirationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val continuationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Show rewrite bottom sheet when text is selected
    val showRewriteSheet = uiState.rewriteState.selectedText.isNotBlank()

    // Bottom sheet for AI continuation panel
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
            // 沉浸模式：隐藏 TopAppBar
            AnimatedVisibility(
                visible = !uiState.isImmersiveMode,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.story?.title ?: "写小说",
                                style = MaterialTheme.typography.titleMedium
                            )
                            uiState.currentChapter?.let {
                                Text(
                                    text = it.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        // V/O 模式切换
                        VoModeToggle(
                            isVoMode = uiState.isVoMode,
                            onToggle = { viewModel.onEvent(WritingEvent.ToggleVoMode) }
                        )

                        // 沉浸模式切换
                        IconButton(onClick = { viewModel.onEvent(WritingEvent.ToggleImmersiveMode) }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "沉浸模式")
                        }

                        IconButton(onClick = { viewModel.onEvent(WritingEvent.ToggleChapterList) }) {
                            Icon(Icons.Default.List, contentDescription = "章节列表")
                        }
                        IconButton(onClick = { viewModel.onEvent(WritingEvent.ToggleCharacterSheet) }) {
                            Icon(Icons.Default.Person, contentDescription = "角色")
                        }
                        IconButton(onClick = { viewModel.onEvent(WritingEvent.ToggleWorldSettingSheet) }) {
                            Icon(Icons.Default.Public, contentDescription = "世界观")
                        }
                        IconButton(onClick = {
                            uiState.currentChapter?.let { chapter ->
                                exportToTxt(context, chapter.title, uiState.content)
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "导出")
                        }
                        IconButton(onClick = { viewModel.onEvent(WritingEvent.ToggleInspirationSheet) }) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "灵感")
                        }
                        IconButton(onClick = { viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet) }) {
                            Icon(Icons.Default.AccountTree, contentDescription = "多分支续写")
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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Rewrite style tab row (展开时显示)
                AnimatedVisibility(
                    visible = uiState.showRewriteStyleRow,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    RewriteStyleTabRow(
                        selectedStyle = uiState.selectedStyle,
                        onStyleSelected = { viewModel.onEvent(WritingEvent.SelectStyle(it)) },
                        enabled = !uiState.rewriteState.isRewriting
                    )
                }

                // Main content area
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (uiState.currentChapter == null) {
                        NoChapterView(
                            onAddChapter = { viewModel.onEvent(WritingEvent.ShowAddChapterDialog) }
                        )
                    } else {
                        WritingContent(
                            content = uiState.content,
                            onContentChange = { viewModel.onEvent(WritingEvent.UpdateContent(it)) },
                            onSave = { viewModel.onEvent(WritingEvent.SaveContent) },
                            onTextSelected = { selected ->
                                if (selected.isNotBlank()) {
                                    viewModel.onEvent(WritingEvent.TriggerRewrite(selected))
                                }
                            }
                        )
                    }
                }

                // WritingToolbar 底部工具栏
                if (uiState.showToolbar) {
                    WritingToolbar(
                        onUndo = { viewModel.onEvent(WritingEvent.Undo) },
                        onRedo = { viewModel.onEvent(WritingEvent.Redo) },
                        onPolish = { viewModel.onEvent(WritingEvent.Polish) },
                        onRewrite = {
                            // 改写：使用当前内容，弹出风格 Tab
                            if (uiState.content.isNotBlank()) {
                                viewModel.onEvent(WritingEvent.TriggerRewrite(uiState.content))
                                viewModel.onEvent(WritingEvent.ToggleRewriteStyleRow)
                            }
                        },
                        onInspiration = { viewModel.onEvent(WritingEvent.ToggleInspirationSheet) },
                        onSave = { viewModel.onEvent(WritingEvent.SaveContent) },
                        canUndo = uiState.undoStack.isNotEmpty(),
                        canRedo = uiState.redoStack.isNotEmpty()
                    )
                }

                // AI generation bar (保留，作为prompt输入区域)
                AiGenerationBar(
                    isGenerating = uiState.isGenerating,
                    prompt = uiState.userPrompt,
                    onPromptChange = { viewModel.onEvent(WritingEvent.UpdatePrompt(it)) },
                    lengthOption = uiState.lengthOption,
                    onLengthOptionChange = { viewModel.onEvent(WritingEvent.UpdateLengthOption(it)) },
                    onGenerate = { viewModel.onEvent(WritingEvent.GenerateContinue) },
                    onCancel = { viewModel.onEvent(WritingEvent.CancelGeneration) },
                    onSaveDraft = {
                        uiState.currentChapter?.let {
                            viewModel.onEvent(WritingEvent.SaveContent)
                        }
                    },
                    onShowHistory = { viewModel.onEvent(WritingEvent.ToggleDraftHistory) },
                    onMultiBranch = { viewModel.onEvent(WritingEvent.ToggleMultiBranchSheet) }
                )
            }

            // Floating AI Button — 右下角悬浮
            FloatingAiButton(
                isGenerating = uiState.isGenerating,
                onClick = {
                    if (uiState.isGenerating) {
                        viewModel.onEvent(WritingEvent.CancelGeneration)
                    } else {
                        viewModel.onEvent(WritingEvent.GenerateContinue)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = if (uiState.showToolbar) 140.dp else 16.dp
                    )
            )
        }

        // Error snackbar
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                viewModel.onEvent(WritingEvent.ClearError)
            }
        }

        // Chapter list sheet
        if (uiState.showChapterList) {
            ChapterListSheet(
                chapters = uiState.chapters,
                currentChapter = uiState.currentChapter,
                onSelectChapter = { viewModel.onEvent(WritingEvent.SelectChapter(it)) },
                onDeleteChapter = { viewModel.onEvent(WritingEvent.DeleteChapter(it)) },
                onAddChapter = { viewModel.onEvent(WritingEvent.ShowAddChapterDialog) },
                onDismiss = { viewModel.onEvent(WritingEvent.ToggleChapterList) }
            )
        }

        // Character sheet
        if (uiState.showCharacterSheet) {
            CharacterSheet(
                characters = uiState.characters,
                onAddCharacter = { viewModel.onEvent(WritingEvent.ShowAddCharacterDialog) },
                onDeleteCharacter = { viewModel.onEvent(WritingEvent.DeleteCharacter(it)) },
                onDismiss = { viewModel.onEvent(WritingEvent.ToggleCharacterSheet) }
            )
        }

        // World setting sheet
        if (uiState.showWorldSettingSheet) {
            WorldSettingSheet(
                worldSettings = uiState.worldSettings,
                onAddWorldSetting = { viewModel.onEvent(WritingEvent.ShowAddWorldSettingDialog) },
                onDeleteWorldSetting = { viewModel.onEvent(WritingEvent.DeleteWorldSetting(it)) },
                onDismiss = { viewModel.onEvent(WritingEvent.ToggleWorldSettingSheet) }
            )
        }

        // Draft history sheet
        if (uiState.showDraftHistory) {
            DraftHistorySheet(
                drafts = uiState.drafts,
                onSelectDraft = { viewModel.onEvent(WritingEvent.SelectDraft(it)) },
                onDeleteDraft = { viewModel.onEvent(WritingEvent.DeleteDraft(it)) },
                onViewVersions = { viewModel.onEvent(WritingEvent.SelectDraftForVersions(it)) },
                onDismiss = { viewModel.onEvent(WritingEvent.ToggleDraftHistory) }
            )
        }

        // Draft versions sheet
        if (uiState.showDraftVersions) {
            DraftVersionsSheet(
                versions = uiState.draftVersions,
                onRestoreVersion = { viewModel.onEvent(WritingEvent.RestoreVersion(it)) },
                onDeleteVersion = { viewModel.onEvent(WritingEvent.DeleteVersion(it)) },
                onDismiss = { viewModel.onEvent(WritingEvent.HideDraftVersions) }
            )
        }

        // Add chapter dialog
        if (uiState.showAddChapterDialog) {
            AddChapterDialog(
                title = uiState.newChapterTitle,
                onTitleChange = { viewModel.onEvent(WritingEvent.UpdateNewChapterTitle(it)) },
                onConfirm = { viewModel.onEvent(WritingEvent.AddChapter) },
                onDismiss = { viewModel.onEvent(WritingEvent.HideAddChapterDialog) }
            )
        }

        // Add character dialog
        if (uiState.showAddCharacterDialog) {
            AddCharacterDialog(
                name = uiState.newCharacterName,
                description = uiState.newCharacterDescription,
                onNameChange = { viewModel.onEvent(WritingEvent.UpdateNewCharacterName(it)) },
                onDescriptionChange = { viewModel.onEvent(WritingEvent.UpdateNewCharacterDescription(it)) },
                onConfirm = { viewModel.onEvent(WritingEvent.AddCharacter) },
                onDismiss = { viewModel.onEvent(WritingEvent.HideAddCharacterDialog) }
            )
        }

        // Add world setting dialog
        if (uiState.showAddWorldSettingDialog) {
            AddWorldSettingDialog(
                name = uiState.newWorldSettingName,
                content = uiState.newWorldSettingContent,
                onNameChange = { viewModel.onEvent(WritingEvent.UpdateNewWorldSettingName(it)) },
                onContentChange = { viewModel.onEvent(WritingEvent.UpdateNewWorldSettingContent(it)) },
                onConfirm = { viewModel.onEvent(WritingEvent.AddWorldSetting) },
                onDismiss = { viewModel.onEvent(WritingEvent.HideAddWorldSettingDialog) }
            )
        }

        // Rewrite bottom sheet
        if (showRewriteSheet) {
            RewriteBottomSheet(
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
        }

        // Multi-branch bottom sheet
        if (uiState.showMultiBranchSheet) {
            MultiBranchBottomSheet(
                multiBranchState = uiState.multiBranchState,
                onCountChanged = { viewModel.onEvent(WritingEvent.UpdateBranchCount(it)) },
                onGenerate = { viewModel.onEvent(WritingEvent.GenerateBranches) },
                onCancel = { viewModel.onEvent(WritingEvent.CancelBranches) },
                onBranchSelected = { viewModel.onEvent(WritingEvent.SelectBranch(it)) },
                onAccept = { viewModel.onEvent(WritingEvent.AcceptBranch) },
                onRegenerateBranch = { viewModel.onEvent(WritingEvent.RegenerateBranch(it)) },
                onDismiss = { viewModel.onEvent(WritingEvent.DismissBranches) },
                sheetState = multiBranchSheetState
            )
        }

        // Inspiration bottom sheet
        if (uiState.showInspirationSheet) {
            InspirationBottomSheet(
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
    }
}

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
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无章节",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddChapter) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("创建章节")
        }
    }
}

@Composable
private fun WritingContent(
    content: String,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onTextSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var textFieldValue by remember { mutableStateOf(TextFieldValue(content)) }
    var lastSelection by remember { mutableStateOf<TextRange?>(null) }
    var pendingSelectedText by remember { mutableStateOf<String?>(null) }

    // Keep onTextSelected up to date without restarting the effect
    val onTextSelectedState by rememberUpdatedState(onTextSelected)

    // Sync external content changes into TextFieldValue
    LaunchedEffect(content) {
        if (textFieldValue.text != content) {
            textFieldValue = TextFieldValue(content)
        }
    }

    // When a pure selection change occurs (text unchanged, selection moved), store selected text
    SelectionContainer {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val prevSel = lastSelection
                val newSel = newValue.selection

                // Detect pure selection: text same, selection changed
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
                    if (selected.isNotBlank()) {
                        pendingSelectedText = selected
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = "开始写作...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }

    // Fire callback when pending selection is set (runs in Composable context)
    LaunchedEffect(pendingSelectedText) {
        pendingSelectedText?.let { text ->
            onTextSelectedState(text)
        }
    }
}

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
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("续写方向指引（可选）") },
                singleLine = true,
                enabled = !isGenerating
            )

            Spacer(modifier = Modifier.height(8.dp))

            LengthSelector(
                selectedOption = lengthOption,
                onOptionSelected = onLengthOptionChange,
                enabled = !isGenerating
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShowHistory,
                    modifier = Modifier.weight(1f),
                    enabled = !isGenerating
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("历史")
                }

                OutlinedButton(
                    onClick = onMultiBranch,
                    modifier = Modifier.weight(1f),
                    enabled = !isGenerating
                ) {
                    Icon(Icons.Default.AccountTree, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("多分支")
                }

                OutlinedButton(
                    onClick = onSaveDraft,
                    modifier = Modifier.weight(1f),
                    enabled = !isGenerating
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存")
                }

                if (isGenerating) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("停止")
                    }
                } else {
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI续写")
                    }
                }
            }

            if (isGenerating) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "生成中…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

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
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "当前",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable { onSelectChapter(chapter) }
                    )
                    Divider()
                }
            }
        }
    }
}

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
                Text(
                    text = "暂无角色，点击添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                LazyColumn {
                    items(characters) { character ->
                        CharacterItem(
                            character = character,
                            onDelete = { onDeleteCharacter(character.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterItem(
    character: Character,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(character.name) },
        supportingContent = {
            Text(
                character.description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
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
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

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
                Text(
                    text = "暂无世界观设定，点击添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                LazyColumn {
                    items(worldSettings) { setting ->
                        WorldSettingItem(
                            setting = setting,
                            onDelete = { onDeleteWorldSetting(setting.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldSettingItem(
    setting: WorldSetting,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(setting.name) },
        supportingContent = {
            Text(
                setting.content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
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
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

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
                Text(
                    text = "暂无历史版本",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                LazyColumn {
                    items(drafts) { draft ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        var expanded by remember { mutableStateOf(false) }

                        ListItem(
                            headlineContent = { Text("版本 ${draft.version}") },
                            supportingContent = {
                                Text(
                                    "${dateFormat.format(Date(draft.createdAt))} · ${draft.wordCount} 字"
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { onSelectDraft(draft) }) {
                                        Text("使用")
                                    }
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Icon(
                                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "展开"
                                        )
                                    }
                                    IconButton(onClick = { showDeleteConfirm = true }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除")
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
                                Text(
                                    text = draft.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    maxLines = 10,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = { onViewVersions(draft) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null)
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
                                    TextButton(onClick = {
                                        onDeleteDraft(draft.id)
                                        showDeleteConfirm = false
                                    }) {
                                        Text("删除", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("取消")
                                    }
                                }
                            )
                        }

                        Divider()
                    }
                }
            }
        }
    }
}

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
                Text(
                    text = "共 ${versions.size} 个版本",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (versions.isEmpty()) {
                Text(
                    text = "暂无历史版本",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                LazyColumn {
                    items(versions) { version ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        var expanded by remember { mutableStateOf(false) }

                        ListItem(
                            headlineContent = { Text("版本 ${version.version}") },
                            supportingContent = {
                                Text(
                                    "${dateFormat.format(Date(version.createdAt))} · ${version.wordCount} 字"
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Icon(
                                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "展开"
                                        )
                                    }
                                    TextButton(onClick = { onRestoreVersion(version) }) {
                                        Text("恢复")
                                    }
                                    IconButton(onClick = { showDeleteConfirm = true }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除")
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
                                Text(
                                    text = version.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    maxLines = 20,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("确认删除") },
                                text = { Text("确定要删除版本 ${version.version} 吗？此操作不可恢复。") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        onDeleteVersion(version.id)
                                        showDeleteConfirm = false
                                    }) {
                                        Text("删除", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("取消")
                                    }
                                }
                            )
                        }

                        Divider()
                    }
                }
            }
        }
    }
}

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
                value = title,
                onValueChange = onTitleChange,
                label = { Text("章节标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AddCharacterDialog(
    name: String,
    description: String,
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
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("角色名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("角色描述") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AddWorldSettingDialog(
    name: String,
    content: String,
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
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("设定名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("设定内容") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun exportToTxt(context: Context, title: String, content: String) {
    try {
        val fileName = "${title}.txt"
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

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
