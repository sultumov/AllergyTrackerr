package com.example.myapplication.data.service

import android.util.Log
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MLKitTranslationService @Inject constructor() {
    companion object {
        private const val TAG = "MLKitTranslationService"
    }

    private val englishRussianTranslator by lazy {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH)
            .setTargetLanguage(com.google.mlkit.nl.translate.TranslateLanguage.RUSSIAN)
            .build()
        Translation.getClient(options)
    }

    private val russianEnglishTranslator by lazy {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(com.google.mlkit.nl.translate.TranslateLanguage.RUSSIAN)
            .setTargetLanguage(com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH)
            .build()
        Translation.getClient(options)
    }

    suspend fun translateToRussian(texts: List<String>): Result<List<String>> {
        return try {
            if (texts.isEmpty()) {
                Log.d(TAG, "Пустой список текстов для перевода")
                return Result.success(emptyList())
            }

            // Убедимся, что модель загружена
            suspendCancellableCoroutine { continuation ->
                englishRussianTranslator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        continuation.resume(Unit)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Ошибка загрузки модели: ${exception.message}")
                        continuation.resume(Unit)
                    }
            }

            // Переводим каждый текст
            val translations = texts.map { text ->
                suspendCancellableCoroutine { continuation ->
                    englishRussianTranslator.translate(text)
                        .addOnSuccessListener { translatedText ->
                            continuation.resume(translatedText)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Ошибка перевода: ${exception.message}")
                            continuation.resume(text) // В случае ошибки возвращаем оригинальный текст
                        }
                }
            }

            Result.success(translations)
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

            // Убедимся, что модель загружена
            suspendCancellableCoroutine { continuation ->
                russianEnglishTranslator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        continuation.resume(Unit)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Ошибка загрузки модели: ${exception.message}")
                        continuation.resume(Unit)
                    }
            }

            // Переводим каждый текст
            val translations = texts.map { text ->
                suspendCancellableCoroutine { continuation ->
                    russianEnglishTranslator.translate(text)
                        .addOnSuccessListener { translatedText ->
                            continuation.resume(translatedText)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Ошибка перевода: ${exception.message}")
                            continuation.resume(text) // В случае ошибки возвращаем оригинальный текст
                        }
                }
            }

            Result.success(translations)
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при переводе: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun translateToEnglish(text: String): Result<String> {
        return translateToEnglish(listOf(text)).map { it.first() }
    }

    fun close() {
        englishRussianTranslator.close()
        russianEnglishTranslator.close()
    }
} 