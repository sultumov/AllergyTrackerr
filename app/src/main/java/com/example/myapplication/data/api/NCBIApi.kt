package com.example.myapplication.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Интерфейс для работы с API NCBI (National Center for Biotechnology Information)
 * для получения медицинской информации об аллергенах
 */
interface NCBIApi {

    /**
     * Поиск статей по аллергенам
     * @param term Поисковый запрос
     * @param retmax Максимальное количество результатов
     */
    @GET("esearch.fcgi")
    suspend fun searchArticles(
        @Query("db") db: String = "pubmed",
        @Query("term") term: String,
        @Query("retmax") retmax: Int = 10,
        @Query("retmode") retmode: String = "json",
        @Query("sort") sort: String = "relevance"
    ): Response<NCBISearchResponse>
    
    /**
     * Получение информации о статье по ID
     * @param id ID статьи
     */
    @GET("esummary.fcgi")
    suspend fun getArticleSummary(
        @Query("db") db: String = "pubmed",
        @Query("id") id: String,
        @Query("retmode") retmode: String = "json"
    ): Response<NCBISummaryResponse>
}

/**
 * Ответ от API поиска
 */
data class NCBISearchResponse(
    val esearchresult: ESearchResult
)

data class ESearchResult(
    val count: String,
    val retmax: String,
    val retstart: String,
    val idlist: List<String>
)

/**
 * Ответ с кратким содержанием статьи
 */
data class NCBISummaryResponse(
    val result: NCBIResult
)

data class NCBIResult(
    val uids: List<String>,
    val documentSummarySet: Map<String, DocumentSummary>
)

data class DocumentSummary(
    val title: String,
    val source: String,
    val pubdate: String,
    val authors: List<Author>,
    val abstract: String?
)

data class Author(
    val name: String
) 