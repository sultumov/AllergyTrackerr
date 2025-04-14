package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiService
import com.example.myapplication.data.model.Product
import com.example.myapplication.data.model.ProductResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class ProductRepository {
    private val api = ApiService.openFoodFactsApi
    
    // Получение продукта по штрих-коду
    suspend fun getProductByBarcode(barcode: String): Result<Product?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getProductByBarcode(barcode)
                if (response.isSuccessful && response.body()?.product != null) {
                    Result.success(response.body()?.product)
                } else {
                    Result.failure(Exception("Продукт не найден или ошибка ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Поиск продуктов по названию
    suspend fun searchProducts(query: String, page: Int = 1): Result<List<Product>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.searchProducts(query, page = page)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.products ?: emptyList())
                } else {
                    Result.failure(Exception("Ошибка поиска: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Поиск продуктов по категории
    suspend fun searchProductsByCategory(category: String, page: Int = 1): Result<List<Product>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.searchProductsByCategory(category = category, page = page)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.products ?: emptyList())
                } else {
                    Result.failure(Exception("Ошибка поиска по категории: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Поиск продуктов без указанных аллергенов
    suspend fun searchProductsWithoutAllergen(allergen: String, page: Int = 1): Result<List<Product>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.searchProductsWithoutAllergens(allergen = allergen, page = page)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.products ?: emptyList())
                } else {
                    Result.failure(Exception("Ошибка поиска без аллергенов: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
} 