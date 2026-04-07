# 妙笔 AI 写小说 — 润色改写功能 架构变更文档

## 一、概述

润色改写（Rewrite）是指用户选中一段文字后，由 AI 将其改写成不同风格版本供选择。
核心复用已有 SSE 流式输出、PromptBuilder、AiRepository 架构，**不做重复建设**。

---

## 二、代码模块变更

### 2.1 新增模块

| 文件 | 职责 |
|------|------|
| `domain/model/RewriteStyle.kt` | 风格枚举（6种）+ RewriteVersion 数据类 |
| `domain/model/RewriteResult.kt` | 改写结果（含多版本 + 当前选中版本） |
| `domain/repository/AiRepository.kt` | 新增 `rewriteText()` 声明 |
| `domain/usecase/AiUseCases.kt` | 新增 `RewriteTextUseCase` |
| `util/PromptBuilder.kt` | 新增 `buildRewritePrompt()` |
| `data/repository/AiRepositoryImpl.kt` | 实现 `rewriteText()` |
| `ui/screens/writing/WritingViewModel.kt` | 新增改写相关 State + Event + 处理逻辑 |
| `ui/screens/writing/WritingScreen.kt` | 新增改写 BottomSheet UI |
| `ui/screens/writing/RewriteBottomSheet.kt` | **新增** 独立的润色改写弹窗组件 |

### 2.2 修改模块

| 文件 | 变更说明 |
|------|------|
| `WritingUiState` | 新增 `rewriteState: RewriteState` |
| `WritingEvent` | 新增 `TriggerRewrite`、`AcceptRewrite` 等事件 |
| `AiRepository` | 新增 `rewriteText()` 方法签名 |
| `AiUseCases` | 新增 `RewriteTextUseCase` |
| `PromptBuilder` | 新增 `RewriteStyle` 和对应 Prompt 模板 |

---

## 三、Prompt 变更设计

### 3.1 新增 RewriteStyle 枚举

```kotlin
enum class RewriteStyle(val label: String, val prompt: String) {
    CLASSICAL("古风", "请将以下文字改写成古风文言风格……"),
    MODERN("现代", "请将以下文字改写成现代白话风格……"),
    CONCISE("简洁", "请将以下文字用简洁干练的语言改写……"),
    FLOWERY("华丽", "请将以下文字用华丽优美的语言改写……"),
    COLLOQUIAL("口语化", "请将以下文字改写成自然口语化的表达……"),
    LITERARY("文艺", "请将以下文字用文艺清新的语言改写……")
}
```

### 3.2 PromptBuilder.buildRewritePrompt()

系统 Prompt 要求：
- 角色：专业文学编辑
- 输入：原文 + 目标风格
- 输出：**3个不同版本**（用 `---VERSION_DELIMITER---` 分隔）
- 禁用解释，直接输出改写内容

---

## 四、UI 变更说明

### 4.1 触发方式
- 写作界面长按/选中文字 → 浮动工具栏出现「✨ 润色」按钮
- 点击后弹出 `RewriteBottomSheet`

### 4.2 RewriteBottomSheet 内容
```
┌─────────────────────────────────┐
│  润色改写                    ✕   │
│─────────────────────────────────│
│  原文：                         │
│  "他缓缓地走进了那间古老的房间" │
│                                 │
│  风格：[古风][现代][简洁][华丽] │
│        [口语化][文艺]           │
│                                 │
│  [改写]                         │
│─────────────────────────────────│
│  版本 1 ✓（当前选中）           │
│  "他缓步迈入那沧桑古宅"         │
│  [采纳] [编辑]                  │
│─────────────────────────────────│
│  版本 2                         │
│  "他慢慢走进那间老房子"         │
│  [采纳] [编辑]                  │
│─────────────────────────────────│
│  版本 3                         │
│  "他踱步入了那沧桑古旧的房间"   │
│  [采纳] [编辑]                  │
│                                 │
│  [再改一版]                     │
└─────────────────────────────────┘
```

### 4.3 采纳/编辑/再改逻辑
- **采纳**：将选中版本内容替换原文，关闭 Sheet
- **编辑**：弹出内联编辑器，允许用户手动修改后再插入
- **再改一版**：用当前选中版本重新请求，替换版本列表

---

## 五、状态设计

### RewriteState（嵌入 WritingUiState）

```kotlin
data class RewriteState(
    val selectedText: String = "",      // 用户选中的原文
    val selectedStyle: RewriteStyle = RewriteStyle.MODERN,
    val isRewriting: Boolean = false,
    val versions: List<RewriteVersion> = emptyList(),  // 最多3个
    val selectedVersionIndex: Int = 0,
    val isEditing: Boolean = false,
    val editingText: String = "",
    val error: String? = null
)

data class RewriteVersion(
    val index: Int,
    val content: String,
    val isSelected: Boolean
)
```

---

## 六、关键设计决策

1. **复用现有 SSE 流式输出**：AiSseClient 不改，AiRepositoryImpl 新增 `rewriteText()` flow 方法，复用 `streamChat` 路径
2. **服务端多版本解析**：Prompt 要求模型输出分隔符，客户端按分隔符 split 得到多版本；流式过程中拼接 content，遇到分隔符则切割出完整版本
3. **不改已有续写流程**：润色改写是独立功能，与 `continueStory` 完全解耦
4. **文本替换策略**：采纳时直接调用 `WritingEvent.UpdateContent` 替换原文中选中段落
