package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.api.AllergenApiService
import com.example.myapplication.data.model.Allergen
import com.example.myapplication.data.model.AllergenCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с данными об аллергенах
 * Объединяет локальные данные и информацию из внешних API
 */
class AllergenRepository(private val context: Context? = null) {
    private val localDataSource = AllergenLocalDataSource(context)
    private val ncbiApi = AllergenApiService.ncbiApi
    private val wikipediaApi = AllergenApiService.wikipediaApi
    
    /**
     * Получение списка всех аллергенов
     */
    fun getAllAllergens(): List<Allergen> {
        return localDataSource.getAllAllergens()
    }
    
    /**
     * Получение аллергенов определенной категории
     */
    fun getAllergensForCategory(category: AllergenCategory): List<Allergen> {
        return localDataSource.getAllergensForCategory(category)
    }
    
    /**
     * Поиск аллергенов по имени
     */
    fun searchAllergensByName(query: String): List<Allergen> {
        return localDataSource.searchAllergensByName(query)
    }
    
    /**
     * Получение аллергена по ID
     */
    fun getAllergenById(id: String): Allergen? {
        return localDataSource.getAllergenById(id)
    }
    
    /**
     * Получение списка всех категорий
     */
    fun getCategories(): List<AllergenCategory> {
        return localDataSource.getCategories()
    }
    
    /**
     * Получение дополнительной научной информации об аллергене из NCBI
     */
    suspend fun getNcbiInfo(query: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = ncbiApi.searchInfo("allergen+$query")
                response.body()?.toString() ?: "Информация не найдена"
            } catch (e: Exception) {
                e.printStackTrace()
                "Ошибка при получении данных: ${e.message}"
            }
        }
    }
    
    /**
     * Получение дополнительной информации из Википедии
     */
    suspend fun getWikipediaInfo(query: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = wikipediaApi.searchInfo(query = query)
                val pages = response.body()?.query?.pages
                
                if (pages != null && pages.isNotEmpty()) {
                    val firstPage = pages.values.firstOrNull()
                    return@withContext firstPage?.extract ?: "Информация не найдена"
                } else {
                    "Информация не найдена"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Ошибка при получении данных: ${e.message}"
            }
        }
    }
}

/**
 * Информация о научной статье
 */
data class ArticleInfo(
    val title: String,
    val source: String,
    val authors: List<String>,
    val abstract: String,
    val publicationDate: String
)

/**
 * Информация из Википедии
 */
data class WikipediaInfo(
    val title: String,
    val content: String,
    val imageUrl: String?
) 