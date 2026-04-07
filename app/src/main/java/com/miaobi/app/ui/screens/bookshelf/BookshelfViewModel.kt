package com.miaobi.app.ui.screens.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.miaobi.app.domain.model.*
import com.miaobi.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookshelfUiState(
    val stories: List<Story> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val newStoryTitle: String = "",
    val newStoryDescription: String = "",
    val templates: List<StoryTemplate> = emptyList(),
    val showTemplateDialog: Boolean = false,
    val selectedTemplate: StoryTemplate? = null,
    val showTemplateDetailDialog: Boolean = false
)

sealed class BookshelfEvent {
    object ToggleCreateDialog : BookshelfEvent()
    data class UpdateNewStoryTitle(val title: String) : BookshelfEvent()
    data class UpdateNewStoryDescription(val description: String) : BookshelfEvent()
    data class CreateStory(val title: String, val description: String, val templateType: String = "free") : BookshelfEvent()
    data class CreateStoryFromTemplate(val title: String, val description: String, val template: StoryTemplate) : BookshelfEvent()
    data class DeleteStory(val storyId: Long) : BookshelfEvent()
    object ShowTemplateDialog : BookshelfEvent()
    object HideTemplateDialog : BookshelfEvent()
    data class SelectTemplate(val template: StoryTemplate) : BookshelfEvent()
    data class UpdateNewStoryTemplate(val template: StoryTemplate?) : BookshelfEvent()
    object ShowTemplateDetailDialog : BookshelfEvent()
    object HideTemplateDetailDialog : BookshelfEvent()
    object ConfirmCreateFromTemplate : BookshelfEvent()
}

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldSettingRepository: WorldSettingRepository,
    private val storyTemplateRepository: StoryTemplateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookshelfUiState())
    val uiState: StateFlow<BookshelfUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        loadStories()
        loadTemplates()
    }

    private fun loadStories() {
        viewModelScope.launch {
            storyRepository.getAllStories()
                .collect { stories ->
                    _uiState.update {
                        it.copy(stories = stories, isLoading = false)
                    }
                }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            storyTemplateRepository.getAllTemplates().collect { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    fun onEvent(event: BookshelfEvent) {
        when (event) {
            BookshelfEvent.ToggleCreateDialog -> {
                _uiState.update {
                    it.copy(
                        showCreateDialog = !it.showCreateDialog,
                        newStoryTitle = "",
                        newStoryDescription = "",
                        selectedTemplate = null
                    )
                }
            }

            is BookshelfEvent.UpdateNewStoryTitle -> {
                _uiState.update { it.copy(newStoryTitle = event.title) }
            }

            is BookshelfEvent.UpdateNewStoryDescription -> {
                _uiState.update { it.copy(newStoryDescription = event.description) }
            }

            is BookshelfEvent.CreateStory -> {
                createStory(event.title, event.description, event.templateType)
            }

            is BookshelfEvent.CreateStoryFromTemplate -> {
                createStoryFromTemplate(event.title, event.description, event.template)
            }

            is BookshelfEvent.DeleteStory -> {
                deleteStory(event.storyId)
            }

            BookshelfEvent.ShowTemplateDialog -> {
                _uiState.update { it.copy(showTemplateDialog = true) }
            }

            BookshelfEvent.HideTemplateDialog -> {
                _uiState.update { it.copy(showTemplateDialog = false) }
            }

            is BookshelfEvent.SelectTemplate -> {
                _uiState.update {
                    it.copy(
                        selectedTemplate = event.template,
                        showTemplateDialog = false,
                        showTemplateDetailDialog = true
                    )
                }
            }

            is BookshelfEvent.UpdateNewStoryTemplate -> {
                _uiState.update { it.copy(selectedTemplate = event.template) }
            }

            BookshelfEvent.ShowTemplateDetailDialog -> {
                _uiState.update { it.copy(showTemplateDetailDialog = true) }
            }

            BookshelfEvent.HideTemplateDetailDialog -> {
                _uiState.update {
                    it.copy(
                        showTemplateDetailDialog = false,
                        selectedTemplate = null,
                        newStoryTitle = "",
                        newStoryDescription = ""
                    )
                }
            }

            BookshelfEvent.ConfirmCreateFromTemplate -> {
                val template = _uiState.value.selectedTemplate ?: return
                val title = _uiState.value.newStoryTitle.ifBlank { template.title }
                val description = _uiState.value.newStoryDescription.ifBlank { template.summary }
                createStoryFromTemplate(title, description, template)
            }
        }
    }

    private fun createStory(title: String, description: String, templateType: String) {
        viewModelScope.launch {
            val story = Story(
                title = title.ifBlank { "无标题故事" },
                description = description,
                templateType = templateType
            )
            val storyId = storyRepository.insertStory(story)
            // 自动创建第一章，避免打开 WritingScreen 时无章节导致问题
            val chapter = Chapter(
                storyId = storyId,
                title = "第一章",
                content = "",
                orderIndex = 0
            )
            try {
                chapterRepository.insertChapter(chapter)
            } catch (e: Exception) {
                // 章节创建失败不影响故事创建，只记录日志
                e.printStackTrace()
            }
            _uiState.update {
                it.copy(showCreateDialog = false, newStoryTitle = "", newStoryDescription = "", selectedTemplate = null)
            }
        }
    }

    private fun createStoryFromTemplate(title: String, description: String, template: StoryTemplate) {
        viewModelScope.launch {
            // Create story with template type
            val story = Story(
                title = title.ifBlank { template.title },
                description = description.ifBlank { template.summary },
                templateType = template.genre
            )
            val storyId = storyRepository.insertStory(story)

            // Create characters from template
            template.charactersJson?.let { charsJson ->
                if (charsJson.isNotBlank()) {
                    try {
                        val characterType = object : TypeToken<List<Map<String, String>>>() {}.type
                        val characters: List<Map<String, String>> = gson.fromJson(charsJson, characterType)
                        characters.forEach { charMap ->
                            val character = Character(
                                storyId = storyId,
                                name = charMap["name"] ?: "",
                                description = charMap["description"] ?: ""
                            )
                            characterRepository.insertCharacter(character)
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors
                    }
                }
            }

            // Create world settings from template
            template.worldSettingsJson?.let { settingsJson ->
                if (settingsJson.isNotBlank()) {
                    try {
                        val settingsType = object : TypeToken<List<Map<String, String>>>() {}.type
                        val settings: List<Map<String, String>> = gson.fromJson(settingsJson, settingsType)
                        settings.forEach { settingMap ->
                            val worldSetting = WorldSetting(
                                storyId = storyId,
                                name = settingMap["name"] ?: "",
                                content = settingMap["content"] ?: "",
                                category = settingMap["category"] ?: "general"
                            )
                            worldSettingRepository.insertWorldSetting(worldSetting)
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors
                    }
                }
            }

            _uiState.update {
                it.copy(
                    showCreateDialog = false,
                    showTemplateDialog = false,
                    showTemplateDetailDialog = false,
                    newStoryTitle = "",
                    newStoryDescription = "",
                    selectedTemplate = null
                )
            }
        }
    }

    private fun deleteStory(storyId: Long) {
        viewModelScope.launch {
            storyRepository.deleteStory(storyId)
        }
    }
}
