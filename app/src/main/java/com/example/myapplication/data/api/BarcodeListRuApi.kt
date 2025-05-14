package com.example.myapplication.data.api

import com.example.myapplication.data.model.BarcodeListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Интерфейс для работы с API сайта barcode-list.ru
 * для получения информации о российских товарах по штрих-коду
 */
interface BarcodeListRuApi {
    
    /**
     * Получение информации о продукте по штрих-коду
     * @param barcode Штрих-код продукта
     */
    @GET("api/v1/barcode/")
    suspend fun getProductByBarcode(
        @Query("barcode") barcode: String
    ): Response<BarcodeListResponse>
    
    /**
     * Поиск продуктов по названию
     * @param query Поисковый запрос
     * @param limit Ограничение количества результатов
     */
    @GET("api/v1/search/")
    suspend fun searchProducts(
        @Query("query") query: String,
        @Query("limit") limit: Int = 10
    ): Response<BarcodeListResponse>
} 