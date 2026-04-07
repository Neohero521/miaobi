package com.miaobi.app.data.local.dao

import androidx.room.*
import com.miaobi.app.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY updatedAt DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :storyId")
    suspend fun getStoryById(storyId: Long): StoryEntity?

    @Query("SELECT * FROM stories WHERE id = :storyId")
    fun getStoryByIdFlow(storyId: Long): Flow<StoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity): Long

    @Update
    suspend fun updateStory(story: StoryEntity)

    @Delete
    suspend fun deleteStory(story: StoryEntity)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: Long)

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getStoryCount(): Int
}
