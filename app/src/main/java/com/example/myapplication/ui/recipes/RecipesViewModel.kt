package com.example.myapplication.ui.recipes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.UserManager
import com.example.myapplication.data.model.Recipe
import com.example.myapplication.data.model.RecipeInformation
import com.example.myapplication.data.repository.USDARecipeRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomRecipe(
    val id: Long,
    val title: String,
    val ingredients: List<String>,
    val instructions: String,
    val allergens: List<String>,
    val isCustom: Boolean = true
)

@HiltViewModel
class RecipesViewModel @Inject constructor(
    application: Application,
    private val usdaRecipeRepository: USDARecipeRepository
) : AndroidViewModel(application) {

    private val userManager = UserManager.getInstance(application.applicationContext)
    private val gson = Gson()
    private val sharedPreferences = application.getSharedPreferences("recipe_prefs", 0)
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes
    
    private val _customRecipes = MutableLiveData<List<CustomRecipe>>()
    val customRecipes: LiveData<List<CustomRecipe>> = _customRecipes
    
    private val _recipeDetail = MutableLiveData<RecipeInformation>()
    val recipeDetail: LiveData<RecipeInformation> = _recipeDetail
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    init {
        loadCustomRecipes()
        loadRandomRecipes() // Загружаем случайные рецепты при старте
    }
    
    // Загрузка пользовательских рецептов из SharedPreferences
    private fun loadCustomRecipes() {
        val customRecipesJson = sharedPreferences.getString(KEY_CUSTOM_RECIPES, null)
        if (customRecipesJson != null) {
            val type = object : TypeToken<List<CustomRecipe>>() {}.type
            val recipes = gson.fromJson<List<CustomRecipe>>(customRecipesJson, type)
            _customRecipes.value = recipes
        } else {
            _customRecipes.value = emptyList()
        }
    }
    
    // Добавление нового пользовательского рецепта
    fun addCustomRecipe(title: String, ingredients: List<String>, instructions: String, allergens: List<String>) {
        val newRecipe = CustomRecipe(
            id = System.currentTimeMillis(),
            title = title,
            ingredients = ingredients,
            instructions = instructions,
            allergens = allergens
        )
        
        // Обновляем список рецептов
        val currentRecipes = _customRecipes.value?.toMutableList() ?: mutableListOf()
        currentRecipes.add(0, newRecipe) // Добавляем в начало списка
        _customRecipes.value = currentRecipes
        
        // Сохраняем пользовательские рецепты
        saveCustomRecipes()
    }
    
    // Сохранение пользовательских рецептов в SharedPreferences
    private fun saveCustomRecipes() {
        val recipes = _customRecipes.value ?: return
        val recipesJson = gson.toJson(recipes)
        sharedPreferences.edit().putString(KEY_CUSTOM_RECIPES, recipesJson).apply()
    }
    
    // Поиск рецептов по запросу
    fun searchRecipes(query: String) {
        _loading.value = true
        viewModelScope.launch {
            usdaRecipeRepository.searchRecipes(query)
                .onSuccess { recipes ->
                    _recipes.value = recipes
                    if (recipes.isEmpty()) {
                        _error.value = "По вашему запросу ничего не найдено"
                    }
                }
                .onFailure { exception ->
                    _error.value = "Ошибка поиска рецептов: ${exception.message}"
                }
            _loading.value = false
        }
    }
    
    // Получение подробной информации о рецепте
    fun getRecipeDetails(recipeId: Int) {
        _loading.value = true
        viewModelScope.launch {
            usdaRecipeRepository.getRecipeInformation(recipeId)
                .onSuccess { recipeInfo ->
                    _recipeDetail.value = recipeInfo
                }
                .onFailure { exception ->
                    _error.value = "Ошибка получения информации о рецепте: ${exception.message}"
                }
            _loading.value = false
        }
    }
    
    // Поиск безопасных рецептов (без аллергенов пользователя)
    fun findSafeRecipes() {
        val userAllergens = userManager.getAllergens()
        if (userAllergens.isEmpty()) {
            _error.value = "Не указаны аллергены в профиле. Добавьте их в настройках для персонализированного поиска."
            loadRandomRecipes() // Загружаем случайные рецепты, если нет аллергенов
            return
        }
        
        _loading.value = true
        viewModelScope.launch {
            usdaRecipeRepository.searchRecipesWithoutAllergens(userAllergens)
                .onSuccess { recipes ->
                    _recipes.value = recipes
                    if (recipes.isEmpty()) {
                        _error.value = "Не найдено рецептов, которые соответствуют вашим требованиям"
                    }
                }
                .onFailure { exception ->
                    _error.value = "Ошибка поиска безопасных рецептов: ${exception.message}"
                    loadRandomRecipes() // Загружаем случайные рецепты в случае ошибки
                }
            _loading.value = false
        }
    }
    
    // Загрузка случайных рецептов
    private fun loadRandomRecipes() {
        _loading.value = true
        viewModelScope.launch {
            usdaRecipeRepository.getRandomRecipes()
                .onSuccess { recipes ->
                    _recipes.value = recipes
                }
                .onFailure { exception ->
                    _error.value = "Ошибка получения рецептов: ${exception.message}"
                }
            _loading.value = false
        }
    }
    
    companion object {
        private const val KEY_CUSTOM_RECIPES = "custom_recipes"
    }
} 