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
        @Query("pageSize") pageSize: Int = 25,
        @Query("requireAllWords") requireAllWords: Boolean = true,
        @Query("brandOwner") brandOwner: String? = null
    ): USDASearchResponse

    @GET("foods/list")
    suspend fun getFoodsList(
        @Query("api_key") apiKey: String,
        @Query("dataType") dataType: String = "Foundation,SR Legacy",
        @Query("pageSize") pageSize: Int = 50
    ): List<USDAFoodResponse>
}

data class USDAFoodResponse(
    val fdcId: Long,
    val description: String,
    val foodNutrients: List<USDANutrient>,
    val foodAttributes: List<USDAAttribute>? = null,
    val ingredients: String? = null,
    val brandOwner: String? = null,
    val foodCategory: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null
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