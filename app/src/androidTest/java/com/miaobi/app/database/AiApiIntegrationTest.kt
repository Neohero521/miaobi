package com.miaobi.app.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.miaobi.app.data.local.dao.StoryDao
import com.miaobi.app.data.local.dao.ChapterDao
import com.miaobi.app.data.local.dao.CharacterDao
import com.miaobi.app.data.local.dao.WorldSettingDao
import com.miaobi.app.data.local.dao.ChapterDraftDao
import com.miaobi.app.data.local.entity.StoryEntity
import com.miaobi.app.data.local.entity.ChapterEntity
import com.miaobi.app.data.local.entity.CharacterEntity
import com.miaobi.app.data.local.entity.WorldSettingEntity
import com.miaobi.app.data.local.entity.ChapterDraftEntity
import com.miaobi.app.data.local.database.MiaobiDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*

/**
 * Integration tests for AI-related data flow through the database.
 * These tests verify the complete data path from domain to database.
 */
@RunWith(AndroidJUnit4::class)
class AiApiIntegrationTest {

    private lateinit var database: MiaobiDatabase
    private lateinit var storyDao: StoryDao
    private lateinit var chapterDao: ChapterDao
    private lateinit var characterDao: CharacterDao
    private lateinit var worldSettingDao: WorldSettingDao
    private lateinit var chapterDraftDao: ChapterDraftDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MiaobiDatabase::class.java
        ).build()

        storyDao = database.storyDao()
        chapterDao = database.chapterDao()
        characterDao = database.characterDao()
        worldSettingDao = database.worldSettingDao()
        chapterDraftDao = database.chapterDraftDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    /**
     * End-to-end test: Create story with characters and world settings,
     * then write chapters and save drafts.
     * This simulates the complete AI writing workflow.
     */
    @Test
    fun `complete story writing workflow with drafts`() = runTest {
        // Step 1: Create a story
        val storyId = storyDao.insertStory(
            StoryEntity(
                title = "我的奇幻小说",
                description = "一个关于冒险的故事",
                templateType = "fantasy"
            )
        )

        // Step 2: Add characters
        val char1Id = characterDao.insertCharacter(
            CharacterEntity(
                storyId = storyId,
                name = "小明",
                description = "勇敢的年轻剑士"
            )
        )
        val char2Id = characterDao.insertCharacter(
            CharacterEntity(
                storyId = storyId,
                name = "小红",
                description = "聪明的魔法师"
            )
        )

        // Step 3: Add world settings
        val world1Id = worldSettingDao.insertWorldSetting(
            WorldSettingEntity(
                storyId = storyId,
                name = "魔法体系",
                content = "这个世界的魔法基于元素之力",
                category = "magic"
            )
        )
        worldSettingDao.insertWorldSetting(
            WorldSettingEntity(
                storyId = storyId,
                name = "大陆",
                content = "阿尔特兰大陆",
                category = "location"
            )
        )

        // Step 4: Create first chapter
        val chapter1Id = chapterDao.insertChapter(
            ChapterEntity(
                storyId = storyId,
                title = "第一章：出发",
                content = "小明站在村庄门口，望着远方的山脉。",
                orderIndex = 0
            )
        )

        // Step 5: Save AI-generated continuation as draft
        val draft1Id = chapterDraftDao.insertDraft(
            ChapterDraftEntity(
                chapterId = chapter1Id,
                content = "他听说山那边有一座古老的城堡，传说中藏有无尽的宝藏。",
                wordCount = 12,
                version = 1
            )
        )

        // Step 6: Update chapter content with accepted draft
        chapterDao.updateContent(chapter1Id, "小明站在村庄门口，望着远方的山脉。\n他听说山那边有一座古老的城堡，传说中藏有无尽的宝藏。")
        chapterDao.updateWordCount(chapter1Id, 25)

        // Step 7: Create second chapter
        val chapter2Id = chapterDao.insertChapter(
            ChapterEntity(
                storyId = storyId,
                title = "第二章：相遇",
                content = "",
                orderIndex = 1
            )
        )

        // Step 8: Save multiple drafts for chapter 2
        chapterDraftDao.insertDraft(
            ChapterDraftEntity(chapterId = chapter2Id, content = "版本1", wordCount = 1, version = 1)
        )
        chapterDraftDao.insertDraft(
            ChapterDraftEntity(chapterId = chapter2Id, content = "版本2", wordCount = 1, version = 2)
        )
        chapterDraftDao.insertDraft(
            ChapterDraftEntity(chapterId = chapter2Id, content = "最终版本", wordCount = 2, version = 3)
        )

        // Verify: Story has correct data
        val story = storyDao.getStoryById(storyId)
        assertNotNull(story)
        assertEquals("我的奇幻小说", story?.title)
        assertEquals("fantasy", story?.templateType)

        // Verify: Characters
        val characters = characterDao.getCharactersByStoryId(storyId).first()
        assertEquals(2, characters.size)
        assertTrue(characters.any { it.name == "小明" })
        assertTrue(characters.any { it.name == "小红" })

        // Verify: World settings
        val settings = worldSettingDao.getWorldSettingsByStoryId(storyId).first()
        assertEquals(2, settings.size)
        assertTrue(settings.any { it.category == "magic" })
        assertTrue(settings.any { it.category == "location" })

        // Verify: Chapters in order
        val chapters = chapterDao.getChaptersByStoryId(storyId).first()
        assertEquals(2, chapters.size)
        assertEquals(0, chapters[0].orderIndex)
        assertEquals(1, chapters[1].orderIndex)

        // Verify: Chapter 1 updated
        val updatedChapter1 = chapterDao.getChapterById(chapter1Id)
        assertTrue(updatedChapter1?.content?.contains("宝藏") == true)
        assertEquals(25, updatedChapter1?.wordCount)

        // Verify: Chapter 2 drafts
        val chapter2Drafts = chapterDraftDao.getDraftsByChapterId(chapter2Id).first()
        assertEquals(3, chapter2Drafts.size)
        assertEquals(3, chapter2Drafts[0].version) // DESC order, latest first
        assertEquals("最终版本", chapter2Drafts[0].content)

        // Verify: Draft count
        assertEquals(1, chapterDraftDao.getDraftCount(chapter1Id))
        assertEquals(3, chapterDraftDao.getDraftCount(chapter2Id))
    }

    /**
     * Test: Draft versioning correctly increments
     */
    @Test
    fun `draft versioning increments correctly`() = runTest {
        val storyId = storyDao.insertStory(
            StoryEntity(title = "测试", description = "")
        )
        val chapterId = chapterDao.insertChapter(
            ChapterEntity(storyId = storyId, title = "章节", content = "", orderIndex = 0)
        )

        // Insert drafts with specific versions
        chapterDraftDao.insertDraft(
            ChapterDraftEntity(chapterId = chapterId, content = "v1", version = 1)
        )
        chapterDraftDao.insertDraft(
            ChapterDraftEntity(chapterId = chapterId, content = "v2", version = 2)
        )
        chapterDraftDao.insertDraft(
            ChapterDraftEntity(chapterId = chapterId, content = "v3", version = 3)
        )

        val maxVersion = chapterDraftDao.getMaxVersion(chapterId)
        assertEquals(3, maxVersion)
    }

    /**
     * Test: Deleting story cascades to all related data
     */
    @Test
    fun `story deletion cascades to all related data`() = runTest {
        val storyId = storyDao.insertStory(
            StoryEntity(title = "待删除", description = "")
        )
        chapterDao.insertChapter(
            ChapterEntity(storyId = storyId, title = "章节", content = "", orderIndex = 0)
        )
        characterDao.insertCharacter(
            CharacterEntity(storyId = storyId, name = "角色", description = "")
        )
        worldSettingDao.insertWorldSetting(
            WorldSettingEntity(storyId = storyId, name = "设定", content = "", category = "")
        )

        storyDao.deleteStoryById(storyId)

        assertNull(storyDao.getStoryById(storyId))
        assertTrue(chapterDao.getChaptersByStoryId(storyId).first().isEmpty())
        assertTrue(characterDao.getCharactersByStoryId(storyId).first().isEmpty())
        assertTrue(worldSettingDao.getWorldSettingsByStoryId(storyId).first().isEmpty())
    }

    /**
     * Test: AI continuation with context data
     * Verifies that when AI generates content, all context (characters, settings)
     * is properly stored and retrievable.
     */
    @Test
    fun `AI context data stored and retrievable`() = runTest {
        val storyId = storyDao.insertStory(
            StoryEntity(
                title = "AI测试",
                description = "测试AI上下文",
                templateType = "fantasy"
            )
        )

        // Add comprehensive character data
        characterDao.insertCharacter(
            CharacterEntity(
                storyId = storyId,
                name = "主角",
                description = "性格坚韧，机智勇敢"
            )
        )

        // Add world settings
        worldSettingDao.insertWorldSetting(
            WorldSettingEntity(
                storyId = storyId,
                name = "时代背景",
                content = "蒸汽朋克时代",
                category = "history"
            )
        )
        worldSettingDao.insertWorldSetting(
            WorldSettingEntity(
                storyId = storyId,
                name = "社会结构",
                content = "贵族与平民对立",
                category = "culture"
            )
        )

        // Retrieve all context for AI
        val characters = characterDao.getCharactersByStoryId(storyId).first()
        val settings = worldSettingDao.getWorldSettingsByStoryId(storyId).first()

        val charactersContext = characters.joinToString("\n") {
            "${it.name}: ${it.description}"
        }
        val settingsContext = settings.joinToString("\n") {
            "${it.name}: ${it.content}"
        }

        // Verify context can be formatted for AI prompt
        assertTrue(charactersContext.contains("主角"))
        assertTrue(charactersContext.contains("坚韧"))
        assertTrue(settingsContext.contains("蒸汽朋克"))
        assertTrue(settingsContext.contains("贵族与平民"))
    }
}
