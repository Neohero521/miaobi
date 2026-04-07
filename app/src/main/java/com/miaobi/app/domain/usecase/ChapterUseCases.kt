package com.miaobi.app.domain.usecase

import com.miaobi.app.domain.model.Chapter
import com.miaobi.app.domain.repository.ChapterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChaptersByStoryIdUseCase @Inject constructor(
    private val chapterRepository: ChapterRepository
) {
    operator fun invoke(storyId: Long): Flow<List<Chapter>> {
        return chapterRepository.getChaptersByStoryId(storyId)
    }
}

class GetChapterByIdUseCase @Inject constructor(
    private val chapterRepository: ChapterRepository
) {
    operator fun invoke(chapterId: Long): Flow<Chapter?> {
        return chapterRepository.getChapterById(chapterId)
    }
}

class CreateChapterUseCase @Inject constructor(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(storyId: Long, title: String, content: String = ""): Long {
        val chapter = Chapter(
            storyId = storyId,
            title = title.ifBlank { "新章节" },
            content = content,
            orderIndex = 0
        )
        return chapterRepository.insertChapter(chapter)
    }
}

class UpdateChapterContentUseCase @Inject constructor(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(chapterId: Long, content: String) {
        chapterRepository.updateChapterContent(chapterId, content)
    }
}

class DeleteChapterUseCase @Inject constructor(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(chapterId: Long) {
        chapterRepository.deleteChapter(chapterId)
    }
}
