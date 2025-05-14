package com.example.myapplication.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Сервис для настройки API для работы с информацией об аллергенах
 */
object AllergenApiService {
    
    // URLs для API
    private const val NCBI_BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/"
    private const val WIKIPEDIA_BASE_URL = "https://ru.wikipedia.org/api/"
    
    // Настройка логирования запросов
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // Настройка клиента OkHttp
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Retrofit для NCBI API
    private val ncbiRetrofit = Retrofit.Builder()
        .baseUrl(NCBI_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Retrofit для Wikipedia API
    private val wikipediaRetrofit = Retrofit.Builder()
        .baseUrl(WIKIPEDIA_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // API клиенты
    val ncbiApi: NCBIApi = ncbiRetrofit.create(NCBIApi::class.java)
    val wikipediaApi: WikipediaApi = wikipediaRetrofit.create(WikipediaApi::class.java)
} 