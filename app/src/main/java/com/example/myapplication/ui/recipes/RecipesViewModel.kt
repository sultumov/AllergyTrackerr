package com.example.myapplication.ui.recipes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class Recipe(
    val id: Long,
    val title: String,
    val ingredients: List<String>,
    val instructions: String,
    val allergens: List<String>
)

class RecipesViewModel : ViewModel() {

    // Для демонстрации используем фиксированный список аллергенов пользователя
    private val userAllergens = listOf("молоко", "арахис")
    
    private val _safeRecipes = MutableLiveData<List<Recipe>>().apply {
        value = getFilteredRecipes()
    }

    val safeRecipes: LiveData<List<Recipe>> = _safeRecipes

    private fun getFilteredRecipes(): List<Recipe> {
        val allRecipes = getAllRecipes()
        return allRecipes.filter { recipe ->
            recipe.allergens.none { allergen -> allergen in userAllergens }
        }
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