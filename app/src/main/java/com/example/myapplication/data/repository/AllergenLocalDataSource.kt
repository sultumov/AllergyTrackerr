package com.example.myapplication.data.repository

import android.content.Context
import android.util.Log
import com.example.myapplication.data.model.Allergen
import com.example.myapplication.data.model.AllergenCategory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Локальный источник данных для аллергенов
 * В будущем может быть заменен на базу данных
 */
class AllergenLocalDataSource(private val context: Context? = null) {
    
    private val TAG = "AllergenLocalDataSource"
    
    // Структура для JSON
    data class AllergenData(
        val categories: List<CategoryData>
    )
    
    data class CategoryData(
        val id: String,
        val name: String,
        val allergens: List<AllergenItemData>
    )
    
    data class AllergenItemData(
        val id: String,
        val name: String,
        val description: String,
        val symptoms: List<String>? = null,
        val avoidanceRecommendations: List<String>? = null,
        val scientificName: String? = null,
        val relatedAllergens: List<String>? = null
    )
    
    /**
     * Получение списка категорий аллергенов
     */
    fun getCategories(): List<AllergenCategory> {
        // Если контекст не передан, возвращаем стандартные категории
        if (context == null) {
            return listOf(
                AllergenCategory.FOOD,
                AllergenCategory.POLLEN,
                AllergenCategory.ANIMAL,
                AllergenCategory.DRUG
            )
        }
        
        try {
            val jsonData = loadJsonFromAsset("allergens.json")
            val allergenData = parseAllergenData(jsonData)
            
            return allergenData.categories.map { category ->
                when (category.id) {
                    "FOOD" -> AllergenCategory.FOOD
                    "POLLEN" -> AllergenCategory.POLLEN
                    "ANIMAL" -> AllergenCategory.ANIMAL
                    "DRUG" -> AllergenCategory.DRUG
                    else -> AllergenCategory.OTHER
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении категорий аллергенов: ${e.message}", e)
            // Возвращаем стандартные категории при ошибке
            return listOf(
                AllergenCategory.FOOD,
                AllergenCategory.POLLEN,
                AllergenCategory.ANIMAL,
                AllergenCategory.DRUG
            )
        }
    }
    
    /**
     * Получение списка всех аллергенов
     */
    fun getAllAllergens(): List<Allergen> {
        return getCategories().flatMap { getAllergensForCategory(it) }
    }

    /**
     * Получение списка аллергенов по категории
     */
    fun getAllergensForCategory(category: AllergenCategory): List<Allergen> {
        // Если контекст не передан, возвращаем стандартные аллергены
        if (context == null) {
            return when (category) {
                AllergenCategory.FOOD -> listOf(
                    Allergen("milk", "Молоко", category, "Аллергия на молоко", emptyList(), emptyList()),
                    Allergen("peanut", "Арахис", category, "Аллергия на арахис", emptyList(), emptyList()),
                    Allergen("egg", "Яйца", category, "Аллергия на яйца", emptyList(), emptyList())
                )
                AllergenCategory.POLLEN -> listOf(
                    Allergen("ragweed_pollen", "Пыльца амброзии", category, "Аллергия на пыльцу амброзии", emptyList(), emptyList()),
                    Allergen("birch_pollen", "Пыльца березы", category, "Аллергия на пыльцу березы", emptyList(), emptyList())
                )
                AllergenCategory.ANIMAL -> listOf(
                    Allergen("cat_dander", "Кошачья шерсть", category, "Аллергия на кошек", emptyList(), emptyList()),
                    Allergen("dog_dander", "Собачья шерсть", category, "Аллергия на собак", emptyList(), emptyList())
                )
                AllergenCategory.DRUG -> listOf(
                    Allergen("penicillin", "Пенициллин", category, "Аллергия на пенициллин", emptyList(), emptyList())
                )
                else -> emptyList()
            }
        }
        
        try {
            val jsonData = loadJsonFromAsset("allergens.json")
            val allergenData = parseAllergenData(jsonData)
            
            val categoryId = when (category) {
                AllergenCategory.FOOD -> "FOOD"
                AllergenCategory.POLLEN -> "POLLEN"
                AllergenCategory.ANIMAL -> "ANIMAL"
                AllergenCategory.DRUG -> "DRUG"
                else -> return emptyList()
            }
            
            val categoryData = allergenData.categories.find { it.id == categoryId } ?: return emptyList()
            
            return categoryData.allergens.map { allergenItem ->
                Allergen(
                    id = allergenItem.id,
                    name = allergenItem.name,
                    category = category,
                    description = allergenItem.description,
                    symptoms = allergenItem.symptoms ?: emptyList(),
                    avoidanceRecommendations = allergenItem.avoidanceRecommendations ?: emptyList(),
                    scientificName = allergenItem.scientificName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении аллергенов по категории $category: ${e.message}", e)
            // Возвращаем стандартные аллергены при ошибке
            return when (category) {
                AllergenCategory.FOOD -> listOf(
                    Allergen("milk", "Молоко", category, "Аллергия на молоко", emptyList(), emptyList()),
                    Allergen("peanut", "Арахис", category, "Аллергия на арахис", emptyList(), emptyList())
                )
                else -> emptyList()
            }
        }
    }
    
    /**
     * Поиск аллергенов по имени
     */
    fun searchAllergensByName(query: String): List<Allergen> {
        val allAllergens = getAllAllergens()
        return allAllergens.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.description.contains(query, ignoreCase = true) 
        }
    }
    
    /**
     * Получение аллергена по ID
     */
    fun getAllergenById(id: String): Allergen? {
        // Если контекст не передан, ищем в стандартных аллергенах
        if (context == null) {
            return getAllAllergens().find { it.id == id }
        }
        
        try {
            val jsonData = loadJsonFromAsset("allergens.json")
            val allergenData = parseAllergenData(jsonData)
            
            for (categoryData in allergenData.categories) {
                val allergenItem = categoryData.allergens.find { it.id == id }
                if (allergenItem != null) {
                    val category = when (categoryData.id) {
                        "FOOD" -> AllergenCategory.FOOD
                        "POLLEN" -> AllergenCategory.POLLEN
                        "ANIMAL" -> AllergenCategory.ANIMAL
                        "DRUG" -> AllergenCategory.DRUG
                        else -> AllergenCategory.OTHER
                    }
                    
                    return Allergen(
                        id = allergenItem.id,
                        name = allergenItem.name,
                        category = category,
                        description = allergenItem.description,
                        symptoms = allergenItem.symptoms ?: emptyList(),
                        avoidanceRecommendations = allergenItem.avoidanceRecommendations ?: emptyList(),
                        relatedAllergens = allergenItem.relatedAllergens ?: emptyList(),
                        scientificName = allergenItem.scientificName
                    )
                }
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении аллергена по ID $id: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Вспомогательный метод для загрузки JSON из assets
     */
    private fun loadJsonFromAsset(fileName: String): String {
        return try {
            // Используем UTF-8 кодировку для корректного чтения русского текста
            context?.assets?.open(fileName)?.bufferedReader(StandardCharsets.UTF_8).use { it?.readText() } ?: "{}"
        } catch (ex: IOException) {
            Log.e(TAG, "Ошибка при чтении файла $fileName: ${ex.message}", ex)
            "{}"
        }
    }
    
    /**
     * Вспомогательный метод для парсинга JSON
     */
    private fun parseAllergenData(jsonString: String): AllergenData {
        val gson = Gson()
        val type = object : TypeToken<AllergenData>() {}.type
        return try {
            gson.fromJson(jsonString, type) ?: AllergenData(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при парсинге JSON: ${e.message}", e)
            AllergenData(emptyList())
        }
    }
} 