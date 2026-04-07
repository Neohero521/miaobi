package com.miaobi.app.data.local.dao

import androidx.room.*
import com.miaobi.app.data.local.entity.ChapterDraftEntity
import com.miaobi.app.data.local.entity.ChapterDraftVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDraftDao {
    @Query("SELECT * FROM chapter_drafts WHERE chapterId = :chapterId ORDER BY version DESC")
    fun getDraftsByChapterId(chapterId: Long): Flow<List<ChapterDraftEntity>>

    @Query("SELECT * FROM chapter_drafts WHERE id = :draftId")
    suspend fun getDraftById(draftId: Long): ChapterDraftEntity?

    @Query("SELECT MAX(version) FROM chapter_drafts WHERE chapterId = :chapterId")
    suspend fun getMaxVersion(chapterId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ChapterDraftEntity): Long

    @Delete
    suspend fun deleteDraft(draft: ChapterDraftEntity)

    @Query("DELETE FROM chapter_drafts WHERE chapterId = :chapterId")
    suspend fun deleteDraftsByChapterId(chapterId: Long)

    @Query("DELETE FROM chapter_drafts WHERE id = :draftId")
    suspend fun deleteDraftById(draftId: Long)

    @Query("SELECT COUNT(*) FROM chapter_drafts WHERE chapterId = :chapterId")
    suspend fun getDraftCount(chapterId: Long): Int

    @Query("DELETE FROM chapter_drafts WHERE chapterId = :chapterId AND id = (SELECT id FROM chapter_drafts WHERE chapterId = :chapterId ORDER BY createdAt ASC LIMIT 1)")
    suspend fun deleteOldestDraft(chapterId: Long)

    // Version management
    @Query("SELECT * FROM chapter_draft_versions WHERE draftId = :draftId ORDER BY version DESC")
    fun getVersionsByDraftId(draftId: Long): Flow<List<ChapterDraftVersionEntity>>

    @Query("SELECT * FROM chapter_draft_versions WHERE id = :versionId")
    suspend fun getVersionById(versionId: Long): ChapterDraftVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraftVersion(version: ChapterDraftVersionEntity): Long

    @Delete
    suspend fun deleteDraftVersion(version: ChapterDraftVersionEntity)

    @Query("DELETE FROM chapter_draft_versions WHERE draftId = :draftId")
    suspend fun deleteVersionsByDraftId(draftId: Long)

    @Query("DELETE FROM chapter_draft_versions WHERE draftId = :draftId AND id = (SELECT id FROM chapter_draft_versions WHERE draftId = :draftId ORDER BY createdAt ASC LIMIT 1)")
    suspend fun deleteOldestVersion(draftId: Long)

    @Query("SELECT COUNT(*) FROM chapter_draft_versions WHERE draftId = :draftId")
    suspend fun getVersionCount(draftId: Long): Int
}
