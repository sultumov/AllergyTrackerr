package com.example.myapplication.data.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

interface USDAFoodApi {
    @GET("food/{fdcId}")
    suspend fun getFoodDetails(
        @Path("fdcId") fdcId: String,
        @Query("api_key") apiKey: String
    ): USDAFoodResponse

    @GET("foods/search")
    suspend fun searchFoods(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("pageSize") pageSize: Int = 25
    ): USDASearchResponse
}

data class USDAFoodResponse(
    val fdcId: Long,
    val description: String,
    val foodNutrients: List<USDANutrient>,
    val foodAttributes: List<USDAAttribute>? = null
)

data class USDANutrient(
    val nutrientId: Long,
    val nutrientName: String,
    val value: Double,
    val unitName: String
)

data class USDAAttribute(
    val id: Long,
    val name: String,
    val value: String
)

data class USDASearchResponse(
    val totalHits: Int,
    val currentPage: Int,
    val totalPages: Int,
    val foods: List<USDAFoodResponse>
) 