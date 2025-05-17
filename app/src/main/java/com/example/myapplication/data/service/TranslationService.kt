package com.example.myapplication.data.service

import com.example.myapplication.data.api.TranslationApi
import com.example.myapplication.data.api.TranslationRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationService @Inject constructor(
    private val translationApi: TranslationApi
) {
    companion object {
        private const val API_KEY = "Bearer t1.9euelZqLkpOQmZiTlJGKyY6Vl5KVle3rnpWajZqNx5eTi4qKkZWRjJCNkpbl8_dEFWFg-e8PFEp03fP3B0RfX_nvDxRKdA.VxAYqt_svCN_Tg_EXgYqxlcCSqxQYqKyqGXlGxQYTYeHGXBwPxQEfGJvZGJQxTWnxhGhG-1THEfLxGXyBg"
    }

    suspend fun translateToRussian(texts: List<String>): Result<List<String>> {
        return try {
            val request = TranslationRequest(texts = texts)
            val response = translationApi.translate(API_KEY, request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.translations.map { it.text })
            } else {
                Result.failure(Exception("Ошибка перевода: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun translateToRussian(text: String): Result<String> {
        return translateToRussian(listOf(text)).map { it.first() }
    }
} 