package com.example.myapplication.di

import com.example.myapplication.data.api.USDAFoodApi
import com.example.myapplication.data.repository.USDARecipeRepository
import com.example.myapplication.data.service.MLKitTranslationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideUSDAApi(okHttpClient: OkHttpClient): USDAFoodApi {
        return Retrofit.Builder()
            .baseUrl("https://api.nal.usda.gov/fdc/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(USDAFoodApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTranslationService(): MLKitTranslationService {
        return MLKitTranslationService()
    }

    @Provides
    @Singleton
    fun provideUSDARecipeRepository(
        usdaApi: USDAFoodApi,
        translationService: MLKitTranslationService
    ): USDARecipeRepository {
        return USDARecipeRepository(usdaApi, translationService)
    }
} 