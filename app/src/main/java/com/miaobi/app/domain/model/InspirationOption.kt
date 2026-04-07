package com.miaobi.app.domain.model

/**
 * 单条灵感选项
 */
data class InspirationOption(
    val index: Int,
    val type: InspirationType,
    val title: String,
    val content: String,
    val summaryTag: String = "",
    val isSelected: Boolean = false,
    val isGenerating: Boolean = true,
    val isFavorite: Boolean = false,
    val error: String? = null
) {
    val canAccept: Boolean
        get() = content.isNotBlank() && !isGenerating && error == null
}

/**
 * 灵感生成的完整状态
 */
data class InspirationState(
    val options: List<InspirationOption> = emptyList(),
    val selectedType: InspirationType? = null,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val favorites: Set<Int> = emptySet()
) {
    val selectedOption: InspirationOption?
        get() = options.find { it.isSelected }

    val canGenerate: Boolean
        get() = !isGenerating && options.isEmpty()

    val hasResults: Boolean
        get() = options.isNotEmpty() && options.none { it.isGenerating }

    val allDone: Boolean
        get() = options.isNotEmpty() && options.none { it.isGenerating }

    val filteredOptions: List<InspirationOption>
        get() = if (selectedType != null) {
            options.filter { it.type == selectedType }
        } else {
            options
        }
}
