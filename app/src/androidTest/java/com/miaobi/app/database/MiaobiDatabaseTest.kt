package com.miaobi.app.database

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.miaobi.app.data.local.dao.*
import com.miaobi.app.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class MiaobiDatabaseTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: com.miaobi.app.data.local.database.MiaobiDatabase
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
            com.miaobi.app.data.local.database.MiaobiDatabase::class.java
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

    // ===== Story DAO Tests =====

    @Test
    fun `StoryDao inserts and retrieves story`() = runTest {
        val entity = StoryEntity(
            title = "测试故事",
            description = "描述",
            templateType = "fantasy"
        )
        val id = storyDao.insertStory(entity)

        val result = storyDao.getStoryById(id)

        assertNotNull(result)
        assertEquals("测试故事", result?.title)
    }

    @Test
    fun `StoryDao getAllStories returns all stories`() = runTest {
        storyDao.insertStory(StoryEntity(title = "故事1", description = ""))
        storyDao.insertStory(StoryEntity(title = "故事2", description = ""))

        val result = storyDao.getAllStories().first()

        assertEquals(2, result.size)
    }

    @Test
    fun `StoryDao deleteStoryById removes story`() = runTest {
        val id = storyDao.insertStory(StoryEntity(title = "删除我", description = ""))

        storyDao.deleteStoryById(id)

        assertNull(storyDao.getStoryById(id))
    }

    @Test
    fun `StoryDao updateStory updates story`() = runTest {
        val id = storyDao.insertStory(StoryEntity(title = "旧标题", description = ""))

        storyDao.updateStory(StoryEntity(id = id, title = "新标题", description = ""))

        assertEquals("新标题", storyDao.getStoryById(id)?.title)
    }

    @Test
    fun `StoryDao getStoryCount returns count`() = runTest {
        storyDao.insertStory(StoryEntity(title = "1", description = ""))
        storyDao.insertStory(StoryEntity(title = "2", description = ""))
        storyDao.insertStory(StoryEntity(title = "3", description = ""))

        assertEquals(3, storyDao.getStoryCount())
    }

    // ===== Chapter DAO Tests =====

    @Test
    fun `ChapterDao inserts and retrieves chapter`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val entity = ChapterEntity(
            storyId = storyId,
            title = "第一章",
            content = "内容",
            orderIndex = 0
        )
        val id = chapterDao.insertChapter(entity)

        val result = chapterDao.getChapterById(id)

        assertNotNull(result)
        assertEquals("第一章", result?.title)
    }

    @Test
    fun `ChapterDao getChaptersByStoryId returns chapters in order`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        chapterDao.insertChapter(ChapterEntity(storyId = storyId, title = "第一章", content = "", orderIndex = 0))
        chapterDao.insertChapter(ChapterEntity(storyId = storyId, title = "第二章", content = "", orderIndex = 1))

        val result = chapterDao.getChaptersByStoryId(storyId).first()

        assertEquals(2, result.size)
        assertEquals("第一章", result[0].title)
        assertEquals("第二章", result[1].title)
    }

    @Test
    fun `ChapterDao getMaxOrderIndex returns correct max`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        chapterDao.insertChapter(ChapterEntity(storyId = storyId, title = "第一章", content = "", orderIndex = 0))
        chapterDao.insertChapter(ChapterEntity(storyId = storyId, title = "第二章", content = "", orderIndex = 2))

        val maxIndex = chapterDao.getMaxOrderIndex(storyId)

        assertEquals(2, maxIndex)
    }

    @Test
    fun `ChapterDao updateContent updates content`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val chapterId = chapterDao.insertChapter(
            ChapterEntity(storyId = storyId, title = "第一章", content = "", orderIndex = 0)
        )

        chapterDao.updateContent(chapterId, "新内容")

        assertEquals("新内容", chapterDao.getChapterById(chapterId)?.content)
    }

    @Test
    fun `ChapterDao updateWordCount updates count`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val chapterId = chapterDao.insertChapter(
            ChapterEntity(storyId = storyId, title = "第一章", content = "", orderIndex = 0)
        )

        chapterDao.updateWordCount(chapterId, 100)

        assertEquals(100, chapterDao.getChapterById(chapterId)?.wordCount)
    }

    @Test
    fun `ChapterDao cascade delete when story deleted`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        chapterDao.insertChapter(ChapterEntity(storyId = storyId, title = "第一章", content = "", orderIndex = 0))

        storyDao.deleteStoryById(storyId)

        assertTrue(chapterDao.getChaptersByStoryId(storyId).first().isEmpty())
    }

    // ===== Character DAO Tests =====

    @Test
    fun `CharacterDao inserts and retrieves character`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val entity = CharacterEntity(
            storyId = storyId,
            name = "小明",
            description = "主角"
        )
        val id = characterDao.insertCharacter(entity)

        val result = characterDao.getCharacterById(id)

        assertNotNull(result)
        assertEquals("小明", result?.name)
    }

    @Test
    fun `CharacterDao getCharactersByStoryId returns characters`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        characterDao.insertCharacter(CharacterEntity(storyId = storyId, name = "小明", description = ""))
        characterDao.insertCharacter(CharacterEntity(storyId = storyId, name = "小红", description = ""))

        val result = characterDao.getCharactersByStoryId(storyId).first()

        assertEquals(2, result.size)
    }

    @Test
    fun `CharacterDao updateCharacter updates character`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val id = characterDao.insertCharacter(CharacterEntity(storyId = storyId, name = "旧名", description = ""))

        characterDao.updateCharacter(CharacterEntity(id = id, storyId = storyId, name = "新名", description = ""))

        assertEquals("新名", characterDao.getCharacterById(id)?.name)
    }

    // ===== WorldSetting DAO Tests =====

    @Test
    fun `WorldSettingDao inserts and retrieves setting`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val entity = WorldSettingEntity(
            storyId = storyId,
            name = "世界观",
            content = "设定",
            category = "magic"
        )
        val id = worldSettingDao.insertWorldSetting(entity)

        val result = worldSettingDao.getWorldSettingById(id)

        assertNotNull(result)
        assertEquals("世界观", result?.name)
        assertEquals("magic", result?.category)
    }

    @Test
    fun `WorldSettingDao getWorldSettingsByStoryId returns settings`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        worldSettingDao.insertWorldSetting(WorldSettingEntity(storyId = storyId, name = "设定1", content = ""))
        worldSettingDao.insertWorldSetting(WorldSettingEntity(storyId = storyId, name = "设定2", content = ""))

        val result = worldSettingDao.getWorldSettingsByStoryId(storyId).first()

        assertEquals(2, result.size)
    }

    // ===== ChapterDraft DAO Tests =====

    @Test
    fun `ChapterDraftDao inserts and retrieves draft`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val chapterId = chapterDao.insertChapter(
            ChapterEntity(storyId = storyId, title = "第一章", content = "", orderIndex = 0)
        )
        val entity = ChapterDraftEntity(
            chapterId = chapterId,
            content = "草稿",
            wordCount = 1,
            version = 1
        )
        val id = chapterDraftDao.insertDraft(entity)

        val result = chapterDraftDao.getDraftById(id)

        assertNotNull(result)
        assertEquals("草稿", result?.content)
    }

    @Test
    fun `ChapterDraftDao getDraftsByChapterId returns drafts in desc order`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val chapterId = chapterDao.insertChapter(
            ChapterEntity(storyId = storyId, title = "第一章", content = "", orderIndex = 0)
        )
        chapterDraftDao.insertDraft(ChapterDraftEntity(chapterId = chapterId, content = "v1", version = 1))
        chapterDraftDao.insertDraft(ChapterDraftEntity(chapterId = chapterId, content = "v2", version = 2))
        chapterDraftDao.insertDraft(ChapterDraftEntity(chapterId = chapterId, content = "v3", version = 3))

        val result = chapterDraftDao.getDraftsByChapterId(chapterId).first()

        assertEquals(3, result.size)
        assertEquals(3, result[0].version) // DESC order
    }

    @Test
    fun `ChapterDraftDao getMaxVersion returns correct version`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val chapterId = chapterDao.insertChapter(
            ChapterEntity(storyId = storyId, title = "第一章", content = "", orderIndex = 0)
        )
        chapterDraftDao.insertDraft(ChapterDraftEntity(chapterId = chapterId, content = "", version = 2))
        chapterDraftDao.insertDraft(ChapterDraftEntity(chapterId = chapterId, content = "", version = 5))

        val maxVersion = chapterDraftDao.getMaxVersion(chapterId)

        assertEquals(5, maxVersion)
    }

    @Test
    fun `ChapterDraftDao getDraftCount returns count`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val chapterId = chapterDao.insertChapter(
            ChapterEntity(storyId = storyId, title = "第一章", content = "", orderIndex = 0)
        )
        chapterDraftDao.insertDraft(ChapterDraftEntity(chapterId = chapterId, content = "", version = 1))
        chapterDraftDao.insertDraft(ChapterDraftEntity(chapterId = chapterId, content = "", version = 2))

        val count = chapterDraftDao.getDraftCount(chapterId)

        assertEquals(2, count)
    }

    @Test
    fun `ChapterDraftDao cascade delete when chapter deleted`() = runTest {
        val storyId = storyDao.insertStory(StoryEntity(title = "故事", description = ""))
        val chapterId = chapterDao.insertChapter(
            ChapterEntity(storyId = storyId, title = "第一章", content = "", orderIndex = 0)
        )
        chapterDraftDao.insertDraft(ChapterDraftEntity(chapterId = chapterId, content = "草稿", version = 1))

        chapterDao.deleteChapterById(chapterId)

        assertEquals(0, chapterDraftDao.getDraftCount(chapterId))
    }
}
