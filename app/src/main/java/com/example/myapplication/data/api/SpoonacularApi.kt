package com.example.myapplication.data.api

import com.example.myapplication.data.model.Recipe
import com.example.myapplication.data.model.RecipeInformation
import com.example.myapplication.data.model.RecipeSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpoonacularApi {
    
    // Поиск рецептов с учетом аллергий и предпочтений
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("intolerances") intolerances: String? = null, // например: "gluten,dairy"
        @Query("excludeIngredients") excludeIngredients: String? = null,
        @Query("diet") diet: String? = null,
        @Query("number") number: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("instructionsRequired") instructionsRequired: Boolean = true,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("apiKey") apiKey: String
    ): Response<RecipeSearchResponse>
    
    // Получение детальной информации о рецепте
    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: Int,
        @Query("includeNutrition") includeNutrition: Boolean = false,
        @Query("apiKey") apiKey: String
    ): Response<RecipeInformation>
    
    // Поиск рецептов по ингредиентам
    @GET("recipes/findByIngredients")
    suspend fun findRecipesByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 10,
        @Query("ranking") ranking: Int = 1, // 1 = minimize missing ingredients, 2 = maximize used ingredients
        @Query("ignorePantry") ignorePantry: Boolean = true,
        @Query("apiKey") apiKey: String
    ): Response<List<Recipe>>
    
    // Получение случайных рецептов
    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("number") number: Int = 10,
        @Query("tags") tags: String? = null, // например: "vegetarian,dessert"
        @Query("apiKey") apiKey: String
    ): Response<RandomRecipesResponse>
    
    // Поиск рецептов без определенных аллергенов
    @GET("recipes/complexSearch")
    suspend fun searchRecipesWithoutAllergens(
        @Query("intolerances") intolerances: String,
        @Query("number") number: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("instructionsRequired") instructionsRequired: Boolean = true,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("apiKey") apiKey: String
    ): Response<RecipeSearchResponse>
}

data class RandomRecipesResponse(
    val recipes: List<Recipe>
) 