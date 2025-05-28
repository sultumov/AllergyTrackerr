package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.api.ApiConfig
import com.example.myapplication.data.api.USDAFoodApi
import com.example.myapplication.data.api.USDAFoodResponse
import com.example.myapplication.data.model.Recipe
import com.example.myapplication.data.model.RecipeInformation
import com.example.myapplication.data.model.PlaceholderRecipes
import com.example.myapplication.data.service.MLKitTranslationService
import javax.inject.Inject

class USDARecipeRepository @Inject constructor(
    private val usdaApi: USDAFoodApi,
    private val translationService: MLKitTranslationService
) {
    private val TAG = "USDARecipeRepository"
    private val apiKey = ApiConfig.USDA_API_KEY

    // Список нежелательных категорий продуктов
    private val excludedCategories = listOf(
        "alcoholic beverages",
        "alcohol",
        "beer",
        "wine",
        "liquor",
        "beverages",
        "soft drinks",
        "energy drinks",
        "coffee",
        "tea"
    ).map { it.lowercase() }

    // Список нежелательных слов в названиях и ингредиентах
    private val excludedWords = listOf(
        "beer",
        "wine",
        "vodka",
        "liquor",
        "alcohol",
        "whiskey",
        "rum",
        "gin",
        "tequila",
        "brandy",
        "cocktail",
        "soda",
        "cola",
        "energy drink",
        "coffee",
        "caffeine"
    ).map { it.lowercase() }

    private fun isValidFood(food: USDAFoodResponse): Boolean {
        val description = food.description.lowercase()
        val category = food.foodCategory?.lowercase() ?: ""
        val ingredients = food.ingredients?.lowercase() ?: ""

        // Проверяем категорию
        if (excludedCategories.any { category.contains(it) }) {
            return false
        }

        // Проверяем название и ингредиенты
        if (excludedWords.any { word ->
            description.contains(word) || ingredients.contains(word)
        }) {
            return false
        }

        return true
    }

    suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        return try {
            // Переводим запрос на английский для поиска
            val translatedQuery = translationService.translateToEnglish(listOf(query))
                .getOrDefault(listOf(query))
                .first()

            val response = usdaApi.searchFoods(
                query = translatedQuery,
                apiKey = apiKey,
                pageSize = 100,
                requireAllWords = true
            )
            
            // Фильтруем нежелательные продукты
            val filteredFoods = response.foods.filter { isValidFood(it) }
            
            if (filteredFoods.isEmpty()) {
                Log.d(TAG, "Не найдено подходящих рецептов, возвращаем плейсхолдеры")
                return Result.success(PlaceholderRecipes.defaultRecipes)
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
                        extendedIngredients = translatedIngredients?.split(", ")
                    )
                }
                Result.success(recipes)
            } else {
                Log.e(TAG, "Ошибка перевода, возвращаем плейсхолдеры")
                Result.success(PlaceholderRecipes.defaultRecipes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при поиске рецептов: ${e.message}", e)
            Result.success(PlaceholderRecipes.defaultRecipes)
        }
    }

    suspend fun getRandomRecipes(): Result<List<Recipe>> {
        return try {
            val response = usdaApi.getFoodsList(
                apiKey = apiKey,
                dataType = "Foundation,SR Legacy",
                pageSize = 100
            )
            
            // Фильтруем нежелательные продукты
            val filteredFoods = response.filter { isValidFood(it) }
            
            if (filteredFoods.isEmpty()) {
                Log.d(TAG, "Не найдено подходящих рецептов, возвращаем плейсхолдеры")
                return Result.success(PlaceholderRecipes.defaultRecipes)
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
                        extendedIngredients = translatedIngredients?.split(", ")
                    )
                }.take(50)
                
                Result.success(recipes)
            } else {
                Log.e(TAG, "Ошибка перевода, возвращаем плейсхолдеры")
                Result.success(PlaceholderRecipes.defaultRecipes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении случайных рецептов: ${e.message}", e)
            Result.success(PlaceholderRecipes.defaultRecipes)
        }
    }

    suspend fun searchRecipesWithoutAllergens(allergens: List<String>): Result<List<Recipe>> {
        return try {
            // Если список аллергенов пуст, возвращаем плейсхолдеры
            if (allergens.isEmpty()) {
                return Result.success(PlaceholderRecipes.defaultRecipes)
            }

            val response = usdaApi.getFoodsList(
                apiKey = apiKey,
                dataType = "Foundation,SR Legacy",
                pageSize = 100
            )
            
            // Фильтруем нежелательные продукты
            val filteredFoods = response.filter { isValidFood(it) }
            
            // Переводим аллергены на английский для поиска
            val translatedAllergens = translationService.translateToEnglish(allergens)
                .getOrDefault(allergens)
            
            val allergenFilteredFoods = filteredFoods.filter { food ->
                val ingredients = food.ingredients?.lowercase() ?: ""
                translatedAllergens.none { allergen ->
                    ingredients.contains(allergen.lowercase())
                }
            }
            
            if (allergenFilteredFoods.isEmpty()) {
                Log.d(TAG, "Не найдено подходящих рецептов без аллергенов, возвращаем плейсхолдеры")
                return Result.success(PlaceholderRecipes.defaultRecipes)
            }
            
            // Собираем все тексты для перевода
            val textsToTranslate = allergenFilteredFoods.flatMap { food ->
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
                
                val recipes = allergenFilteredFoods.map { food ->
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
                        extendedIngredients = translatedIngredients?.split(", ")
                    )
                }.take(50)
                
                Result.success(recipes)
            } else {
                Log.e(TAG, "Ошибка перевода, возвращаем плейсхолдеры")
                Result.success(PlaceholderRecipes.defaultRecipes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при поиске рецептов без аллергенов: ${e.message}", e)
            Result.success(PlaceholderRecipes.defaultRecipes)
        }
    }

    suspend fun getRecipeInformation(recipeId: Int): Result<RecipeInformation> {
        return try {
            // Проверяем, является ли рецепт плейсхолдером
            val placeholderRecipe = PlaceholderRecipes.defaultRecipes.find { it.id == recipeId }
            if (placeholderRecipe != null) {
                return Result.success(RecipeInformation(
                    id = placeholderRecipe.id,
                    title = placeholderRecipe.title,
                    image = placeholderRecipe.image,
                    servings = placeholderRecipe.servings,
                    readyInMinutes = placeholderRecipe.readyInMinutes,
                    sourceUrl = placeholderRecipe.sourceUrl,
                    summary = placeholderRecipe.summary,
                    cuisines = placeholderRecipe.cuisines,
                    dishTypes = placeholderRecipe.dishTypes,
                    diets = placeholderRecipe.diets,
                    occasions = null,
                    instructions = placeholderRecipe.instructions,
                    analyzedInstructions = placeholderRecipe.analyzedInstructions,
                    extendedIngredients = placeholderRecipe.extendedIngredients,
                    glutenFree = placeholderRecipe.diets?.contains("Без глютена") ?: false,
                    dairyFree = placeholderRecipe.diets?.contains("Без молока") ?: false,
                    vegetarian = placeholderRecipe.diets?.contains("Вегетарианское") ?: false,
                    vegan = placeholderRecipe.diets?.contains("Веганское") ?: false,
                    sustainable = true,
                    cheap = true,
                    veryHealthy = true,
                    veryPopular = false
                ))
            }

            val response = usdaApi.getFoodDetails(
                fdcId = recipeId.toString(),
                apiKey = apiKey
            )
            
            if (!isValidFood(response)) {
                Log.e(TAG, "Продукт недоступен, возвращаем первый плейсхолдер")
                val defaultRecipe = PlaceholderRecipes.defaultRecipes.first()
                return Result.success(RecipeInformation(
                    id = defaultRecipe.id,
                    title = defaultRecipe.title,
                    image = defaultRecipe.image,
                    servings = defaultRecipe.servings,
                    readyInMinutes = defaultRecipe.readyInMinutes,
                    sourceUrl = defaultRecipe.sourceUrl,
                    summary = defaultRecipe.summary,
                    cuisines = defaultRecipe.cuisines,
                    dishTypes = defaultRecipe.dishTypes,
                    diets = defaultRecipe.diets,
                    occasions = null,
                    instructions = defaultRecipe.instructions,
                    analyzedInstructions = defaultRecipe.analyzedInstructions,
                    extendedIngredients = defaultRecipe.extendedIngredients,
                    glutenFree = defaultRecipe.diets?.contains("Без глютена") ?: false,
                    dairyFree = defaultRecipe.diets?.contains("Без молока") ?: false,
                    vegetarian = defaultRecipe.diets?.contains("Вегетарианское") ?: false,
                    vegan = defaultRecipe.diets?.contains("Веганское") ?: false,
                    sustainable = true,
                    cheap = true,
                    veryHealthy = true,
                    veryPopular = false
                ))
            }
            
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
                    extendedIngredients = if (response.ingredients != null) translations[1].split(", ") else null,
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
                Log.e(TAG, "Ошибка перевода, возвращаем первый плейсхолдер")
                val defaultRecipe = PlaceholderRecipes.defaultRecipes.first()
                Result.success(RecipeInformation(
                    id = defaultRecipe.id,
                    title = defaultRecipe.title,
                    image = defaultRecipe.image,
                    servings = defaultRecipe.servings,
                    readyInMinutes = defaultRecipe.readyInMinutes,
                    sourceUrl = defaultRecipe.sourceUrl,
                    summary = defaultRecipe.summary,
                    cuisines = defaultRecipe.cuisines,
                    dishTypes = defaultRecipe.dishTypes,
                    diets = defaultRecipe.diets,
                    occasions = null,
                    instructions = defaultRecipe.instructions,
                    analyzedInstructions = defaultRecipe.analyzedInstructions,
                    extendedIngredients = defaultRecipe.extendedIngredients,
                    glutenFree = defaultRecipe.diets?.contains("Без глютена") ?: false,
                    dairyFree = defaultRecipe.diets?.contains("Без молока") ?: false,
                    vegetarian = defaultRecipe.diets?.contains("Вегетарианское") ?: false,
                    vegan = defaultRecipe.diets?.contains("Веганское") ?: false,
                    sustainable = true,
                    cheap = true,
                    veryHealthy = true,
                    veryPopular = false
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении информации о рецепте: ${e.message}", e)
            val defaultRecipe = PlaceholderRecipes.defaultRecipes.first()
            Result.success(RecipeInformation(
                id = defaultRecipe.id,
                title = defaultRecipe.title,
                image = defaultRecipe.image,
                servings = defaultRecipe.servings,
                readyInMinutes = defaultRecipe.readyInMinutes,
                sourceUrl = defaultRecipe.sourceUrl,
                summary = defaultRecipe.summary,
                cuisines = defaultRecipe.cuisines,
                dishTypes = defaultRecipe.dishTypes,
                diets = defaultRecipe.diets,
                occasions = null,
                instructions = defaultRecipe.instructions,
                analyzedInstructions = defaultRecipe.analyzedInstructions,
                extendedIngredients = defaultRecipe.extendedIngredients,
                glutenFree = defaultRecipe.diets?.contains("Без глютена") ?: false,
                dairyFree = defaultRecipe.diets?.contains("Без молока") ?: false,
                vegetarian = defaultRecipe.diets?.contains("Вегетарианское") ?: false,
                vegan = defaultRecipe.diets?.contains("Веганское") ?: false,
                sustainable = true,
                cheap = true,
                veryHealthy = true,
                veryPopular = false
            ))
        }
    }
} 