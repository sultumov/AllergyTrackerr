package com.example.myapplication.data.repository

import android.content.Context
import android.util.Log
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
    private val TAG = "AllergenRepository"
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
                val response = ncbiApi.searchInfo(term = "allergen+$query")
                if (response.isSuccessful && response.body() != null) {
                    val searchResult = response.body()
                    val idList = searchResult?.esearchresult?.idlist ?: emptyList()
                    
                    if (idList.isNotEmpty()) {
                        // Получаем информацию о первой статье
                        val articleId = idList.first()
                        val summaryResponse = ncbiApi.getArticleSummary(id = articleId)
                        
                        if (summaryResponse.isSuccessful && summaryResponse.body() != null) {
                            val summary = summaryResponse.body()?.result?.documentSummarySet?.get(articleId)
                            if (summary != null) {
                                "Название: ${summary.title}\n\n" +
                                "Источник: ${summary.source}\n\n" +
                                "Дата: ${summary.pubdate}\n\n" +
                                (summary.abstract ?: "Аннотация отсутствует")
                            } else {
                                "Детальная информация по запросу \"$query\" не найдена"
                            }
                        } else {
                            "Ошибка при получении детальной информации: ${summaryResponse.code()}"
                        }
                    } else {
                        "Информация по запросу \"$query\" не найдена"
                    }
                } else {
                    "Ошибка при поиске информации: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при получении научной информации: ${e.message}", e)
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
                if (response.isSuccessful && response.body() != null) {
                    val pages = response.body()?.query?.pages
                    
                    if (pages != null && pages.isNotEmpty()) {
                        val firstPage = pages.values.firstOrNull()
                        return@withContext firstPage?.extract ?: "Информация не найдена"
                    } else {
                        "Информация не найдена"
                    }
                } else {
                    "Ошибка при получении данных: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при получении информации из Википедии: ${e.message}", e)
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