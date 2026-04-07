package com.miaobi.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.miaobi.app.data.local.dao.*
import com.miaobi.app.data.local.database.MiaobiDatabase
import com.miaobi.app.data.local.database.StoryTemplateData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MiaobiDatabase {
        return Room.databaseBuilder(
            context,
            MiaobiDatabase::class.java,
            MiaobiDatabase.DATABASE_NAME
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Prepopulate templates on first database creation
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        populateTemplates(db)
                    }
                }
            })
            .build()
    }

    private fun populateTemplates(db: SupportSQLiteDatabase) {
        val templates = StoryTemplateData.getTemplates()
        templates.forEach { template ->
            db.execSQL(
                """INSERT INTO story_templates 
                   (title, genre, summary, charactersJson, worldSettingsJson, promptTemplate, coverImage, isBuiltIn, createdAt) 
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                arrayOf(
                    template.title,
                    template.genre,
                    template.summary,
                    template.charactersJson,
                    template.worldSettingsJson,
                    template.promptTemplate,
                    template.coverImage,
                    if (template.isBuiltIn) 1 else 0,
                    template.createdAt
                )
            )
        }
    }

    @Provides
    @Singleton
    fun provideStoryDao(database: MiaobiDatabase): StoryDao {
        return database.storyDao()
    }

    @Provides
    @Singleton
    fun provideChapterDao(database: MiaobiDatabase): ChapterDao {
        return database.chapterDao()
    }

    @Provides
    @Singleton
    fun provideCharacterDao(database: MiaobiDatabase): CharacterDao {
        return database.characterDao()
    }

    @Provides
    @Singleton
    fun provideWorldSettingDao(database: MiaobiDatabase): WorldSettingDao {
        return database.worldSettingDao()
    }

    @Provides
    @Singleton
    fun provideChapterDraftDao(database: MiaobiDatabase): ChapterDraftDao {
        return database.chapterDraftDao()
    }

    @Provides
    @Singleton
    fun provideStoryTemplateDao(database: MiaobiDatabase): StoryTemplateDao {
        return database.storyTemplateDao()
    }
}
