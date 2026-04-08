package com.miaobi.app.ui.screens.writing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaobi.app.domain.model.AiStreamResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * =================================══════════════════════════════════════
 * AI 任务管理器 - 参考 Claude Code AsyncGenerator 任务管理模式
 * =================================══════════════════════════════════════
 * 
 * Claude Code 使用 AsyncGenerator 实现流式 AI 生成：
 * - 统一的 Job 管理，支持取消
 * - 流式累积结果
 * - 状态机追踪生成阶段
 * 
 * 我们借鉴这个模式，实现统一的 AI 任务管理
 */

/**
 * AI 任务类型
 */
enum class AiTaskType {
    CONTINUATION,   // 续写
    REWRITE,        // 改写
    MULTI_BRANCH,   // 多分支
    INSPIRATION,    // 灵感
}

/**
 * AI 任务配置
 */
data class AiTaskConfig(
    val type: AiTaskType,
    val userPrompt: String = "",
    val selectedText: String = "",
    val lengthOption: com.miaobi.app.domain.model.LengthOption = com.miaobi.app.domain.model.LengthOption.MEDIUM,
    val rewriteStyle: com.miaobi.app.domain.model.RewriteStyle? = null,
    val branchCount: Int = 3,
    val inspirationType: com.miaobi.app.domain.model.InspirationType? = null,
)

/**
 * AI 任务结果
 */
sealed class AiTaskResult {
    data class Success(val content: String, val extras: Map<String, Any?> = emptyMap()) : AiTaskResult()
    data class Error(val message: String) : AiTaskResult()
    object Cancelled : AiTaskResult()
}

/**
 * 统一的 AI 任务管理器
 * 
 * 功能：
 * 1. 统一管理所有 AI 生成 Job
 * 2. 自动取消上一个同类型任务
 * 3. 流式累积结果
 * 4. 状态机追踪
 */
class AiTaskManager(
    private val viewModelScope: kotlinx.coroutines.CoroutineScope,
    private val onStateUpdate: (AiTaskType, AiTaskState) -> Unit,
    private val onContinuationSuggestions: (List<com.miaobi.app.domain.model.ContinuationSuggestion>) -> Unit,
) {
    // 当前活跃的 Job
    private var currentJob: Job? = null
    
    // 各类型任务的最新状态
    private val taskStates = mutableMapOf<AiTaskType, AiTaskState>()
    
    /**
     * 启动 AI 任务
     * @param config 任务配置
     * @param flowProvider Flow 提供者（懒加载，避免循环依赖）
     * @param accumulator 可选的累积器，用于流式处理
     */
    fun startTask(
        config: AiTaskConfig,
        flowProvider: suspend () -> Flow<AiStreamResponse>,
        accumulator: ((String, AiStreamResponse) -> String)? = null,
    ) {
        // 取消同类型旧任务
        cancelTask(config.type)
        
        // 更新状态为生成中
        updateState(config.type, AiTaskState.Generating())
        
        currentJob = viewModelScope.launch {
            try {
                val accumulated = StringBuilder()
                
                flowProvider().collect { response ->
                    if (response.error != null) {
                        updateState(config.type, AiTaskState.Error(response.error))
                        return@collect
                    }
                    
                    if (response.isFinished) {
                        val finalContent = if (accumulator != null) {
                            accumulator(accumulated.toString(), response)
                        } else {
                            accumulated.toString() + response.content
                        }
                        
                        updateState(config.type, AiTaskState.Completed(finalContent))
                    } else {
                        accumulated.append(response.content)
                        updateState(
                            config.type, 
                            AiTaskState.Generating(
                                accumulated = accumulated.toString(),
                                stage = GenerationStage.GENERATING
                            )
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                updateState(config.type, AiTaskState.Idle)
                throw e
            } catch (t: Throwable) {
                updateState(config.type, AiTaskState.Error(t.message ?: "未知错误"))
            }
        }
    }
    
    /**
     * 启动续写任务
     */
    fun startContinuation(
        config: AiTaskConfig,
        flowProvider: suspend () -> Flow<AiStreamResponse>,
    ) {
        startTask(config, flowProvider) { accumulated, response ->
            // 续写使用累积的内容（不包含最后一次，因为那是 finished signal）
            accumulated
        }
    }
    
    /**
     * 启动改写任务
     */
    fun startRewrite(
        config: AiTaskConfig,
        flowProvider: suspend () -> Flow<AiStreamResponse>,
    ) {
        startTask(config, flowProvider) { accumulated, _ ->
            accumulated
        }
    }
    
    /**
     * 取消指定类型的任务
     */
    fun cancelTask(type: AiTaskType) {
        if (isTaskActive(type)) {
            currentJob?.cancel()
            currentJob = null
            updateState(type, AiTaskState.Idle)
        }
    }
    
    /**
     * 取消所有任务
     */
    fun cancelAll() {
        currentJob?.cancel()
        currentJob = null
        taskStates.keys.forEach { type ->
            updateState(type, AiTaskState.Idle)
        }
    }
    
    /**
     * 检查指定类型的任务是否进行中
     */
    fun isTaskActive(type: AiTaskType): Boolean {
        return taskStates[type] is AiTaskState.Generating
    }
    
    /**
     * 获取指定类型任务的状态
     */
    fun getTaskState(type: AiTaskType): AiTaskState {
        return taskStates[type] ?: AiTaskState.Idle
    }
    
    /**
     * 重置指定类型任务状态
     */
    fun resetTask(type: AiTaskType) {
        cancelTask(type)
        updateState(type, AiTaskState.Idle)
    }
    
    /**
     * 更新状态并通知
     */
    private fun updateState(type: AiTaskType, state: AiTaskState) {
        taskStates[type] = state
        onStateUpdate(type, state)
    }
    
    /**
     * 清理累积的建议（任务完成后调用）
     */
    fun clearSuggestions() {
        onContinuationSuggestions(emptyList())
    }
}

/**
 * 简化版续写任务处理器
 * 适用于不需要完整 AiTaskManager 的场景
 */
class ContinuationTaskHandler(
    private val viewModelScope: kotlinx.coroutines.CoroutineScope,
    private val onGenerating: (String) -> Unit,
    private val onComplete: (List<com.miaobi.app.domain.model.ContinuationSuggestion>, String) -> Unit,
    private val onError: (String) -> Unit,
    private val onCancelled: () -> Unit,
) {
    private var job: Job? = null
    
    fun start(flowProvider: suspend () -> Flow<AiStreamResponse>) {
        cancel()
        
        job = viewModelScope.launch {
            try {
                val accumulated = StringBuilder()
                
                flowProvider().collect { response ->
                    if (response.error != null) {
                        onError(response.error)
                        return@collect
                    }
                    
                    if (response.isFinished) {
                        val suggestions = parseSuggestions(accumulated.toString())
                        onComplete(suggestions, accumulated.toString())
                    } else {
                        accumulated.append(response.content)
                        onGenerating(accumulated.toString())
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                onCancelled()
                throw e
            } catch (t: Throwable) {
                onError(t.message ?: "未知错误")
            }
        }
    }
    
    fun cancel() {
        job?.cancel()
        job = null
    }
    
    val isActive: Boolean get() = job?.isActive == true
    
    /**
     * 解析续写建议
     * 优先解析 ===方向X=== 分隔符，否则均分
     */
    private fun parseSuggestions(content: String): List<com.miaobi.app.domain.model.ContinuationSuggestion> {
        if (content.isBlank()) return emptyList()
        
        val directionRegex = Regex("""===方向(\d+)===""")
        val parts = content.split(directionRegex)
        
        if (parts.size >= 4) {
            val directions = listOf("剧情升级", "情感互动", "意外转折")
            val suggestions = listOf(1, 2, 3).mapNotNull { index ->
                val partContent = parts.getOrNull(index)?.trim() ?: return@mapNotNull null
                if (partContent.isBlank()) return@mapNotNull null
                com.miaobi.app.domain.model.ContinuationSuggestion(
                    id = index - 1,
                    content = partContent,
                    wordCount = partContent.length,
                    isSelected = index == 1,
                    directionLabel = "方向$index：${directions[index - 1]}"
                )
            }
            if (suggestions.isNotEmpty()) return suggestions
        }
        
        // 回退：均分内容为3段
        val totalLength = content.length
        val partSize = totalLength / 3
        val directions = listOf("剧情升级", "情感互动", "意外转折")
        
        return listOf(0, 1, 2).map { index ->
            val start = index * partSize
            val end = if (index == 2) totalLength else (index + 1) * partSize
            val partContent = content.substring(start, end.coerceAtMost(totalLength)).trim()
            com.miaobi.app.domain.model.ContinuationSuggestion(
                id = index,
                content = partContent,
                wordCount = partContent.length,
                isSelected = index == 0,
                directionLabel = "方向${index + 1}：${directions[index]}"
            )
        }.filter { it.content.isNotBlank() }
    }
}
