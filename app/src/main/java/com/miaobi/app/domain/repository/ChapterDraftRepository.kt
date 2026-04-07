package com.miaobi.app.domain.repository

import com.miaobi.app.domain.model.ChapterDraft
import com.miaobi.app.domain.model.ChapterDraftVersion
import kotlinx.coroutines.flow.Flow

interface ChapterDraftRepository {
    fun getDraftsByChapterId(chapterId: Long): Flow<List<ChapterDraft>>
    suspend fun getDraftById(draftId: Long): ChapterDraft?
    suspend fun insertDraft(draft: ChapterDraft): Long
    suspend fun saveDraft(chapterId: Long, content: String): Long
    suspend fun deleteDraft(draftId: Long)
    suspend fun deleteDraftsByChapterId(chapterId: Long)
    suspend fun getDraftCount(chapterId: Long): Int
    fun getVersionsByDraftId(draftId: Long): Flow<List<ChapterDraftVersion>>
    suspend fun getVersionById(versionId: Long): ChapterDraftVersion?
}
