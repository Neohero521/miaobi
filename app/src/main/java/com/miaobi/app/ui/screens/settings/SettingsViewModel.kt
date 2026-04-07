package com.miaobi.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaobi.app.domain.model.AiConfig
import com.miaobi.app.util.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val apiUrl: String = "https://api.siliconflow.cn/v1",
    val modelName: String = "Qwen/Qwen2.5-7B-Instruct",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class SettingsEvent {
    data class UpdateApiKey(val apiKey: String) : SettingsEvent()
    data class UpdateApiUrl(val apiUrl: String) : SettingsEvent()
    data class UpdateModelName(val modelName: String) : SettingsEvent()
    object SaveSettings : SettingsEvent()
    object ClearError : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsManager.aiConfig.collect { config ->
                _uiState.update {
                    it.copy(
                        apiKey = config.apiKey,
                        apiUrl = config.apiUrl,
                        modelName = config.modelName
                    )
                }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateApiKey -> {
                _uiState.update { it.copy(apiKey = event.apiKey) }
            }

            is SettingsEvent.UpdateApiUrl -> {
                _uiState.update { it.copy(apiUrl = event.apiUrl) }
            }

            is SettingsEvent.UpdateModelName -> {
                _uiState.update { it.copy(modelName = event.modelName) }
            }

            SettingsEvent.SaveSettings -> saveSettings()

            SettingsEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                settingsManager.updateAiConfig(
                    AiConfig(
                        apiKey = _uiState.value.apiKey,
                        apiUrl = _uiState.value.apiUrl,
                        modelName = _uiState.value.modelName
                    )
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "保存失败: ${e.message}")
                }
            }
        }
    }
}
