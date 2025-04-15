package com.example.myapplication.data.repository

import com.example.myapplication.data.api.RandomRecipesResponse
import com.example.myapplication.data.api.RecipeApiService
import com.example.myapplication.data.model.Recipe
import com.example.myapplication.data.model.RecipeInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RecipeRepository {
    private val api = RecipeApiService.spoonacularApi
    private val apiKey = RecipeApiService.API_KEY
    
    // Поиск рецептов по запросу с учетом аллергенов
    suspend fun searchRecipes(
        query: String,
        intolerances: List<String>? = null,
        excludeIngredients: List<String>? = null
    ): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val intolerancesStr = intolerances?.joinToString(",")
                val excludeIngredientsStr = excludeIngredients?.joinToString(",")
                val response = api.searchRecipes(
                    query = query,
                    intolerances = intolerancesStr,
                    excludeIngredients = excludeIngredientsStr,
                    apiKey = apiKey
                )
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.results ?: emptyList())
                } else {
                    Result.failure(Exception("Ошибка поиска рецептов: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Получение информации о рецепте по его ID
    suspend fun getRecipeInformation(id: Int): Result<RecipeInformation> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getRecipeInformation(id, apiKey = apiKey)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка получения информации о рецепте: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Поиск рецептов по ингредиентам
    suspend fun findRecipesByIngredients(ingredients: List<String>): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val ingredientsStr = ingredients.joinToString(",")
                val response = api.findRecipesByIngredients(
                    ingredients = ingredientsStr,
                    apiKey = apiKey
                )
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    Result.failure(Exception("Ошибка поиска рецептов по ингредиентам: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Поиск рецептов без определенных аллергенов
    suspend fun searchRecipesWithoutAllergens(
        intolerances: List<String>
    ): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val intolerancesStr = intolerances.joinToString(",")
                val response = api.searchRecipesWithoutAllergens(
                    intolerances = intolerancesStr,
                    apiKey = apiKey
                )
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.results ?: emptyList())
                } else {
                    Result.failure(Exception("Ошибка поиска безопасных рецептов: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Получение случайных рецептов
    suspend fun getRandomRecipes(
        tags: List<String>? = null
    ): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val tagsStr = tags?.joinToString(",")
                val response = api.getRandomRecipes(
                    tags = tagsStr,
                    apiKey = apiKey
                )
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.recipes ?: emptyList())
                } else {
                    Result.failure(Exception("Ошибка получения случайных рецептов: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
} 