package com.miaobi.app.data.remote

import com.miaobi.app.domain.model.AiRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface AiApiService {
    @POST
    suspend fun chat(
        @Url url: String,
        @Body request: AiRequest
    ): okhttp3.Response
}
