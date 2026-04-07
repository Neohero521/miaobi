package com.miaobi.app.repository

import com.miaobi.app.data.local.dao.StoryTemplateDao
import com.miaobi.app.data.local.entity.StoryTemplateEntity
import com.miaobi.app.data.repository.StoryTemplateRepositoryImpl
import com.miaobi.app.domain.model.StoryTemplate
import com.miaobi.app.util.toDomain
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

class StoryTemplateRepositoryImplTest {

    private lateinit var storyTemplateDao: StoryTemplateDao
    private lateinit var repository: StoryTemplateRepositoryImpl

    private val testEntity = StoryTemplateEntity(
        id = 1L,
        title = "修仙模板",
        genre = "xianxia",
        summary = "一个修仙世界的故事",
        charactersJson = """[{"name":"主角","description":"天才少年"}]""",
        worldSettingsJson = """[{"name":"境界","content":"炼气、筑基","category":"cultivation"}]""",
        promptTemplate = "请续写这个修仙故事",
        coverImage = "cover.jpg",
        isBuiltIn = true,
        createdAt = 1000L
    )

    @Before
    fun setup() {
        storyTemplateDao = mockk(relaxed = true)
        repository = StoryTemplateRepositoryImpl(storyTemplateDao)
    }

    @Test
    fun `getAllTemplates returns mapped domain objects`() = runTest {
        coEvery { storyTemplateDao.getAllTemplates() } returns flowOf(listOf(testEntity))

        val result = repository.getAllTemplates().first()

        assertEquals(1, result.size)
        assertEquals("修仙模板", result[0].title)
        assertEquals("xianxia", result[0].genre)
        assertTrue(result[0].isBuiltIn)
    }

    @Test
    fun `getTemplatesByGenre returns filtered templates`() = runTest {
        coEvery { storyTemplateDao.getTemplatesByGenre("xianxia") } returns flowOf(listOf(testEntity))

        val result = repository.getTemplatesByGenre("xianxia").first()

        assertEquals(1, result.size)
        assertEquals("xianxia", result[0].genre)
    }

    @Test
    fun `getTemplateById returns template when exists`() = runTest {
        coEvery { storyTemplateDao.getTemplateById(1L) } returns testEntity

        val result = repository.getTemplateById(1L)

        assertNotNull(result)
        assertEquals("修仙模板", result?.title)
    }

    @Test
    fun `getTemplateById returns null when not exists`() = runTest {
        coEvery { storyTemplateDao.getTemplateById(999L) } returns null

        val result = repository.getTemplateById(999L)

        assertNull(result)
    }

    @Test
    fun `insertTemplate inserts entity and returns id`() = runTest {
        coEvery { storyTemplateDao.insertTemplate(any()) } returns 2L
        val slot = slot<StoryTemplateEntity>()

        val template = StoryTemplate(
            title = "都市模板",
            genre = "urban",
            summary = "都市故事",
            promptTemplate = "请续写"
        )
        val result = repository.insertTemplate(template)

        assertEquals(2L, result)
        coVerify { storyTemplateDao.insertTemplate(capture(slot)) }
        assertEquals("都市模板", slot.captured.title)
    }

    @Test
    fun `insertTemplates inserts multiple templates`() = runTest {
        coEvery { storyTemplateDao.insertTemplates(any()) } returns Unit

        val templates = listOf(
            StoryTemplate(title = "模板1", genre = "xianxia", summary = "摘要", promptTemplate = "模板"),
            StoryTemplate(title = "模板2", genre = "urban", summary = "摘要2", promptTemplate = "模板")
        )
        repository.insertTemplates(templates)

        coVerify { storyTemplateDao.insertTemplates(match { it.size == 2 }) }
    }

    @Test
    fun `deleteTemplate deletes entity`() = runTest {
        repository.deleteTemplate(testEntity.toDomain())

        coVerify { storyTemplateDao.deleteTemplate(match { it.title == "修仙模板" }) }
    }

    @Test
    fun `getTemplateCount returns count from dao`() = runTest {
        coEvery { storyTemplateDao.getTemplateCount() } returns 10

        val result = repository.getTemplateCount()

        assertEquals(10, result)
    }
}
