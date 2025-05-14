package com.example.myapplication.data.api

import com.example.myapplication.data.model.Recipe
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RecipeApiService {
    private const val BASE_URL = "https://api.spoonacular.com/"
    
    // API ключ для Spoonacular. В реальном приложении его нужно хранить в защищенном месте, 
    // например в BuildConfig или через Android Keystore
    const val API_KEY = "4f524550f50947c5a23ba1dcf8cdce5b"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val spoonacularApi: SpoonacularApi = retrofit.create(SpoonacularApi::class.java)
} 