package com.miaobi.app.domain.model

enum class LengthOption(
    val label: String,
    val minChars: Int,
    val maxChars: Int,
    val maxTokens: Int
) {
    SHORT("短", 100, 200, 300),
    MEDIUM("中", 300, 500, 600),
    LONG("长", 800, 1200, 1500)
}

data class AiRequest(
    val model: String,
    val messages: List<AiMessage>,
    val stream: Boolean = true,
    val temperature: Float = 0.8f,
    val maxTokens: Int = 2000,
    val lengthOption: LengthOption = LengthOption.MEDIUM
)

data class AiMessage(
    val role: String, // system, user, assistant
    val content: String
)

data class AiStreamResponse(
    val content: String,
    val isFinished: Boolean = false,
    val error: String? = null
)

data class AiConfig(
    val apiKey: String = "",
    val apiUrl: String = "https://api.siliconflow.cn/v1",
    val modelName: String = "Qwen/Qwen2.5-7B-Instruct"
)
