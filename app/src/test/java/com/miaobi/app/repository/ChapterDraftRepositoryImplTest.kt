package com.miaobi.app.repository

import com.miaobi.app.data.local.dao.ChapterDraftDao
import com.miaobi.app.data.local.entity.ChapterDraftEntity
import com.miaobi.app.data.local.entity.ChapterDraftVersionEntity
import com.miaobi.app.data.repository.ChapterDraftRepositoryImpl
import com.miaobi.app.domain.model.ChapterDraft
import com.miaobi.app.domain.model.ChapterDraftVersion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChapterDraftRepositoryImplTest {

    private lateinit var chapterDraftDao: ChapterDraftDao
    private lateinit var repository: ChapterDraftRepositoryImpl

    private val testEntity = ChapterDraftEntity(
        id = 1L,
        chapterId = 1L,
        content = "草稿内容",
        wordCount = 3,
        version = 1,
        createdAt = 1000L
    )

    @Before
    fun setup() {
        chapterDraftDao = mockk(relaxed = true)
        repository = ChapterDraftRepositoryImpl(chapterDraftDao)
    }

    @Test
    fun `getDraftsByChapterId returns drafts`() = runTest {
        coEvery { chapterDraftDao.getDraftsByChapterId(1L) } returns flowOf(listOf(testEntity))

        val result = repository.getDraftsByChapterId(1L).first()

        assertEquals(1, result.size)
        assertEquals("草稿内容", result[0].content)
    }

    @Test
    fun `getDraftById returns draft when exists`() = runTest {
        coEvery { chapterDraftDao.getDraftById(1L) } returns testEntity

        val result = repository.getDraftById(1L)

        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `insertDraft inserts with correct version`() = runTest {
        coEvery { chapterDraftDao.getMaxVersion(1L) } returns 2
        coEvery { chapterDraftDao.insertDraft(any()) } returns 3L
        val slot = slot<ChapterDraftEntity>()

        val draft = ChapterDraft(chapterId = 1L, content = "内容", wordCount = 1, version = 1)
        repository.insertDraft(draft)

        coVerify { chapterDraftDao.insertDraft(capture(slot)) }
    }

    @Test
    fun `saveDraft creates new draft with incremented version`() = runTest {
        coEvery { chapterDraftDao.getMaxVersion(1L) } returns 2
        coEvery { chapterDraftDao.getDraftCount(1L) } returns 5
        coEvery { chapterDraftDao.insertDraft(any()) } returns 3L
        val slot = slot<ChapterDraftEntity>()

        repository.saveDraft(1L, "这是草稿内容")

        coVerify { chapterDraftDao.insertDraft(capture(slot)) }
        assertEquals(3, slot.captured.version) // maxVersion + 1
        assertEquals(4, slot.captured.wordCount) // 4 words
    }

    @Test
    fun `saveDraft deletes oldest when at version limit`() = runTest {
        coEvery { chapterDraftDao.getMaxVersion(1L) } returns 19
        coEvery { chapterDraftDao.getDraftCount(1L) } returns 20
        coEvery { chapterDraftDao.deleteOldestDraft(1L) } returns Unit
        coEvery { chapterDraftDao.insertDraft(any()) } returns 21L

        repository.saveDraft(1L, "新内容")

        coVerify { chapterDraftDao.deleteOldestDraft(1L) }
    }

    @Test
    fun `deleteDraft deletes by id`() = runTest {
        repository.deleteDraft(1L)

        coVerify { chapterDraftDao.deleteDraftById(1L) }
    }

    @Test
    fun `deleteDraftsByChapterId deletes all`() = runTest {
        repository.deleteDraftsByChapterId(1L)

        coVerify { chapterDraftDao.deleteDraftsByChapterId(1L) }
    }

    @Test
    fun `getDraftCount returns count`() = runTest {
        coEvery { chapterDraftDao.getDraftCount(1L) } returns 5

        val result = repository.getDraftCount(1L)

        assertEquals(5, result)
    }

    // ===== ChapterDraftVersion tests =====

    private val testVersionEntity = ChapterDraftVersionEntity(
        id = 1L,
        draftId = 1L,
        version = 1,
        content = "版本内容",
        wordCount = 3,
        diffSummary = "初始版本",
        createdAt = 1000L
    )

    @Test
    fun `getVersionsByDraftId returns mapped versions`() = runTest {
        coEvery { chapterDraftDao.getVersionsByDraftId(1L) } returns flowOf(listOf(testVersionEntity))

        val result = repository.getVersionsByDraftId(1L).first()

        assertEquals(1, result.size)
        assertEquals("版本内容", result[0].content)
        assertEquals(1, result[0].version)
    }

    @Test
    fun `getVersionById returns version when exists`() = runTest {
        coEvery { chapterDraftDao.getVersionById(1L) } returns testVersionEntity

        val result = repository.getVersionById(1L)

        assertNotNull(result)
        assertEquals(1L, result?.id)
        assertEquals("版本内容", result?.content)
    }

    @Test
    fun `getVersionById returns null when not exists`() = runTest {
        coEvery { chapterDraftDao.getVersionById(999L) } returns null

        val result = repository.getVersionById(999L)

        assertNull(result)
    }
}
