package com.miaobi.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miaobi.app.domain.model.AiConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val API_URL = stringPreferencesKey("api_url")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val WELCOME_SHOWN = booleanPreferencesKey("welcome_shown")
    }

    val welcomeShown: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.WELCOME_SHOWN] ?: false
    }

    val aiConfig: Flow<AiConfig> = context.dataStore.data.map { preferences ->
        AiConfig(
            apiKey = preferences[PreferencesKeys.API_KEY] ?: "",
            apiUrl = preferences[PreferencesKeys.API_URL] ?: "https://api.siliconflow.cn/v1",
            modelName = preferences[PreferencesKeys.MODEL_NAME] ?: "Qwen/Qwen2.5-7B-Instruct"
        )
    }

    suspend fun updateApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_KEY] = apiKey
        }
    }

    suspend fun updateApiUrl(apiUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_URL] = apiUrl
        }
    }

    suspend fun updateModelName(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MODEL_NAME] = modelName
        }
    }

    suspend fun updateAiConfig(config: AiConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_KEY] = config.apiKey
            preferences[PreferencesKeys.API_URL] = config.apiUrl
            preferences[PreferencesKeys.MODEL_NAME] = config.modelName
        }
    }

    suspend fun setWelcomeShown() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WELCOME_SHOWN] = true
        }
    }
}
