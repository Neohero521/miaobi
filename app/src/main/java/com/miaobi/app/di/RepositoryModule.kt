package com.miaobi.app.di

import com.miaobi.app.data.repository.*
import com.miaobi.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStoryRepository(
        storyRepositoryImpl: StoryRepositoryImpl
    ): StoryRepository

    @Binds
    @Singleton
    abstract fun bindChapterRepository(
        chapterRepositoryImpl: ChapterRepositoryImpl
    ): ChapterRepository

    @Binds
    @Singleton
    abstract fun bindCharacterRepository(
        characterRepositoryImpl: CharacterRepositoryImpl
    ): CharacterRepository

    @Binds
    @Singleton
    abstract fun bindWorldSettingRepository(
        worldSettingRepositoryImpl: WorldSettingRepositoryImpl
    ): WorldSettingRepository

    @Binds
    @Singleton
    abstract fun bindChapterDraftRepository(
        chapterDraftRepositoryImpl: ChapterDraftRepositoryImpl
    ): ChapterDraftRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(
        aiRepositoryImpl: AiRepositoryImpl
    ): AiRepository

    @Binds
    @Singleton
    abstract fun bindStoryTemplateRepository(
        storyTemplateRepositoryImpl: StoryTemplateRepositoryImpl
    ): StoryTemplateRepository
}
