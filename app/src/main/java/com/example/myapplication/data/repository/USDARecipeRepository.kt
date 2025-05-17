package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiConfig
import com.example.myapplication.data.api.USDAFoodApi
import com.example.myapplication.data.model.Recipe
import com.example.myapplication.data.model.RecipeInformation
import com.example.myapplication.data.service.TranslationService
import javax.inject.Inject

class USDARecipeRepository @Inject constructor(
    private val usdaApi: USDAFoodApi,
    private val translationService: TranslationService
) {
    private val apiKey = ApiConfig.USDA_API_KEY

    suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        return try {
            val response = usdaApi.searchFoods(
                query = query,
                apiKey = apiKey,
                pageSize = 50,
                requireAllWords = true
            )
            
            // Собираем все тексты для перевода
            val textsToTranslate = response.foods.flatMap { food ->
                listOfNotNull(
                    food.description,
                    food.ingredients,
                    food.foodCategory
                )
            }
            
            // Переводим все тексты за один запрос
            val translations = translationService.translateToRussian(textsToTranslate).getOrNull()
            
            if (translations != null) {
                var translationIndex = 0
                
                // Преобразуем USDA Food в рецепты с переводом
                val recipes = response.foods.map { food ->
                    val translatedTitle = translations[translationIndex++]
                    val translatedIngredients = if (food.ingredients != null) translations[translationIndex++] else null
                    val translatedCategory = if (food.foodCategory != null) translations[translationIndex++] else null
                    
                    Recipe(
                        id = food.fdcId.toInt(),
                        title = translatedTitle,
                        image = null,
                        imageType = null,
                        servings = food.servingSize?.toInt() ?: 1,
                        readyInMinutes = 30,
                        sourceUrl = null,
                        summary = "Ингредиенты: ${translatedIngredients ?: "Нет информации"}",
                        cuisines = null,
                        dishTypes = listOf(translatedCategory ?: "Другое"),
                        diets = emptyList(),
                        instructions = null,
                        analyzedInstructions = null,
                        extendedIngredients = null
                    )
                }
                Result.success(recipes)
            } else {
                // Если перевод не удался, возвращаем оригинальные тексты
                val recipes = response.foods.map { food ->
                    Recipe(
                        id = food.fdcId.toInt(),
                        title = food.description,
                        image = null,
                        imageType = null,
                        servings = food.servingSize?.toInt() ?: 1,
                        readyInMinutes = 30,
                        sourceUrl = null,
                        summary = "Ингредиенты: ${food.ingredients ?: "Нет информации"}",
                        cuisines = null,
                        dishTypes = listOf(food.foodCategory ?: "Другое"),
                        diets = emptyList(),
                        instructions = null,
                        analyzedInstructions = null,
                        extendedIngredients = null
                    )
                }
                Result.success(recipes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRandomRecipes(): Result<List<Recipe>> {
        return try {
            val response = usdaApi.getFoodsList(
                apiKey = apiKey,
                dataType = "Foundation,SR Legacy",
                pageSize = 50
            )
            
            // Собираем все тексты для перевода
            val textsToTranslate = response.flatMap { food ->
                listOfNotNull(
                    food.description,
                    food.ingredients,
                    food.foodCategory
                )
            }
            
            // Переводим все тексты за один запрос
            val translations = translationService.translateToRussian(textsToTranslate).getOrNull()
            
            if (translations != null) {
                var translationIndex = 0
                
                val recipes = response.map { food ->
                    val translatedTitle = translations[translationIndex++]
                    val translatedIngredients = if (food.ingredients != null) translations[translationIndex++] else null
                    val translatedCategory = if (food.foodCategory != null) translations[translationIndex++] else null
                    
                    Recipe(
                        id = food.fdcId.toInt(),
                        title = translatedTitle,
                        image = null,
                        imageType = null,
                        servings = food.servingSize?.toInt() ?: 1,
                        readyInMinutes = 30,
                        sourceUrl = null,
                        summary = "Ингредиенты: ${translatedIngredients ?: "Нет информации"}",
                        cuisines = null,
                        dishTypes = listOf(translatedCategory ?: "Другое"),
                        diets = emptyList(),
                        instructions = null,
                        analyzedInstructions = null,
                        extendedIngredients = null
                    )
                }
                Result.success(recipes)
            } else {
                // Если перевод не удался, возвращаем оригинальные тексты
                val recipes = response.map { food ->
                    Recipe(
                        id = food.fdcId.toInt(),
                        title = food.description,
                        image = null,
                        imageType = null,
                        servings = food.servingSize?.toInt() ?: 1,
                        readyInMinutes = 30,
                        sourceUrl = null,
                        summary = "Ингредиенты: ${food.ingredients ?: "Нет информации"}",
                        cuisines = null,
                        dishTypes = listOf(food.foodCategory ?: "Другое"),
                        diets = emptyList(),
                        instructions = null,
                        analyzedInstructions = null,
                        extendedIngredients = null
                    )
                }
                Result.success(recipes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchRecipesWithoutAllergens(allergens: List<String>): Result<List<Recipe>> {
        return try {
            val response = usdaApi.getFoodsList(
                apiKey = apiKey,
                dataType = "Foundation,SR Legacy",
                pageSize = 100
            )
            
            // Переводим аллергены на английский для поиска
            val translatedAllergens = translationService.translateToRussian(allergens)
                .getOrDefault(allergens)
            
            val filteredFoods = response.filter { food ->
                val ingredients = food.ingredients?.lowercase() ?: ""
                translatedAllergens.none { allergen ->
                    ingredients.contains(allergen.lowercase())
                }
            }
            
            // Собираем все тексты для перевода
            val textsToTranslate = filteredFoods.flatMap { food ->
                listOfNotNull(
                    food.description,
                    food.ingredients,
                    food.foodCategory
                )
            }
            
            // Переводим все тексты за один запрос
            val translations = translationService.translateToRussian(textsToTranslate).getOrNull()
            
            if (translations != null) {
                var translationIndex = 0
                
                val recipes = filteredFoods.map { food ->
                    val translatedTitle = translations[translationIndex++]
                    val translatedIngredients = if (food.ingredients != null) translations[translationIndex++] else null
                    val translatedCategory = if (food.foodCategory != null) translations[translationIndex++] else null
                    
                    Recipe(
                        id = food.fdcId.toInt(),
                        title = translatedTitle,
                        image = null,
                        imageType = null,
                        servings = food.servingSize?.toInt() ?: 1,
                        readyInMinutes = 30,
                        sourceUrl = null,
                        summary = "Ингредиенты: ${translatedIngredients ?: "Нет информации"}",
                        cuisines = null,
                        dishTypes = listOf(translatedCategory ?: "Другое"),
                        diets = emptyList(),
                        instructions = null,
                        analyzedInstructions = null,
                        extendedIngredients = null
                    )
                }.take(50)
                
                Result.success(recipes)
            } else {
                // Если перевод не удался, возвращаем оригинальные тексты
                val recipes = filteredFoods.map { food ->
                    Recipe(
                        id = food.fdcId.toInt(),
                        title = food.description,
                        image = null,
                        imageType = null,
                        servings = food.servingSize?.toInt() ?: 1,
                        readyInMinutes = 30,
                        sourceUrl = null,
                        summary = "Ингредиенты: ${food.ingredients ?: "Нет информации"}",
                        cuisines = null,
                        dishTypes = listOf(food.foodCategory ?: "Другое"),
                        diets = emptyList(),
                        instructions = null,
                        analyzedInstructions = null,
                        extendedIngredients = null
                    )
                }.take(50)
                
                Result.success(recipes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecipeInformation(recipeId: Int): Result<RecipeInformation> {
        return try {
            val response = usdaApi.getFoodDetails(
                fdcId = recipeId.toString(),
                apiKey = apiKey
            )
            
            // Собираем тексты для перевода
            val textsToTranslate = listOfNotNull(
                response.description,
                response.ingredients,
                response.foodCategory
            )
            
            // Переводим тексты
            val translations = translationService.translateToRussian(textsToTranslate).getOrNull()
            
            if (translations != null) {
                var translationIndex = 0
                
                val recipeInfo = RecipeInformation(
                    id = response.fdcId.toInt(),
                    title = translations[translationIndex++],
                    image = null,
                    servings = response.servingSize?.toInt() ?: 1,
                    readyInMinutes = 30,
                    sourceUrl = null,
                    summary = "Ингредиенты: ${if (response.ingredients != null) translations[translationIndex++] else "Нет информации"}",
                    cuisines = null,
                    dishTypes = listOf(if (response.foodCategory != null) translations[translationIndex] else "Другое"),
                    diets = null,
                    occasions = null,
                    instructions = null,
                    analyzedInstructions = null,
                    extendedIngredients = null,
                    glutenFree = response.ingredients?.lowercase()?.contains("wheat")?.not() ?: true,
                    dairyFree = response.ingredients?.lowercase()?.contains("milk")?.not() ?: true,
                    vegetarian = response.ingredients?.lowercase()?.contains("meat")?.not() ?: true,
                    vegan = response.ingredients?.lowercase()?.contains("meat")?.not() ?: true,
                    sustainable = false,
                    cheap = true,
                    veryHealthy = true,
                    veryPopular = false
                )
                Result.success(recipeInfo)
            } else {
                // Если перевод не удался, возвращаем оригинальные тексты
                val recipeInfo = RecipeInformation(
                    id = response.fdcId.toInt(),
                    title = response.description,
                    image = null,
                    servings = response.servingSize?.toInt() ?: 1,
                    readyInMinutes = 30,
                    sourceUrl = null,
                    summary = "Ингредиенты: ${response.ingredients ?: "Нет информации"}",
                    cuisines = null,
                    dishTypes = listOf(response.foodCategory ?: "Другое"),
                    diets = null,
                    occasions = null,
                    instructions = null,
                    analyzedInstructions = null,
                    extendedIngredients = null,
                    glutenFree = response.ingredients?.lowercase()?.contains("wheat")?.not() ?: true,
                    dairyFree = response.ingredients?.lowercase()?.contains("milk")?.not() ?: true,
                    vegetarian = response.ingredients?.lowercase()?.contains("meat")?.not() ?: true,
                    vegan = response.ingredients?.lowercase()?.contains("meat")?.not() ?: true,
                    sustainable = false,
                    cheap = true,
                    veryHealthy = true,
                    veryPopular = false
                )
                Result.success(recipeInfo)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 