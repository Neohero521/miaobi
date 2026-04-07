# 多分支生成功能 — 架构变更文档

## 一、核心设计思路

多分支生成本质上是 **并行调用多次续写**，每次调用注入不同的「走向指令」来实现差异化。

- **并行**：多条分支同时生成，使用 `async + await` 或 `kotlinx.coroutines.flow` 并发收集
- **差异化**：通过 `PromptBuilder` 为每条分支注入不同的 `branchDirection`（如：剧情升级/情感线/意外转折/探索新角色）
- **UI**：新增 `MultiBranchBottomSheet`，以卡片流形式展示各分支，每张卡片含 AI 生成的摘要标签

---

## 二、代码模块变更

### 2.1 新增 Model

**文件**: `domain/model/BranchOption.kt`（新建）

```kotlin
data class BranchOption(
    val index: Int,
    val content: String,          // 生成的正文
    val summaryTag: String,       // AI 生成的摘要标签，如「💥 激烈冲突」「💖 情感升温」
    val isSelected: Boolean = false,
    val isGenerating: Boolean = true,  // 生成中状态
    val error: String? = null
)

data class MultiBranchState(
    val branchCount: Int = 3,           // 可选 2/3/4
    val branches: List<BranchOption> = emptyList(),
    val selectedBranchIndex: Int = -1,
    val isGenerating: Boolean = false,
    val error: String? = null
) {
    val selectedBranch: BranchOption?
        get() = branches.getOrNull(selectedBranchIndex)
}
```

### 2.2 Prompt 变更 — 分支差异化

**文件**: `util/PromptBuilder.kt`（修改）

新增 `buildBranchContinuationPrompt()` 方法，在 system prompt 中注入 `branchHint`：

```kotlin
object PromptBuilder {
    // 新增
    fun buildBranchContinuationPrompt(
        currentContent: String,
        characters: String,
        worldSettings: String,
        branchIndex: Int,       // 0/1/2
        branchCount: Int,       // 总分支数
        lengthOption: LengthOption = LengthOption.MEDIUM
    ): List<AiMessage> {
        val hints = listOf(
            "续写时侧重剧情升级和冲突爆发，节奏加快",
            "续写时侧重角色情感互动，营造细腻氛围",
            "续写时引入意外转折或新角色，打破原有节奏"
            // 若 branchCount > 3，按需追加方向
        )
        val branchHint = hints.getOrElse(branchIndex) { hints.last() }

        val systemPrompt = buildSystemPrompt(characters, worldSettings, lengthOption)
            .replace(
                "请根据已有的内容，延续故事发展，保持文风一致。",
                "请根据已有的内容，延续故事发展，保持文风一致。\n\n【本次续写方向】$branchHint"
            )

        // ... 其余复用现有 buildUserContentForContinuation
    }

    // 摘要标签 Prompt（新增）
    fun buildBranchSummaryPrompt(branchContent: String): List<AiMessage> {
        return listOf(
            AiMessage(role = "system", content = """
                你是一个小说内容摘要专家。请为下面的小说续写内容生成一个简短有力的摘要标签。
                要求：
                1. 8个字以内
                2. 能概括这段内容的核心走向或氛围
                3. 使用 emoji + 文字的形式，如：「💥 激烈冲突」「💖 情感升温」「🔮 命运转折」
                4. 只输出标签，不要解释
            """.trimIndent()),
            AiMessage(role = "user", content = "小说续写内容：\n$branchContent")
        )
    }
}
```

> **Prompt 差异化原理**：每条分支共享相同的上下文（角色、世界观、历史内容），但 `branchHint` 给予 AI 明确的走向引导。`branchIndex` → `hints` 数组保证 2/3/4 条分支都有对应的方向。

---

### 2.3 新增 UseCase

**文件**: `domain/usecase/AiUseCases.kt`（修改 + 新增 UseCase）

```kotlin
class GenerateBranchesUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    /**
     * @param count 分支数量（2/3/4）
     * @return Flow<List<BranchOption>> 每完成一条分支就 emit 一次（渐进式 UI 更新）
     */
    operator fun invoke(
        currentContent: String,
        characters: String,
        worldSettings: String,
        userPrompt: String?,
        branchCount: Int,
        lengthOption: LengthOption
    ): Flow<List<BranchOption>> = flow {
        val messagesList = (0 until branchCount).map { i ->
            PromptBuilder.buildBranchContinuationPrompt(
                currentContent = currentContent,
                characters = characters,
                worldSettings = worldSettings,
                branchIndex = i,
                branchCount = branchCount,
                lengthOption = lengthOption
            )
        }

        // 并行启动所有分支的生成
        val deferreds = (0 until branchCount).map { i ->
            async {
                val fullContent = StringBuilder()
                var isFinished = false
                var error: String? = null

                aiRepository.continueStory(
                    prompt = userPrompt ?: "",
                    characters = characters,
                    worldSettings = worldSettings,
                    historyContent = currentContent,
                    lengthOption = lengthOption,
                    systemMessagesOverride = messagesList[i]  // 新增参数透传
                ).collect { response ->
                    if (response.error != null) {
                        error = response.error
                    } else if (response.isFinished) {
                        isFinished = true
                    } else {
                        fullContent.append(response.content)
                    }
                }

                val content = fullContent.toString()

                // 同步生成摘要标签（也可并行）
                val summaryTag = if (content.isNotBlank()) {
                    generateSummaryTag(content, aiRepository) ?: "📖 未知走向"
                } else null

                BranchOption(
                    index = i,
                    content = content,
                    summaryTag = summaryTag ?: "",
                    isGenerating = !isFinished && error == null,
                    error = error
                )
            }
        }

        // 逐条收集并 emit（最先完成的先 emit）
        deferreds.forEachIndexed { index, deferred ->
            val branch = deferred.await()
            val current = MutableStateFlow(branch)
            emit(current.toList().first()) // emit 单条完成结果
        }
    }

    private suspend fun generateSummaryTag(content: String, aiRepository: AiRepository): String? {
        val messages = PromptBuilder.buildBranchSummaryPrompt(content.take(500))
        var tag: String? = null
        aiRepository.continueStory(
            prompt = "", characters = "", worldSettings = "", historyContent = "",
            lengthOption = LengthOption.SHORT,
            systemMessagesOverride = messages
        ).collect { response ->
            if (!response.isFinished && response.error == null) {
                tag = response.content.trim()
            }
        }
        return tag
    }
}
```

> **替代方案（更简单）**：若不想改 `AiRepository` 接口，可以在调用 `continueStory` 前直接将 `branchHint` 作为 `userPrompt` 的一部分传入，复用现有接口，零接口变更。架构文档建议用后者。

**简化版（推荐，零接口变更）**：

```kotlin
class GenerateBranchesUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    operator fun invoke(
        currentContent: String,
        characters: String,
        worldSettings: String,
        userPrompt: String?,
        branchCount: Int,
        lengthOption: LengthOption
    ): Flow<List<BranchOption>> = flow {
        val directions = listOf(
            "请续写时侧重剧情升级和冲突爆发，节奏加快。",
            "请续写时侧重角色情感互动，营造细腻氛围。",
            "请续写时引入意外转折或新角色，打破原有节奏。",
            "请续写时探索角色的内心独白，深化人物塑造。"
        )

        val jobs = (0 until branchCount).map { i ->
            async {
                val direction = directions[i % directions.size]
                val enrichedPrompt = listOfNotNull(userPrompt, direction).joinToString("\n")
                val fullContent = StringBuilder()

                aiRepository.continueStory(
                    prompt = enrichedPrompt,
                    characters = characters,
                    worldSettings = worldSettings,
                    historyContent = currentContent,
                    lengthOption = lengthOption
                ).collect { response ->
                    if (!response.isFinished && response.error == null) {
                        fullContent.append(response.content)
                    }
                }

                val content = fullContent.toString()
                val summaryTag = generateSummaryTag(content)
                BranchOption(index = i, content = content, summaryTag = summaryTag)
            }
        }

        // 等待所有完成后再 emit 完整列表
        val branches = jobs.map { it.await() }
        emit(branches)
    }

    private suspend fun generateSummaryTag(content: String): String {
        // ... 同上，调用 summary prompt
    }
}
```

---

### 2.4 Repository 接口变更

**文件**: `domain/repository/AiRepository.kt`（无需变更）

推荐方案使用现有 `continueStory` 接口，将 `branchHint` 注入到 `userPrompt` 中，**零接口变更**。

如需更干净的方案，可在 `AiRepository` 中新增方法：

```kotlin
fun continueStoryWithSystem(
    systemMessages: List<AiMessage>,  // 支持注入 system prompt
    // ... 其余参数不变
): Flow<AiStreamResponse>
```

---

### 2.5 ViewModel 变更

**文件**: `ui/screens/writing/WritingViewModel.kt`（修改）

```kotlin
data class WritingUiState(
    // ... 现有字段
    val multiBranchState: MultiBranchState = MultiBranchState(),
    // 新增多分支底部弹窗开关
    val showMultiBranchSheet: Boolean = false,
)

// 新增 Event
sealed class WritingEvent {
    object ToggleMultiBranchSheet : WritingEvent()
    data class UpdateBranchCount(val count: Int) : WritingEvent()
    object GenerateBranches : WritingEvent()
    object CancelBranches : WritingEvent()
    data class SelectBranch(val index: Int) : WritingEvent()
    object AcceptBranch : WritingEvent()
    data class RegenerateBranch(val index: Int) : WritingEvent()
    object DismissBranches : WritingEvent()
}
```

在 `WritingViewModel.generateBranches()` 方法中注入 `GenerateBranchesUseCase`，收集 `Flow` 并更新 `multiBranchState`。

---

### 2.6 新增 BottomSheet UI

**文件**: `ui/screens/writing/MultiBranchBottomSheet.kt`（新建）

组件结构：

```
MultiBranchBottomSheet
├── Header（标题 + 关闭按钮）
├── BranchCountSelector（2/3/4 选项卡）
├── GenerateButton（开始生成）
├── LoadingIndicator（生成中，显示各分支进度）
└── BranchCard × N
    ├── SummaryTag（摘要标签，e.g. "💥 激烈冲突"）
    ├── ContentText（正文预览，maxLines = 5）
    ├── AcceptButton（采纳此分支）
    └── RegenerateButton（重写此分支）
```

设计参考：
- 卡片使用 `Surface` + `RoundedCornerShape(12.dp)` + `border`
- 选中态：`border(2.dp, primary)` + `primaryContainer` 背景
- 加载中：每张卡片显示 `CircularProgressIndicator`
- 可用 `LazyColumn` 排列多张卡片，maxHeight = 500.dp

---

### 2.7 WritingScreen 变更

**文件**: `ui/screens/writing/WritingScreen.kt`（修改）

在「续写」按钮区新增「多分支」按钮：

```kotlin
Row {
    Button(onClick = { onEvent(WritingEvent.GenerateContinue) }) {
        Text("续写")
    }
    Spacer(modifier = Modifier.width(8.dp))
    OutlinedButton(onClick = { onEvent(WritingEvent.ToggleMultiBranchSheet) }) {
        Text("多分支")
    }
}
```

在 `WritingScreen` 根 `Box` 中叠加显示 `MultiBranchBottomSheet`：

```kotlin
if (uiState.showMultiBranchSheet) {
    MultiBranchBottomSheet(
        multiBranchState = uiState.multiBranchState,
        onCountChanged = { onEvent(WritingEvent.UpdateBranchCount(it)) },
        onGenerate = { onEvent(WritingEvent.GenerateBranches) },
        onCancel = { onEvent(WritingEvent.CancelBranches) },
        onBranchSelected = { onEvent(WritingEvent.SelectBranch(it)) },
        onAccept = { onEvent(WritingEvent.AcceptBranch) },
        onRegenerate = { onEvent(WritingEvent.RegenerateBranch(it)) },
        onDismiss = { onEvent(WritingEvent.DismissBranches) },
        sheetState = bottomSheetState
    )
}
```

---

## 三、完整流程图

```
用户点击「多分支」
       ↓
显示 MultiBranchBottomSheet，用户选择分支数（2/3/4）
       ↓
用户点击「开始生成」
       ↓
GenerateBranchesUseCase.invoke()
  ├─ for i in 0..<branchCount: async { continueStory(branchHint_i) }
  │    └─ branchHint_i 由 PromptBuilder 根据 index 注入差异化方向
  └─ Flow.emit(List<BranchOption>)
       ↓
WritingViewModel 收集 Flow，更新 multiBranchState
       ↓
MultiBranchBottomSheet 渲染 BranchCard 列表
  ├─ 用户点击「采纳」→ 替换正文内容，关闭 Sheet
  └─ 用户点击「重写」→ 单独重写该分支（REGENERATE 单支）
```

---

## 四、文件变更清单

| 操作 | 文件路径 |
|------|---------|
| 新增 | `domain/model/BranchOption.kt` |
| 修改 | `util/PromptBuilder.kt` — 新增 `buildBranchContinuationPrompt` + `buildBranchSummaryPrompt` |
| 修改 | `domain/usecase/AiUseCases.kt` — 新增 `GenerateBranchesUseCase` |
| 修改 | `domain/repository/AiRepository.kt` — 零变更（推荐方案）或新增 `continueStoryWithSystem` |
| 修改 | `ui/screens/writing/WritingViewModel.kt` — 新增状态 + 事件处理 |
| 新增 | `ui/screens/writing/MultiBranchBottomSheet.kt` |
| 修改 | `ui/screens/writing/WritingScreen.kt` — 入口按钮 + Sheet 叠加 |

---

## 五、风险 & 注意事项

1. **Token 消耗**：3 条分支 = 3 倍 token 消耗，需在 UI 上有明确提示
2. **并行限流**：若 AI API 有 QPS 限制，`async` 并发数需控制；可用 `Dispatchers.IO` + semaphore 限流
3. **单支重写**：需在 `GenerateBranchesUseCase` 中支持单独重写指定分支（传入 `branchIndex`），其余分支不变
4. **流式进度**：建议 `Flow` 在每条分支完成时立即 `emit`（见简化版注释），实现渐进式卡片展示
5. **摘要标签生成**：可与正文并行调用同一 AI 接口生成标签，额外 token 消耗极少
