package com.example.myapplication.ui.recipes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.data.UserManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Recipe(
    val id: Long,
    val title: String,
    val ingredients: List<String>,
    val instructions: String,
    val allergens: List<String>,
    val isCustom: Boolean = false // Флаг для пользовательских рецептов
)

class RecipesViewModel(application: Application) : AndroidViewModel(application) {

    private val userManager = UserManager.getInstance(application.applicationContext)
    private val gson = Gson()
    private val sharedPreferences = application.getSharedPreferences("recipe_prefs", 0)
    
    private val _allRecipes = MutableLiveData<List<Recipe>>()
    private val _safeRecipes = MutableLiveData<List<Recipe>>()
    val safeRecipes: LiveData<List<Recipe>> = _safeRecipes
    
    init {
        loadRecipes()
    }
    
    private fun loadRecipes() {
        // Загружаем пользовательские рецепты
        val customRecipesJson = sharedPreferences.getString(KEY_CUSTOM_RECIPES, null)
        val customRecipes = if (customRecipesJson != null) {
            val type = object : TypeToken<List<Recipe>>() {}.type
            gson.fromJson<List<Recipe>>(customRecipesJson, type)
        } else {
            emptyList()
        }
        
        // Объединяем с предустановленными рецептами
        val allRecipes = customRecipes + getDefaultRecipes()
        _allRecipes.value = allRecipes
        
        // Фильтруем безопасные рецепты
        filterSafeRecipes()
    }
    
    fun loadSafeRecipes() {
        filterSafeRecipes()
    }
    
    private fun filterSafeRecipes() {
        val userAllergens = userManager.getAllergens()
        val allRecipesList = _allRecipes.value ?: emptyList()
        
        _safeRecipes.value = allRecipesList.filter { recipe ->
            recipe.allergens.none { allergen -> allergen in userAllergens }
        }
    }
    
    fun addRecipe(title: String, ingredients: List<String>, instructions: String, allergens: List<String>) {
        val newRecipe = Recipe(
            id = System.currentTimeMillis(),
            title = title,
            ingredients = ingredients,
            instructions = instructions,
            allergens = allergens,
            isCustom = true
        )
        
        // Обновляем списки рецептов
        val currentAllRecipes = _allRecipes.value?.toMutableList() ?: mutableListOf()
        currentAllRecipes.add(0, newRecipe) // Добавляем в начало списка
        _allRecipes.value = currentAllRecipes
        
        // Сохраняем пользовательские рецепты
        saveCustomRecipes()
        
        // Обновляем безопасные рецепты
        filterSafeRecipes()
    }
    
    private fun saveCustomRecipes() {
        val allRecipesList = _allRecipes.value ?: return
        val customRecipes = allRecipesList.filter { it.isCustom }
        
        val recipesJson = gson.toJson(customRecipes)
        sharedPreferences.edit().putString(KEY_CUSTOM_RECIPES, recipesJson).apply()
    }
    
    private fun getDefaultRecipes(): List<Recipe> {
        // Стандартные рецепты
        return listOf(
            Recipe(
                1,
                "Овощной салат",
                listOf("огурец", "помидор", "лук", "оливковое масло", "соль"),
                "1. Нарежьте овощи. 2. Смешайте в миске. 3. Заправьте маслом и солью.",
                listOf()
            ),
            Recipe(
                2,
                "Фруктовый салат",
                listOf("яблоко", "груша", "виноград", "мед"),
                "1. Нарежьте фрукты. 2. Смешайте в миске. 3. Добавьте немного меда.",
                listOf("мед")
            ),
            Recipe(
                3,
                "Молочный коктейль",
                listOf("молоко", "банан", "сахар", "ваниль"),
                "1. Смешайте все ингредиенты в блендере. 2. Взбивайте до однородной массы.",
                listOf("молоко")
            ),
            Recipe(
                4,
                "Арахисовое печенье",
                listOf("мука", "масло", "яйца", "арахис", "сахар"),
                "1. Смешайте все ингредиенты. 2. Сформируйте печенье. 3. Выпекайте 10-15 минут.",
                listOf("глютен", "яйца", "арахис")
            ),
            Recipe(
                5,
                "Рисовая каша с фруктами",
                listOf("рис", "вода", "яблоко", "корица", "мед"),
                "1. Отварите рис. 2. Добавьте нарезанные яблоки и корицу. 3. По желанию добавьте мед.",
                listOf("мед")
            )
        )
    }
    
    companion object {
        private const val KEY_CUSTOM_RECIPES = "custom_recipes"
    }
} 