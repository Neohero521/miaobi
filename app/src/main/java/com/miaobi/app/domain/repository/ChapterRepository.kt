package com.miaobi.app.domain.repository

import com.miaobi.app.domain.model.Chapter
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {
    fun getChaptersByStoryId(storyId: Long): Flow<List<Chapter>>
    fun getChapterById(chapterId: Long): Flow<Chapter?>
    suspend fun insertChapter(chapter: Chapter): Long
    suspend fun updateChapter(chapter: Chapter)
    suspend fun deleteChapter(chapterId: Long)
    suspend fun updateChapterContent(chapterId: Long, content: String)
}
