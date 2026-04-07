package com.miaobi.app.domain.model

/**
 * 多分支生成的单条分支选项
 */
data class BranchOption(
    val index: Int,
    val content: String = "",
    val summaryTag: String = "",
    val isSelected: Boolean = false,
    val isGenerating: Boolean = true,
    val error: String? = null
) {
    val canAccept: Boolean
        get() = content.isNotBlank() && !isGenerating && error == null
}

/**
 * 多分支生成的完整状态
 */
data class MultiBranchState(
    val branchCount: Int = 3,
    val branches: List<BranchOption> = emptyList(),
    val selectedBranchIndex: Int = -1,
    val isGenerating: Boolean = false,
    val error: String? = null
) {
    val selectedBranch: BranchOption?
        get() = branches.getOrNull(selectedBranchIndex)

    val canGenerate: Boolean
        get() = !isGenerating && branches.isEmpty()

    val hasResults: Boolean
        get() = branches.isNotEmpty() && branches.none { it.isGenerating }

    val allBranchesDone: Boolean
        get() = branches.isNotEmpty() && branches.none { it.isGenerating }
}
