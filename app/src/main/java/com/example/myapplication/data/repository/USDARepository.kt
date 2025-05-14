package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiConfig
import com.example.myapplication.data.api.USDAFoodApi
import com.example.myapplication.data.api.USDAFoodResponse
import com.example.myapplication.data.api.USDASearchResponse
import javax.inject.Inject

class USDARepository @Inject constructor(
    private val usdaApi: USDAFoodApi
) {
    private val apiKey = ApiConfig.USDA_API_KEY

    suspend fun searchFood(query: String): Result<USDASearchResponse> {
        return try {
            val response = usdaApi.searchFoods(query, apiKey)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFoodDetails(fdcId: String): Result<USDAFoodResponse> {
        return try {
            val response = usdaApi.getFoodDetails(fdcId, apiKey)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllergenInfo(foodResponse: USDAFoodResponse): List<String> {
        val allergens = mutableListOf<String>()
        
        foodResponse.foodAttributes?.forEach { attribute ->
            when (attribute.name.toLowerCase()) {
                "contains_milk" -> if (attribute.value == "true") allergens.add("Молоко")
                "contains_egg" -> if (attribute.value == "true") allergens.add("Яйца")
                "contains_fish" -> if (attribute.value == "true") allergens.add("Рыба")
                "contains_shellfish" -> if (attribute.value == "true") allergens.add("Моллюски")
                "contains_tree_nuts" -> if (attribute.value == "true") allergens.add("Орехи")
                "contains_peanuts" -> if (attribute.value == "true") allergens.add("Арахис")
                "contains_wheat" -> if (attribute.value == "true") allergens.add("Пшеница")
                "contains_soybeans" -> if (attribute.value == "true") allergens.add("Соя")
                "contains_sesame" -> if (attribute.value == "true") allergens.add("Кунжут")
            }
        }
        
        return allergens
    }
} 