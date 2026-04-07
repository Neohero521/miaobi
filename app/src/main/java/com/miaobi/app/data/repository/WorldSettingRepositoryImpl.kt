package com.miaobi.app.data.repository

import com.miaobi.app.data.local.dao.WorldSettingDao
import com.miaobi.app.domain.model.WorldSetting
import com.miaobi.app.domain.repository.WorldSettingRepository
import com.miaobi.app.util.toDomain
import com.miaobi.app.util.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorldSettingRepositoryImpl @Inject constructor(
    private val worldSettingDao: WorldSettingDao
) : WorldSettingRepository {

    override fun getWorldSettingsByStoryId(storyId: Long): Flow<List<WorldSetting>> {
        return worldSettingDao.getWorldSettingsByStoryId(storyId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getWorldSettingById(settingId: Long): WorldSetting? {
        return worldSettingDao.getWorldSettingById(settingId)?.toDomain()
    }

    override suspend fun insertWorldSetting(setting: WorldSetting): Long {
        return worldSettingDao.insertWorldSetting(setting.toEntity())
    }

    override suspend fun updateWorldSetting(setting: WorldSetting) {
        worldSettingDao.updateWorldSetting(setting.toEntity())
    }

    override suspend fun deleteWorldSetting(settingId: Long) {
        worldSettingDao.deleteWorldSettingById(settingId)
    }
}
