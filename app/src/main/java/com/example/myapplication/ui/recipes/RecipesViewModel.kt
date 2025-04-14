package com.example.myapplication.ui.recipes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.data.UserManager

data class Recipe(
    val id: Long,
    val title: String,
    val ingredients: List<String>,
    val instructions: String,
    val allergens: List<String>
)

class RecipesViewModel(application: Application) : AndroidViewModel(application) {

    private val userManager = UserManager.getInstance(application.applicationContext)
    
    private val _safeRecipes = MutableLiveData<List<Recipe>>()
    val safeRecipes: LiveData<List<Recipe>> = _safeRecipes
    
    init {
        loadSafeRecipes()
    }
    
    fun loadSafeRecipes() {
        val userAllergens = userManager.getAllergens()
        val filteredRecipes = getAllRecipes().filter { recipe ->
            recipe.allergens.none { allergen -> allergen in userAllergens }
        }
        _safeRecipes.value = filteredRecipes
    }

    private fun getAllRecipes(): List<Recipe> {
        // Тестовые данные для демонстрации
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
} 