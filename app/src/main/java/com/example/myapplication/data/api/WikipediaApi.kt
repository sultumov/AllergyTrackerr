package com.example.myapplication.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Интерфейс для работы с Wikipedia API
 * для получения общей информации об аллергенах
 */
interface WikipediaApi {

    /**
     * Поиск статей в Википедии
     * @param search Поисковый запрос
     * @param limit Ограничение количества результатов
     */
    @GET("api.php")
    suspend fun searchArticles(
        @Query("action") action: String = "query",
        @Query("format") format: String = "json",
        @Query("list") list: String = "search",
        @Query("srsearch") search: String,
        @Query("srlimit") limit: Int = 5
    ): Response<WikiSearchResponse>
    
    /**
     * Получение содержимого статьи
     * @param titles Названия статей
     */
    @GET("api.php")
    suspend fun getArticleContent(
        @Query("action") action: String = "query",
        @Query("format") format: String = "json",
        @Query("prop") prop: String = "extracts|pageimages",
        @Query("exintro") exintro: Boolean = true,
        @Query("explaintext") explaintext: Boolean = true,
        @Query("redirects") redirects: Int = 1,
        @Query("titles") titles: String
    ): Response<WikiContentResponse>
}

/**
 * Ответ от API поиска Википедии
 */
data class WikiSearchResponse(
    val query: WikiQuery
)

data class WikiQuery(
    val search: List<WikiSearchResult>
)

data class WikiSearchResult(
    val pageid: Int,
    val title: String,
    val snippet: String
)

/**
 * Ответ с содержимым статьи из Википедии
 */
data class WikiContentResponse(
    val query: WikiContentQuery
)

data class WikiContentQuery(
    val pages: Map<String, WikiPage>
)

data class WikiPage(
    val pageid: Int,
    val title: String,
    val extract: String?,
    val thumbnail: WikiThumbnail?
)

data class WikiThumbnail(
    val source: String,
    val width: Int,
    val height: Int
) 