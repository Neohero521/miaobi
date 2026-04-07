package com.miaobi.app.data.remote

import com.miaobi.app.domain.model.AiRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface AiApiService {
    @POST("")
    suspend fun chat(
        @Url url: String,
        @Body request: AiRequest
    ): Response<ResponseBody>
}
