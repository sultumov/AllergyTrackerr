package com.example.myapplication.data.service

import android.util.Log
import com.example.myapplication.data.api.TranslationApi
import com.example.myapplication.data.api.TranslationRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationService @Inject constructor(
    private val translationApi: TranslationApi
) {
    companion object {
        private const val TAG = "TranslationService"
        private const val API_KEY = "Bearer t1.9euelZqLkpOQmZiTlJGKyY6Vl5KVle3rnpWajZqNx5eTi4qKkZWRjJCNkpbl8_dEFWFg-e8PFEp03fP3B0RfX_nvDxRKdA.VxAYqt_svCN_Tg_EXgYqxlcCSqxQYqKyqGXlGxQYTYeHGXBwPxQEfGJvZGJQxTWnxhGhG-1THEfLxGXyBg"
    }

    suspend fun translateToRussian(texts: List<String>): Result<List<String>> {
        return try {
            if (texts.isEmpty()) {
                Log.d(TAG, "Пустой список текстов для перевода")
                return Result.success(emptyList())
            }

            val request = TranslationRequest(
                sourceLanguageCode = "en",
                targetLanguageCode = "ru",
                texts = texts,
                folderId = ""
            )
            
            Log.d(TAG, "Отправка запроса на перевод: ${texts.size} текстов")
            val response = translationApi.translate(API_KEY, request)
            
            if (response.isSuccessful && response.body() != null) {
                val translations = response.body()!!.translations.map { it.text }
                Log.d(TAG, "Успешно переведено ${translations.size} текстов")
                Result.success(translations)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMessage = "Ошибка перевода: ${response.code()} - ${response.message()} - $errorBody"
                Log.e(TAG, errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при переводе: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun translateToRussian(text: String): Result<String> {
        return translateToRussian(listOf(text)).map { it.first() }
    }

    suspend fun translateToEnglish(texts: List<String>): Result<List<String>> {
        return try {
            if (texts.isEmpty()) {
                Log.d(TAG, "Пустой список текстов для перевода")
                return Result.success(emptyList())
            }

            val request = TranslationRequest(
                sourceLanguageCode = "ru",
                targetLanguageCode = "en",
                texts = texts
            )
            
            Log.d(TAG, "Отправка запроса на перевод: ${texts.size} текстов")
            val response = translationApi.translate(API_KEY, request)
            
            if (response.isSuccessful && response.body() != null) {
                val translations = response.body()!!.translations.map { it.text }
                Log.d(TAG, "Успешно переведено ${translations.size} текстов")
                Result.success(translations)
            } else {
                val errorMessage = "Ошибка перевода: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при переводе: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun translateToEnglish(text: String): Result<String> {
        return translateToEnglish(listOf(text)).map { it.first() }
    }
} 