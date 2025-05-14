package com.example.myapplication.data.repository

import com.example.myapplication.data.api.AllergenApiService
import com.example.myapplication.data.model.Allergen
import com.example.myapplication.data.model.AllergenCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с данными об аллергенах
 * Объединяет локальные данные и информацию из внешних API
 */
class AllergenRepository {
    private val localDataSource = AllergenLocalDataSource()
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
    fun getAllCategories(): List<AllergenCategory> {
        return localDataSource.getAllCategories()
    }
    
    /**
     * Получение дополнительной научной информации об аллергене из NCBI
     */
    suspend fun getScientificInfo(allergen: Allergen): Result<List<ArticleInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val searchTerm = "${allergen.name} allergy"
                val response = ncbiApi.searchArticles(term = searchTerm)
                
                if (response.isSuccessful && response.body() != null) {
                    val articleIds = response.body()?.esearchresult?.idlist ?: emptyList()
                    
                    if (articleIds.isNotEmpty()) {
                        // Получаем информацию о первой статье
                        val firstArticleId = articleIds.first()
                        val summaryResponse = ncbiApi.getArticleSummary(id = firstArticleId)
                        
                        if (summaryResponse.isSuccessful && summaryResponse.body() != null) {
                            val articles = summaryResponse.body()?.result?.uids?.mapNotNull { uid ->
                                val summary = summaryResponse.body()?.result?.documentSummarySet?.get(uid)
                                if (summary != null) {
                                    ArticleInfo(
                                        title = summary.title,
                                        source = summary.source,
                                        authors = summary.authors.map { it.name },
                                        abstract = summary.abstract ?: "Аннотация отсутствует",
                                        publicationDate = summary.pubdate
                                    )
                                } else null
                            } ?: emptyList()
                            
                            Result.success(articles)
                        } else {
                            Result.failure(Exception("Ошибка получения информации о статье: ${summaryResponse.code()}"))
                        }
                    } else {
                        Result.success(emptyList())
                    }
                } else {
                    Result.failure(Exception("Ошибка поиска научных статей: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Получение дополнительной информации из Википедии
     */
    suspend fun getWikipediaInfo(allergen: Allergen): Result<WikipediaInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                val searchTerm = "${allergen.name} аллергия"
                val response = wikipediaApi.searchArticles(search = searchTerm)
                
                if (response.isSuccessful && response.body() != null) {
                    val searchResults = response.body()?.query?.search ?: emptyList()
                    
                    if (searchResults.isNotEmpty()) {
                        // Получаем информацию о первой найденной статье
                        val firstResult = searchResults.first()
                        val contentResponse = wikipediaApi.getArticleContent(titles = firstResult.title)
                        
                        if (contentResponse.isSuccessful && contentResponse.body() != null) {
                            val pages = contentResponse.body()?.query?.pages ?: emptyMap()
                            val page = pages.values.firstOrNull()
                            
                            if (page != null) {
                                val wikipediaInfo = WikipediaInfo(
                                    title = page.title,
                                    content = page.extract ?: "Информация отсутствует",
                                    imageUrl = page.thumbnail?.source
                                )
                                return@withContext Result.success(wikipediaInfo)
                            } else {
                                return@withContext Result.success(null)
                            }
                        } else {
                            return@withContext Result.failure(Exception("Ошибка получения содержимого статьи: ${contentResponse.code()}"))
                        }
                    } else {
                        return@withContext Result.success(null)
                    }
                } else {
                    return@withContext Result.failure(Exception("Ошибка поиска в Википедии: ${response.code()}"))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
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