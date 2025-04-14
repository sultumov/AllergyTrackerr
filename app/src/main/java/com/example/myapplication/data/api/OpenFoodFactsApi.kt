package com.example.myapplication.data.api

import com.example.myapplication.data.model.ProductResponse
import com.example.myapplication.data.model.ProductSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    
    // Получение информации о продукте по штрих-коду
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<ProductResponse>
    
    // Поиск продуктов по названию
    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") searchTerms: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<ProductSearchResponse>
    
    // Поиск продуктов по категории
    @GET("cgi/search.pl")
    suspend fun searchProductsByCategory(
        @Query("tagtype_0") tagType: String = "categories",
        @Query("tag_contains_0") tagContains: String = "contains",
        @Query("tag_0") category: String,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<ProductSearchResponse>
    
    // Поиск продуктов, не содержащих определенные аллергены
    @GET("cgi/search.pl")
    suspend fun searchProductsWithoutAllergens(
        @Query("tagtype_0") tagType: String = "allergens",
        @Query("tag_contains_0") tagContains: String = "does_not_contain",
        @Query("tag_0") allergen: String,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<ProductSearchResponse>
} 