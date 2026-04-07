# 妙笔 AI 写小说 App — 架构变更文档

**版本：** v2.0（迭代规划）
**作者：** 架构 Agent
**日期：** 2026-04-07

---

## 一、概述

本轮迭代新增三大功能：
1. **P0 - 内置 API Key**：内置默认 Key，支持切换自定义 Key
2. **P1 - 故事模板**：10 个预设模板，创建书籍时可选
3. **P1 - 续写历史版本**：每章节最多 20 版本，支持查看/恢复/对比/删除

---

## 二、数据库变更

### 2.1 新增表

#### `story_templates` 表

```sql
CREATE TABLE story_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,                    -- 模板名称，如"都市重生"
    genre TEXT NOT NULL,                    -- 题材：urban/fantasy/mystery/sci-fi/romance
    summary TEXT NOT NULL,                  -- 模板简介
    characters_json TEXT,                   -- 预设角色 JSON
    world_settings_json TEXT,               -- 预设世界观 JSON
    prompt_template TEXT NOT NULL,          -- AI 续写 Prompt 模板
    cover_image TEXT,                        -- 封面图（可选）
    is_built_in INTEGER DEFAULT 1,          -- 是否内置模板
    created_at INTEGER DEFAULT (strftime('%s', 'now'))
);
```

#### `chapter_draft_versions` 表（新表，替代原 `chapter_drafts` 部分职责）

> 原 `chapter_drafts` 表**结构调整**：去除 `version` 字段（version 由新表 `chapter_draft_versions` 管理）。

```sql
CREATE TABLE chapter_draft_versions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    draft_id INTEGER NOT NULL,              -- 关联 chapter_drafts.id
    version INTEGER NOT NULL,                -- 版本号（1~20）
    content TEXT NOT NULL,                  -- 该版本内容
    word_count INTEGER DEFAULT 0,
    diff_summary TEXT,                       -- 与上一版本的差异摘要（可选）
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (draft_id) REFERENCES chapter_drafts(id) ON DELETE CASCADE
);

CREATE INDEX idx_draft_versions_draft_id ON chapter_draft_versions(draft_id);
```

#### `chapter_drafts` 表调整

```sql
-- 新增 is_current 字段，标识当前生效版本
ALTER TABLE chapter_drafts ADD COLUMN is_current INTEGER DEFAULT 1;
```

---

### 2.2 数据库版本变更

```
version: 1 → 2
```

迁移策略：使用 Room `Migration` 或 `fallbackToDestructiveMigration`（开发阶段）。

---

## 三、关键模块变更

### 3.1 内置 API Key（P0）

#### 影响范围

| 层级 | 文件 | 变更类型 |
|------|------|----------|
| Domain/Model | `AiConfig.kt` | 新增字段 |
| Data/Datastore | `SettingsManager.kt` | 新增 `useBuiltInApiKey` preference |
| Data/Repository | `AiRepositoryImpl.kt` | 读取 DataStore 决定使用哪个 Key |
| UI | `SettingsViewModel.kt` | 新增切换开关逻辑 |
| UI | `SettingsScreen.kt` | 新增 UI 控件 |

#### 变更详情

**Domain Model — `AiConfig.kt`**
```kotlin
data class AiConfig(
    val apiKey: String = "",
    val apiUrl: String = "https://api.siliconflow.cn/v1",
    val modelName: String = "Qwen/Qwen2.5-7B-Instruct",
    val useBuiltInKey: Boolean = true,   // [新增] 默认使用内置 Key
    val builtInApiKey: String = "sk-xxxxx" // [新增] 内置 Key（硬编码或加密存储）
)
```

**DataStore Preferences — `SettingsManager.kt`**
```kotlin
private object PreferencesKeys {
    val USE_BUILT_IN_KEY = booleanPreferencesKey("use_built_in_api_key")
    // 原 API_KEY now stores user's custom key (only when useBuiltInKey = false)
}
```

**AiRepositoryImpl 逻辑调整**
```
if (config.useBuiltInKey) → 使用 config.builtInApiKey
else → 使用 config.apiKey (用户自定义)
```

---

### 3.2 故事模板（P1）

#### 影响范围

| 层级 | 新增文件 | 职责 |
|------|----------|------|
| Data/Entity | `StoryTemplateEntity.kt` | 模板数据实体 |
| Data/DAO | `StoryTemplateDao.kt` | 模板 CRUD |
| Data/Repository | `StoryTemplateRepositoryImpl.kt` | 模板仓库实现 |
| Domain/Repository | `StoryTemplateRepository.kt` | 模板仓库接口 |
| Domain/Model | `StoryTemplate.kt` | 领域模型 |
| Domain/UseCase | `TemplateUseCases.kt` | 模板用例（获取列表/按类型查询/应用模板） |
| UI | `BookshelfViewModel.kt` | 创建书籍时加载模板列表 |
| UI | `BookshelfScreen.kt` | 模板选择 UI |

#### 模板数据结构（10 个内置）

| 题材 | genre | 说明 |
|------|-------|------|
| 都市重生 | `urban` | 现代都市，主角重生逆天改命 |
| 玄幻修仙 | `fantasy` | 东方玄幻，修炼飞升 |
| 悬疑推理 | `mystery` | 烧脑剧情，破案解谜 |
| 科幻星际 | `sci-fi` | 未来科技，星际探索 |
| 言情甜宠 | `romance` | 现代言情，甜蜜恋爱 |

（每种题材 2 个模板，共 10 个）

#### 关键逻辑

**应用模板创建书籍时：**
1. 根据模板的 `characters_json` 创建 CharacterEntity 记录
2. 根据 `world_settings_json` 创建 WorldSettingEntity 记录
3. 将 `prompt_template` 存入 StoryEntity 或单独关联

---

### 3.3 续写历史版本（P1）

#### 影响范围

| 层级 | 文件 | 变更类型 |
|------|------|----------|
| Data/Entity | `ChapterDraftEntity.kt` | 新增 `is_current` 字段 |
| Data/Entity | `ChapterDraftVersionEntity.kt` | **新增** 独立版本实体 |
| Data/DAO | `ChapterDraftDao.kt` | 新增版本管理方法 |
| Data/DAO | `ChapterDraftVersionDao.kt` | **新增** 版本 CRUD |
| Data/Repository | `ChapterDraftRepositoryImpl.kt` | 调整保存逻辑，版本上限 20 |
| Domain/Model | `ChapterDraft.kt` | 新增 `isCurrent` 字段 |
| Domain/Model | `ChapterDraftVersion.kt` | **新增** 版本领域模型 |
| Domain/Repository | `ChapterDraftRepository.kt` | 接口调整 |
| Domain/UseCase | `ChapterUseCases.kt` | 新增版本对比方法 |
| UI | `WritingViewModel.kt` | 新增对比视图状态 |
| UI | `WritingScreen.kt` | 版本历史 UI（含对比视图） |

#### 版本保存逻辑（ChapterDraftRepositoryImpl）

```
每次 saveDraft(chapterId, content):
1. 获取当前版本数 → COUNT WHERE draft_id = current_draft_id
2. IF count >= 20:
   - 删除最早的版本（MIN(version)）
3. 新增 version 记录到 chapter_draft_versions
4. 更新 chapter_drafts.is_current = 0（旧稿标记为非当前）
5. 创建/更新 chapter_drafts 新记录，is_current = 1
```

#### 对比功能

**新增 Diff 计算 UseCase：**
```kotlin
class CompareDraftVersionsUseCase {
    operator fun invoke(versionA: ChapterDraftVersion, versionB: ChapterDraftVersion): String {
        // 使用简单行级 diff 或第三方库（如 java-diff-utils）
        // 返回格式化的差异文本
    }
}
```

---

## 四、目录结构变更

```
app/src/main/java/com/miaobi/app/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── StoryTemplateDao.kt      [新增]
│   │   │   └── ChapterDraftVersionDao.kt [新增]
│   │   └── entity/
│   │       ├── StoryTemplateEntity.kt   [新增]
│   │       └── ChapterDraftVersionEntity.kt [新增]
│   └── repository/
│       ├── StoryTemplateRepositoryImpl.kt [新增]
│       └── ChapterDraftRepositoryImpl.kt   [调整]
├── domain/
│   ├── model/
│   │   ├── StoryTemplate.kt             [新增]
│   │   └── ChapterDraftVersion.kt        [新增]
│   ├── repository/
│   │   ├── StoryTemplateRepository.kt   [新增]
│   │   └── ChapterDraftRepository.kt     [调整]
│   └── usecase/
│       ├── TemplateUseCases.kt          [新增]
│       ├── ChapterUseCases.kt            [调整]
│       └── CompareDraftVersionsUseCase.kt [新增]
├── ui/
│   └── screens/
│       └── settings/
│           └── SettingsViewModel.kt      [调整]
└── util/
    └── SettingsManager.kt               [调整]
```

**新增文件（6 个）：**
- `StoryTemplateEntity.kt`
- `StoryTemplateDao.kt`
- `StoryDraftVersionEntity.kt`
- `StoryDraftVersionDao.kt`
- `StoryTemplateRepositoryImpl.kt`
- `ChapterDraftVersion.kt`
- `StoryTemplate.kt`
- `StoryTemplateRepository.kt`
- `TemplateUseCases.kt`
- `CompareDraftVersionsUseCase.kt`

---

## 五、架构变更说明

### 5.1 架构风格
保持现有 **Clean Architecture + MVVM**，各层职责不变，新增内容按层归位。

### 5.2 关键设计决策

| 决策点 | 方案 | 理由 |
|--------|------|------|
| 内置 Key 存储 | 硬编码或 Base64 混淆 | 防止轻易泄露，可后续升级为服务器下发 |
| 模板数据 | Room 预填充 + 内置 JSON | 10 个模板可在 App 首次启动时写入数据库 |
| 版本上限 | 数据库层强制清理 | 避免存储膨胀，保证 UI 响应 |
| 对比方案 | 行级 diff 输出文本 | 避免引入过大 UI 库，轻量化实现 |

### 5.3 向后兼容性

- `StoryEntity.templateType` 现有值 `"free"` 保留，映射为"自由创作"模板
- 现有草稿数据通过 Migration 自动迁移，`is_current = 1`

### 5.4 测试策略

- 新增 `StoryTemplateRepositoryImplTest.kt`
- `ChapterDraftRepositoryImplTest.kt` 补充版本上限测试
- `CompareDraftVersionsUseCaseTest.kt` 新增 diff 算法测试

---

## 六、实施顺序建议

```
Phase 1（P0）: 内置 API Key
  ↓
Phase 2（P1）: 故事模板
  ↓
Phase 3（P1）: 续写历史版本（含对比）
```

P0 最简实现可只改 DataStore + AiRepositoryImpl，P1 模板和 P1 版本可并行开发。
