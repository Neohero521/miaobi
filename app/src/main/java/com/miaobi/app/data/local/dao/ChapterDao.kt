package com.miaobi.app.data.local.dao

import androidx.room.*
import com.miaobi.app.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE storyId = :storyId ORDER BY orderIndex ASC")
    fun getChaptersByStoryId(storyId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: Long): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    fun getChapterByIdFlow(chapterId: Long): Flow<ChapterEntity?>

    @Query("SELECT MAX(orderIndex) FROM chapters WHERE storyId = :storyId")
    suspend fun getMaxOrderIndex(storyId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE id = :chapterId")
    suspend fun deleteChapterById(chapterId: Long)

    @Query("UPDATE chapters SET wordCount = :wordCount, updatedAt = :updatedAt WHERE id = :chapterId")
    suspend fun updateWordCount(chapterId: Long, wordCount: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE chapters SET content = :content, updatedAt = :updatedAt WHERE id = :chapterId")
    suspend fun updateContent(chapterId: Long, content: String, updatedAt: Long = System.currentTimeMillis())
}
