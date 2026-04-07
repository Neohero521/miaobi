# 妙笔 AI 写小说 — 界面重构架构变更文档

**版本：** v1.0
**日期：** 2026-04-07
**范围：** WritingScreen 及相关 UI 组件重构

---

## 一、现状概述

### 当前架构

```
WritingScreen
├── TopAppBar（标题 + 8个 action 图标）
├── GeneratedContentBar（AI 续写结果采纳条）
├── WritingContent（OutlinedTextField 全屏编辑）
├── AiGenerationBar（底部 AI 操作条：prompt + 长度选择 + 4按钮）
├── RewriteBottomSheet（ModalBottomSheet）
├── MultiBranchBottomSheet（ModalBottomSheet）
├── InspirationBottomSheet（ModalBottomSheet）
├── ChapterListSheet / CharacterSheet / WorldSettingSheet / DraftHistorySheet / DraftVersionsSheet
└── 多个 Dialog（AddChapter / AddCharacter / AddWorldSetting）
```

**主要问题：**
- TopAppBar 承载 8 个 action，过于拥挤，与沉浸式写作体验冲突
- 润色改写、灵感生成、AI 续写各走独立的 BottomSheet，切换成本高
- "AI 续写" 以单条长文本输出，用户需先预览再采纳，缺乏多选对比
- 缺少写作风格（Tab）直接选择和 V/O（Verbose/Original）模式切换

---

## 二、目标设计

```
┌──────────────────────────────────────┐
│  [沉浸模式：隐藏 TopAppBar，           │
│   或仅保留 ↵ 标题栏]                  │
│                                      │
│         写作编辑区（TextField）        │
│                                      │
│                         [悬浮 AI ✨] │  ← 右下角 FloatingActionButton
│                                      │
│  ┌────────────────────────────────┐  │
│  │ ↩ ↪ │ 润色 │ 改写 │ 灵感 │ 💾  │  │  ← 底部工具栏（WritingToolbar）
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │ 风格 Tab：古风│现代│简洁│华丽│口语│  │  ← 风格选择（RewriteStyleTabRow）
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘

AI 续写抽屉（AiContinuationPanel）：
  ┌──────────────────────────────────┐
  │ AI 续写 · 3 条建议                │
  │ ─────────────────────────────── │
  │ [建议1] ···内容预览···  [使用]    │
  │ [建议2] ···内容预览···  [使用]    │
  │ [建议3] ···内容预览···  [使用]    │
  │ ─────────────────────────────── │
  │       [🔄 换一批]                │
  └──────────────────────────────────┘
```

---

## 三、需要重构的 UI 组件

| 组件 | 当前状态 | 重构方向 |
|------|---------|---------|
| `WritingScreen` | 主页面，全功能混合 | 拆分为编辑区 + 底部工具栏 + 悬浮按钮，事件统一收敛 |
| `TopAppBar` | 8 图标拥挤 | 沉浸模式下隐藏；退出沉浸时显示极简标题栏 |
| `AiGenerationBar` | prompt + 长度 + 按钮 | 移除，原功能分流至工具栏 + 悬浮按钮 |
| `GeneratedContentBar` | 单条采纳/丢弃 | 替换为 `AiContinuationPanel`（底部抽屉，3 条建议） |
| `RewriteBottomSheet` | 独立 Modal | 风格选择整合为 `RewriteStyleTabRow`，触发方式改为工具栏 |
| `InspirationBottomSheet` | 独立 Modal | 改为工具栏"灵感"按钮弹出，结果同 `AiContinuationPanel` 样式展示 |
| `MultiBranchBottomSheet` | 独立 Modal | **保留**，多分支场景复杂，单独界面更清晰 |

**保留（最小改动）：**
- `ChapterListSheet`、`CharacterSheet`、`WorldSettingSheet`（管理侧边栏，触发频率低）
- `DraftHistorySheet`、`DraftVersionsSheet`
- `AddChapterDialog`、`AddCharacterDialog`、`AddWorldSettingDialog`

---

## 四、新组件设计

### 4.1 WritingToolbar（底部工具栏）

```kotlin
@Composable
fun WritingToolbar(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPolish: () -> Unit,       // 润色：需先选中文本
    onRewrite: () -> Unit,      // 改写：弹出 RewriteStyleTabRow
    onInspiration: () -> Unit,
    onSave: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    modifier: Modifier = Modifier
)
```

**布局：** `Row(horizontalArrangement = Arrangement.SpaceEvenly)` 或横向 `IconToggleButton` 排列，左侧 Undo/Redo，右侧 4 个功能按钮。最低高度 56dp，颜色跟随 `Surface`。

**Event 映射：**
- `WritingEvent.Undo` / `WritingEvent.Redo`：需在 `WritingUiState` 中维护 `undoStack` / `redoStack`
- `WritingEvent.Polish`：等同于当前 Rewrite 但不替换原文，仅展示润色结果供对比
- `WritingEvent.OpenRewriteStyle`：展开 `RewriteStyleTabRow`
- `WritingEvent.SaveContent`：复用现有 SaveContent

---

### 4.2 FloatingAiButton（悬浮 AI 按钮）

```kotlin
@Composable
fun FloatingAiButton(
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**设计：** `FloatingActionButton` 固定在右下角（`Modifier.align(Alignment.BottomEnd)` 配合 `Box`），图标 `AutoAwesome`，生成中显示 `CircularProgressIndicator`。

**交互：**
- 点击 → 触发 `WritingEvent.GenerateContinue`（或弹出 AI 续写面板，取决于 UX 选择）
- 生成中 → 显示 progress，点击停止

---

### 4.3 RewriteStyleTabRow（风格选择 Tab）

```kotlin
@Composable
fun RewriteStyleTabRow(
    selectedStyle: RewriteStyle,
    onStyleSelected: (RewriteStyle) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
)
```

**5 种风格（与现有 `RewriteStyle` 对齐）：** 古风 · 现代 · 简洁 · 华丽 · 口语化

**位置：** 工具栏下方，`AnimatedVisibility` 控制显隐，展开时高度 ~48dp，Tab 横向可滚动。

**Event：** `WritingEvent.SelectRewriteStyle`（复用）

---

### 4.4 AiContinuationPanel（AI 续写抽屉）

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiContinuationPanel(
    suggestions: List<ContinuationSuggestion>,
    isGenerating: Boolean,
    generationProgress: Float,       // 0~1，用于 LinearProgressIndicator
    onUse: (Int) -> Unit,             // 采纳第 N 条
    onRegenerate: () -> Unit,         // 换一批
    onDismiss: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
)

data class ContinuationSuggestion(
    val id: Int,
    val content: String,
    val wordCount: Int,
    val isSelected: Boolean = false
)
```

**与原 `GeneratedContentBar` 的区别：**
- 原版：单条长文本 → "采纳/丢弃"
- 新版：3 条建议并行展示，每条独立"使用"按钮，用户可比较后再选

**交互：**
- "使用" → 追加对应 suggestion 到内容末尾
- "换一批" → 重新调用 `generateContinuation()`
- 生成中：`LinearProgressIndicator` 进度条 + 3 个 placeholder card
- 抽屉默认 `peekHeight = 0`（完全隐藏），展开时约 60% 屏幕高度

---

### 4.5 VoModeToggle（V/O 模式切换）

```kotlin
@Composable
fun VoModeToggle(
    isVoMode: Boolean,        // true = Verbose, false = Original
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
)
```

**位置：** 可放在 TopAppBar 右侧（沉浸模式隐藏时随 TopAppBar 消失），或工具栏最右侧。

**语义：**
- **V (Verbose) 模式**：AI 输出更详细、描写丰富
- **O (Original) 模式**：AI 输出简洁，贴近用户已有文风

---

## 五、状态管理变更

### 5.1 WritingUiState 新增字段

```kotlin
data class WritingUiState(
    // === 新增：沉浸模式与工具栏 ===
    val isImmersiveMode: Boolean = false,          // 沉浸模式（隐藏 TopAppBar）
    val showToolbar: Boolean = true,               // 工具栏显隐

    // === 新增：底部抽屉 ===
    val showContinuationPanel: Boolean = false,     // AI 续写抽屉
    val continuationSuggestions: List<ContinuationSuggestion> = emptyList(),
    val generationProgress: Float = 0f,              // 0~1

    // === 新增：Rewrite 风格 Tab ===
    val showRewriteStyleRow: Boolean = false,       // 风格 Tab 展开
    val selectedStyle: RewriteStyle = RewriteStyle.MODERN,

    // === 新增：V/O 模式 ===
    val isVoMode: Boolean = true,                   // true=Verbose, false=Original

    // === 新增：Undo/Redo 栈 ===
    val undoStack: List<String> = emptyList(),
    val redoStack: List<String> = emptyList(),

    // === 现有字段（保留）===
    val story: Story? = null,
    val currentChapter: Chapter? = null,
    val chapters: List<Chapter> = emptyList(),
    val characters: List<Character> = emptyList(),
    val worldSettings: List<WorldSetting> = emptyList(),
    val content: String = "",
    val userPrompt: String = "",
    val lengthOption: LengthOption = LengthOption.MEDIUM,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = true,
    // ... 其余字段保持不变
)
```

### 5.2 WritingEvent 新增

```kotlin
sealed class WritingEvent {
    // 沉浸模式
    object ToggleImmersiveMode : WritingEvent()

    // 工具栏 & 编辑
    object Undo : WritingEvent()
    object Redo : WritingEvent()
    object ToggleToolbar : WritingEvent()

    // AI 续写抽屉
    object ToggleContinuationPanel : WritingEvent()
    data class UseContinuationSuggestion(val index: Int) : WritingEvent()
    object RegenerateSuggestions : WritingEvent()

    // Rewrite 风格
    object ToggleRewriteStyleRow : WritingEvent()
    data class SelectStyle(val style: RewriteStyle) : WritingEvent()
    object Polish : WritingEvent()          // 润色（不替换原文）

    // V/O 模式
    object ToggleVoMode : WritingEvent()

    // 其余事件保持不变
}
```

### 5.3 状态流转变化

**生成续写流程（Before → After）：**

```
Before:
  GenerateContinue → isGenerating=true → SSE流式接收 → generatedContent累加
  → 显示 GeneratedContentBar → AcceptGenerated → content拼接

After:
  GenerateContinue → isGenerating=true → 收集3条suggestion
  → showContinuationPanel=true → 显示 AiContinuationPanel
  → UseSuggestion(N) → content拼接对应建议 → 清空 suggestions
```

**Rewrite 流程（Before → After）：**

```
Before:
  TriggerRewrite(选中文字) → 弹出 RewriteBottomSheet → 选择风格 → 改写 → 采纳

After:
  工具栏点击"改写" → 展开 RewriteStyleTabRow → 选择风格
  → 执行 Rewrite（复用现有 executeRewrite 逻辑）
  → 弹出 RenameBottomSheet（复用）或内联到工具栏下方
  → 采纳后替换原文
```

---

## 六、组件依赖关系

```
WritingScreen
├── WritingToolbar          ← 新增，底部固定
│   └── RewriteStyleTabRow ← 新增，可折叠
├── FloatingAiButton       ← 新增，右下角 FAB
├── AiContinuationPanel    ← 新增，ModalBottomSheet
├── WritingContent         ← 重构，移除 TopAppBar 按钮区
├── RewriteBottomSheet     ← 重构接入方式（不自动弹出，改为工具栏触发）
├── InspirationBottomSheet ← 重构接入方式（工具栏触发）
├── MultiBranchBottomSheet ← 保留原样
├── ChapterListSheet       ← 保留
├── CharacterSheet         ← 保留
├── WorldSettingSheet       ← 保留
└── [Dialogs]              ← 保留
```

---

## 七、实施计划建议

### Phase 1：骨架重构（不影响现有功能）
1. 新增 `WritingToolbar` 组件，`WritingScreen` 中加入底部占位（暂时透明）
2. 新增 `FloatingAiButton`，放在右下角（不影响现有 `AiGenerationBar`）
3. 新增 `VoModeToggle`，放在 TopAppBar 右侧

### Phase 2：状态接入
4. `WritingUiState` 新增 `isImmersiveMode`、`undoStack`/`redoStack`、`showContinuationPanel`、`selectedStyle` 字段
5. `WritingEvent` 新增对应事件
6. `WritingViewModel` 实现 Undo/Redo 逻辑（每次 `UpdateContent` 前压栈）

### Phase 3：UI 替换
7. 隐藏/简化 TopAppBar（沉浸模式）
8. 将 `AiGenerationBar` 替换为 `WritingToolbar` + `FloatingAiButton`
9. 将 `GeneratedContentBar` 替换为 `AiContinuationPanel`
10. `RewriteBottomSheet` 改由工具栏"改写"按钮触发

### Phase 4：细节打磨
11. `RewriteStyleTabRow` 动画集成
12. 润色（Polish）功能实现（原文+润色结果对比展示）
13. 多语言、主题适配

---

## 八、风险与注意事项

1. **SSE 流式输出拆分**：原版 `generatedContent` 是整段接收；新版需要后端配合或前端做流式拆分，目前 `AiRepository.continueStory` 返回的是 SSE stream，需要在 ViewModel 层做累积后按策略拆分为 3 条
2. **Undo/Redo 栈内存**：纯文本栈无压缩，长文场景需限制栈深度（如 max 50 条）
3. **BottomSheet 层级**：工具栏弹出类 BottomSheet 与 `MultiBranchBottomSheet` 并存时需注意 `sheetState` 独立
4. **向后兼容**：当前 `RewriteState`、`InspirationState`、`MultiBranchState` 保持独立，新增 `ContinuationState` 专门处理续写建议，三者可并存
