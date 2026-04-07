package com.miaobi.app.data.remote

import com.google.gson.Gson
import com.miaobi.app.domain.model.AiMessage
import com.miaobi.app.domain.model.AiRequest
import com.miaobi.app.domain.model.AiStreamResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSseClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    fun streamChat(
        apiUrl: String,
        apiKey: String,
        request: AiRequest
    ): Flow<AiStreamResponse> = callbackFlow {
        val fullUrl = "$apiUrl/chat/completions"

        val httpRequest = Request.Builder()
            .url(fullUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(request).toRequestBody("application/json".toMediaType()))
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                try {
                    if (data.trim() == "[DONE]") {
                        trySend(AiStreamResponse(content = "", isFinished = true))
                        close()
                        return
                    }

                    val response = gson.fromJson(data, SseResponse::class.java)
                    val content = response.choices?.firstOrNull()?.delta?.content ?: ""
                    if (content.isNotEmpty()) {
                        trySend(AiStreamResponse(content = content, isFinished = false))
                    }
                } catch (e: Exception) {
                    // Ignore parse errors for incomplete JSON
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                val errorMessage = t?.message ?: response?.message ?: "Unknown error"
                trySend(AiStreamResponse(content = "", isFinished = true, error = errorMessage))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(okHttpClient)
            .newEventSource(httpRequest, listener)

        awaitClose {
            eventSource.cancel()
        }
    }

    suspend fun nonStreamChat(
        apiUrl: String,
        apiKey: String,
        request: AiRequest
    ): AiStreamResponse = withContext(Dispatchers.IO) {
        val fullUrl = "$apiUrl/chat/completions"

        val httpRequest = Request.Builder()
            .url(fullUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(request).toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(httpRequest).execute()
            val body = response.body?.string() ?: ""
            val parsed = gson.fromJson(body, SseResponse::class.java)
            val content = parsed.choices?.firstOrNull()?.message?.content ?: ""
            AiStreamResponse(content = content, isFinished = true)
        } catch (e: Exception) {
            AiStreamResponse(content = "", isFinished = true, error = e.message)
        }
    }
}

data class SseResponse(
    val id: String? = null,
    val choices: List<SseChoice>? = null
)

data class SseChoice(
    val delta: SseDelta? = null,
    val message: SseMessage? = null,
    val finish_reason: String? = null
)

data class SseDelta(
    val content: String? = null
)

data class SseMessage(
    val content: String? = null,
    val role: String? = null
)
