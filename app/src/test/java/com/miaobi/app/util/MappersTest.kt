package com.miaobi.app.util

import com.miaobi.app.data.local.entity.*
import com.miaobi.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class MappersTest {

    // ===== Story mappers =====

    @Test
    fun `StoryEntity toDomain maps all fields`() {
        val entity = StoryEntity(
            id = 1L,
            title = "标题",
            description = "描述",
            templateType = "fantasy",
            coverImage = "cover.jpg",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals("标题", domain.title)
        assertEquals("描述", domain.description)
        assertEquals("fantasy", domain.templateType)
        assertEquals("cover.jpg", domain.coverImage)
        assertEquals(1000L, domain.createdAt)
        assertEquals(2000L, domain.updatedAt)
    }

    @Test
    fun `Story toEntity maps all fields`() {
        val domain = Story(
            id = 1L,
            title = "标题",
            description = "描述",
            templateType = "fantasy",
            coverImage = "cover.jpg",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals("标题", entity.title)
        assertEquals("描述", entity.description)
        assertEquals("fantasy", entity.templateType)
        assertEquals("cover.jpg", entity.coverImage)
        assertEquals(1000L, entity.createdAt)
        assertEquals(2000L, entity.updatedAt)
    }

    // ===== Chapter mappers =====

    @Test
    fun `ChapterEntity toDomain maps all fields`() {
        val entity = ChapterEntity(
            id = 1L,
            storyId = 2L,
            title = "第一章",
            content = "内容",
            orderIndex = 0,
            wordCount = 100,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals(2L, domain.storyId)
        assertEquals("第一章", domain.title)
        assertEquals("内容", domain.content)
        assertEquals(0, domain.orderIndex)
        assertEquals(100, domain.wordCount)
    }

    @Test
    fun `Chapter toEntity maps all fields`() {
        val domain = Chapter(
            id = 1L,
            storyId = 2L,
            title = "第一章",
            content = "内容",
            orderIndex = 0,
            wordCount = 100,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals(2L, entity.storyId)
        assertEquals("第一章", entity.title)
        assertEquals("内容", entity.content)
        assertEquals(0, entity.orderIndex)
        assertEquals(100, entity.wordCount)
    }

    // ===== Character mappers =====

    @Test
    fun `CharacterEntity toDomain maps all fields`() {
        val entity = CharacterEntity(
            id = 1L,
            storyId = 2L,
            name = "小明",
            description = "主角",
            avatar = "avatar.jpg",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals(2L, domain.storyId)
        assertEquals("小明", domain.name)
        assertEquals("主角", domain.description)
        assertEquals("avatar.jpg", domain.avatar)
    }

    @Test
    fun `Character toEntity maps all fields`() {
        val domain = Character(
            id = 1L,
            storyId = 2L,
            name = "小明",
            description = "主角",
            avatar = "avatar.jpg",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals(2L, entity.storyId)
        assertEquals("小明", entity.name)
        assertEquals("主角", entity.description)
        assertEquals("avatar.jpg", entity.avatar)
    }

    // ===== WorldSetting mappers =====

    @Test
    fun `WorldSettingEntity toDomain maps all fields`() {
        val entity = WorldSettingEntity(
            id = 1L,
            storyId = 2L,
            name = "世界观",
            content = "设定内容",
            category = "magic",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals(2L, domain.storyId)
        assertEquals("世界观", domain.name)
        assertEquals("设定内容", domain.content)
        assertEquals("magic", domain.category)
    }

    @Test
    fun `WorldSetting toEntity maps all fields`() {
        val domain = WorldSetting(
            id = 1L,
            storyId = 2L,
            name = "世界观",
            content = "设定内容",
            category = "magic",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals(2L, entity.storyId)
        assertEquals("世界观", entity.name)
        assertEquals("设定内容", entity.content)
        assertEquals("magic", entity.category)
    }

    // ===== ChapterDraft mappers =====

    @Test
    fun `ChapterDraftEntity toDomain maps all fields`() {
        val entity = ChapterDraftEntity(
            id = 1L,
            chapterId = 2L,
            content = "草稿内容",
            wordCount = 3,
            version = 5,
            createdAt = 1000L
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals(2L, domain.chapterId)
        assertEquals("草稿内容", domain.content)
        assertEquals(3, domain.wordCount)
        assertEquals(5, domain.version)
    }

    @Test
    fun `ChapterDraft toEntity maps all fields`() {
        val domain = ChapterDraft(
            id = 1L,
            chapterId = 2L,
            content = "草稿内容",
            wordCount = 3,
            version = 5,
            createdAt = 1000L
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals(2L, entity.chapterId)
        assertEquals("草稿内容", entity.content)
        assertEquals(3, entity.wordCount)
        assertEquals(5, entity.version)
    }

    // ===== ChapterDraftVersion mappers =====

    @Test
    fun `ChapterDraftVersionEntity toDomain maps all fields`() {
        val entity = ChapterDraftVersionEntity(
            id = 1L,
            draftId = 2L,
            version = 3,
            content = "版本内容",
            wordCount = 100,
            diffSummary = "新增了100字",
            createdAt = 1000L
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals(2L, domain.draftId)
        assertEquals(3, domain.version)
        assertEquals("版本内容", domain.content)
        assertEquals(100, domain.wordCount)
        assertEquals("新增了100字", domain.diffSummary)
        assertEquals(1000L, domain.createdAt)
    }

    @Test
    fun `ChapterDraftVersion toEntity maps all fields`() {
        val domain = ChapterDraftVersion(
            id = 1L,
            draftId = 2L,
            version = 3,
            content = "版本内容",
            wordCount = 100,
            diffSummary = "新增了100字",
            createdAt = 1000L
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals(2L, entity.draftId)
        assertEquals(3, entity.version)
        assertEquals("版本内容", entity.content)
        assertEquals(100, entity.wordCount)
        assertEquals("新增了100字", entity.diffSummary)
        assertEquals(1000L, entity.createdAt)
    }

    // ===== StoryTemplate mappers =====

    @Test
    fun `StoryTemplateEntity toDomain maps all fields`() {
        val entity = StoryTemplateEntity(
            id = 1L,
            title = "修仙模板",
            genre = "xianxia",
            summary = "修仙世界故事",
            charactersJson = """[{"name":"主角","description":"天才"}]""",
            worldSettingsJson = """[{"name":"境界","content":"炼气","category":"cultivation"}]""",
            promptTemplate = "请续写这个故事",
            coverImage = "cover.jpg",
            isBuiltIn = true,
            createdAt = 1000L
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals("修仙模板", domain.title)
        assertEquals("xianxia", domain.genre)
        assertEquals("修仙世界故事", domain.summary)
        assertTrue(domain.charactersJson?.contains("主角") == true)
        assertTrue(domain.worldSettingsJson?.contains("境界") == true)
        assertEquals("请续写这个故事", domain.promptTemplate)
        assertEquals("cover.jpg", domain.coverImage)
        assertTrue(domain.isBuiltIn)
        assertEquals(1000L, domain.createdAt)
    }

    @Test
    fun `StoryTemplate toEntity maps all fields`() {
        val domain = StoryTemplate(
            id = 1L,
            title = "都市模板",
            genre = "urban",
            summary = "都市故事",
            charactersJson = """[{"name":"女主","description":"白领"}]""",
            worldSettingsJson = """[{"name":"公司","content":"大公司","category":"corporate"}]""",
            promptTemplate = "续写都市生活",
            coverImage = "urban.jpg",
            isBuiltIn = false,
            createdAt = 2000L
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals("都市模板", entity.title)
        assertEquals("urban", entity.genre)
        assertEquals("都市故事", entity.summary)
        assertTrue(entity.charactersJson?.contains("女主") == true)
        assertTrue(entity.worldSettingsJson?.contains("公司") == true)
        assertEquals("续写都市生活", entity.promptTemplate)
        assertEquals("urban.jpg", entity.coverImage)
        assertFalse(entity.isBuiltIn)
        assertEquals(2000L, entity.createdAt)
    }

    // ===== Round-trip tests =====

    @Test
    fun `Story round-trip preserves data`() {
        val original = Story(
            id = 1L,
            title = "标题",
            description = "描述",
            templateType = "fantasy",
            coverImage = "cover.jpg",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = original.toEntity()
        val result = entity.toDomain()

        assertEquals(original, result)
    }

    @Test
    fun `Chapter round-trip preserves data`() {
        val original = Chapter(
            id = 1L,
            storyId = 2L,
            title = "第一章",
            content = "内容",
            orderIndex = 0,
            wordCount = 100,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = original.toEntity()
        val result = entity.toDomain()

        assertEquals(original, result)
    }

    @Test
    fun `Character round-trip preserves data`() {
        val original = Character(
            id = 1L,
            storyId = 2L,
            name = "小明",
            description = "主角",
            avatar = "avatar.jpg",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = original.toEntity()
        val result = entity.toDomain()

        assertEquals(original, result)
    }

    @Test
    fun `WorldSetting round-trip preserves data`() {
        val original = WorldSetting(
            id = 1L,
            storyId = 2L,
            name = "世界观",
            content = "设定内容",
            category = "magic",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = original.toEntity()
        val result = entity.toDomain()

        assertEquals(original, result)
    }

    @Test
    fun `ChapterDraft round-trip preserves data`() {
        val original = ChapterDraft(
            id = 1L,
            chapterId = 2L,
            content = "草稿内容",
            wordCount = 3,
            version = 5,
            createdAt = 1000L
        )

        val entity = original.toEntity()
        val result = entity.toDomain()

        assertEquals(original, result)
    }

    @Test
    fun `ChapterDraftVersion round-trip preserves data`() {
        val original = ChapterDraftVersion(
            id = 1L,
            draftId = 2L,
            version = 3,
            content = "版本内容",
            wordCount = 100,
            diffSummary = "新增了100字",
            createdAt = 1000L
        )

        val entity = original.toEntity()
        val result = entity.toDomain()

        assertEquals(original, result)
    }

    @Test
    fun `StoryTemplate round-trip preserves data`() {
        val original = StoryTemplate(
            id = 1L,
            title = "修仙模板",
            genre = "xianxia",
            summary = "修仙世界故事",
            charactersJson = """[{"name":"主角","description":"天才"}]""",
            worldSettingsJson = """[{"name":"境界","content":"炼气","category":"cultivation"}]""",
            promptTemplate = "请续写这个故事",
            coverImage = "cover.jpg",
            isBuiltIn = true,
            createdAt = 1000L
        )

        val entity = original.toEntity()
        val result = entity.toDomain()

        assertEquals(original, result)
    }
}
