package com.miaobi.app.data.repository

import com.miaobi.app.data.local.dao.ChapterDraftDao
import com.miaobi.app.data.local.entity.ChapterDraftEntity
import com.miaobi.app.data.local.entity.ChapterDraftVersionEntity
import com.miaobi.app.domain.model.ChapterDraft
import com.miaobi.app.domain.model.ChapterDraftVersion
import com.miaobi.app.domain.repository.ChapterDraftRepository
import com.miaobi.app.util.toDomain
import com.miaobi.app.util.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterDraftRepositoryImpl @Inject constructor(
    private val chapterDraftDao: ChapterDraftDao
) : ChapterDraftRepository {

    override fun getDraftsByChapterId(chapterId: Long): Flow<List<ChapterDraft>> {
        return chapterDraftDao.getDraftsByChapterId(chapterId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDraftById(draftId: Long): ChapterDraft? {
        return chapterDraftDao.getDraftById(draftId)?.toDomain()
    }

    override suspend fun insertDraft(draft: ChapterDraft): Long {
        return chapterDraftDao.insertDraft(draft.toEntity())
    }

    override suspend fun saveDraft(chapterId: Long, content: String): Long {
        val maxVersion = chapterDraftDao.getMaxVersion(chapterId) ?: 0
        val wordCount = content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        
        // Check version count limit (20)
        val count = chapterDraftDao.getDraftCount(chapterId)
        if (count >= 20) {
            chapterDraftDao.deleteOldestDraft(chapterId)
        }
        
        val draft = ChapterDraft(
            chapterId = chapterId,
            content = content,
            wordCount = wordCount,
            version = maxVersion + 1,
            isCurrent = true
        )
        return chapterDraftDao.insertDraft(draft.toEntity())
    }

    override suspend fun deleteDraft(draftId: Long) {
        chapterDraftDao.deleteDraftById(draftId)
    }

    override suspend fun deleteDraftsByChapterId(chapterId: Long) {
        chapterDraftDao.deleteDraftsByChapterId(chapterId)
    }

    override suspend fun getDraftCount(chapterId: Long): Int {
        return chapterDraftDao.getDraftCount(chapterId)
    }

    override fun getVersionsByDraftId(draftId: Long): Flow<List<ChapterDraftVersion>> {
        return chapterDraftDao.getVersionsByDraftId(draftId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getVersionById(versionId: Long): ChapterDraftVersion? {
        return chapterDraftDao.getVersionById(versionId)?.toDomain()
    }
}
