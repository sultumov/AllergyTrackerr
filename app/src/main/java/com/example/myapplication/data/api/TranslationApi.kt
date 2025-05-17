package com.example.myapplication.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TranslationApi {
    @POST("translate/v2/translate")
    suspend fun translate(
        @Header("Authorization") apiKey: String,
        @Body request: TranslationRequest
    ): Response<TranslationResponse>
}

data class TranslationRequest(
    val sourceLanguageCode: String = "en",
    val targetLanguageCode: String = "ru",
    val texts: List<String>
)

data class TranslationResponse(
    val translations: List<Translation>
)

data class Translation(
    val text: String,
    val detectedLanguageCode: String? = null
) 