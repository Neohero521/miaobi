package com.miaobi.app.data.repository

import com.miaobi.app.data.local.dao.ChapterDao
import com.miaobi.app.domain.model.Chapter
import com.miaobi.app.domain.repository.ChapterRepository
import com.miaobi.app.util.toDomain
import com.miaobi.app.util.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepositoryImpl @Inject constructor(
    private val chapterDao: ChapterDao
) : ChapterRepository {

    override fun getChaptersByStoryId(storyId: Long): Flow<List<Chapter>> {
        return chapterDao.getChaptersByStoryId(storyId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getChapterById(chapterId: Long): Flow<Chapter?> {
        return chapterDao.getChapterByIdFlow(chapterId).map { it?.toDomain() }
    }

    override suspend fun insertChapter(chapter: Chapter): Long {
        val maxIndex = chapterDao.getMaxOrderIndex(chapter.storyId) ?: -1
        val entity = chapter.toEntity().copy(orderIndex = maxIndex + 1)
        return chapterDao.insertChapter(entity)
    }

    override suspend fun updateChapter(chapter: Chapter) {
        chapterDao.updateChapter(chapter.toEntity())
    }

    override suspend fun deleteChapter(chapterId: Long) {
        chapterDao.deleteChapterById(chapterId)
    }

    override suspend fun updateChapterContent(chapterId: Long, content: String) {
        chapterDao.updateContent(chapterId, content)
        chapterDao.updateWordCount(chapterId, countWords(content))
    }

    private fun countWords(text: String): Int {
        return text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    }
}
