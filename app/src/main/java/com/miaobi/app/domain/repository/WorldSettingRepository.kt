package com.miaobi.app.domain.repository

import com.miaobi.app.domain.model.WorldSetting
import kotlinx.coroutines.flow.Flow

interface WorldSettingRepository {
    fun getWorldSettingsByStoryId(storyId: Long): Flow<List<WorldSetting>>
    suspend fun getWorldSettingById(settingId: Long): WorldSetting?
    suspend fun insertWorldSetting(setting: WorldSetting): Long
    suspend fun updateWorldSetting(setting: WorldSetting)
    suspend fun deleteWorldSetting(settingId: Long)
}
