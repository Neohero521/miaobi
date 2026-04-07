package com.miaobi.app.data.local.dao

import androidx.room.*
import com.miaobi.app.data.local.entity.WorldSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldSettingDao {
    @Query("SELECT * FROM world_settings WHERE storyId = :storyId ORDER BY createdAt ASC")
    fun getWorldSettingsByStoryId(storyId: Long): Flow<List<WorldSettingEntity>>

    @Query("SELECT * FROM world_settings WHERE id = :settingId")
    suspend fun getWorldSettingById(settingId: Long): WorldSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorldSetting(setting: WorldSettingEntity): Long

    @Update
    suspend fun updateWorldSetting(setting: WorldSettingEntity)

    @Delete
    suspend fun deleteWorldSetting(setting: WorldSettingEntity)

    @Query("DELETE FROM world_settings WHERE id = :settingId")
    suspend fun deleteWorldSettingById(settingId: Long)
}
