package com.miaobi.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.miaobi.app.data.local.dao.*
import com.miaobi.app.data.local.entity.*

@Database(
    entities = [
        StoryEntity::class,
        ChapterEntity::class,
        CharacterEntity::class,
        WorldSettingEntity::class,
        ChapterDraftEntity::class,
        ChapterDraftVersionEntity::class,
        StoryTemplateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MiaobiDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun chapterDao(): ChapterDao
    abstract fun characterDao(): CharacterDao
    abstract fun worldSettingDao(): WorldSettingDao
    abstract fun chapterDraftDao(): ChapterDraftDao
    abstract fun storyTemplateDao(): StoryTemplateDao

    companion object {
        const val DATABASE_NAME = "miaobi_database"
    }
}
