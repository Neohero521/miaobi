package com.miaobi.app.util

import com.miaobi.app.data.local.entity.*
import com.miaobi.app.domain.model.*

// Story mappers
fun StoryEntity.toDomain(): Story = Story(
    id = id,
    title = title,
    description = description,
    templateType = templateType,
    coverImage = coverImage,
    wordCount = wordCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Story.toEntity(): StoryEntity = StoryEntity(
    id = id,
    title = title,
    description = description,
    templateType = templateType,
    coverImage = coverImage,
    wordCount = wordCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// Chapter mappers
fun ChapterEntity.toDomain(): Chapter = Chapter(
    id = id,
    storyId = storyId,
    title = title,
    content = content,
    orderIndex = orderIndex,
    wordCount = wordCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Chapter.toEntity(): ChapterEntity = ChapterEntity(
    id = id,
    storyId = storyId,
    title = title,
    content = content,
    orderIndex = orderIndex,
    wordCount = wordCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// Character mappers
fun CharacterEntity.toDomain(): Character = Character(
    id = id,
    storyId = storyId,
    name = name,
    description = description,
    avatar = avatar,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Character.toEntity(): CharacterEntity = CharacterEntity(
    id = id,
    storyId = storyId,
    name = name,
    description = description,
    avatar = avatar,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// WorldSetting mappers
fun WorldSettingEntity.toDomain(): WorldSetting = WorldSetting(
    id = id,
    storyId = storyId,
    name = name,
    content = content,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WorldSetting.toEntity(): WorldSettingEntity = WorldSettingEntity(
    id = id,
    storyId = storyId,
    name = name,
    content = content,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ChapterDraft mappers
fun ChapterDraftEntity.toDomain(): ChapterDraft = ChapterDraft(
    id = id,
    chapterId = chapterId,
    content = content,
    wordCount = wordCount,
    version = version,
    isCurrent = isCurrent,
    createdAt = createdAt
)

fun ChapterDraft.toEntity(): ChapterDraftEntity = ChapterDraftEntity(
    id = id,
    chapterId = chapterId,
    content = content,
    wordCount = wordCount,
    version = version,
    isCurrent = isCurrent,
    createdAt = createdAt
)

// ChapterDraftVersion mappers
fun ChapterDraftVersionEntity.toDomain(): ChapterDraftVersion = ChapterDraftVersion(
    id = id,
    draftId = draftId,
    version = version,
    content = content,
    wordCount = wordCount,
    diffSummary = diffSummary,
    createdAt = createdAt
)

fun ChapterDraftVersion.toEntity(): ChapterDraftVersionEntity = ChapterDraftVersionEntity(
    id = id,
    draftId = draftId,
    version = version,
    content = content,
    wordCount = wordCount,
    diffSummary = diffSummary,
    createdAt = createdAt
)

// StoryTemplate mappers
fun StoryTemplateEntity.toDomain(): StoryTemplate = StoryTemplate(
    id = id,
    title = title,
    genre = genre,
    summary = summary,
    charactersJson = charactersJson,
    worldSettingsJson = worldSettingsJson,
    promptTemplate = promptTemplate,
    coverImage = coverImage,
    isBuiltIn = isBuiltIn,
    createdAt = createdAt
)

fun StoryTemplate.toEntity(): StoryTemplateEntity = StoryTemplateEntity(
    id = id,
    title = title,
    genre = genre,
    summary = summary,
    charactersJson = charactersJson,
    worldSettingsJson = worldSettingsJson,
    promptTemplate = promptTemplate,
    coverImage = coverImage,
    isBuiltIn = isBuiltIn,
    createdAt = createdAt
)
