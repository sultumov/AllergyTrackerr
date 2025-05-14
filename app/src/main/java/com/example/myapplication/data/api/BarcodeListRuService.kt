package com.example.myapplication.data.api

import android.util.Log
import com.example.myapplication.data.model.BarcodeListProduct
import com.example.myapplication.data.model.Product
import com.example.myapplication.data.model.ProductScanResult
import com.example.myapplication.data.model.ScanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Сервис для работы с API barcode-list.ru
 */
open class BarcodeListRuService {
    private val TAG = "BarcodeListRuService"
    
    // Настройка логирования запросов
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // Настройка клиента OkHttp
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Создание Retrofit-клиента
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://barcode-list.ru/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Делаем API открытым для возможности переопределения в тестах
    open val api: BarcodeListRuApi = retrofit.create(BarcodeListRuApi::class.java)
    
    /**
     * Получение информации о продукте по штрих-коду
     * @param barcode Штрих-код продукта
     * @param userAllergens Список аллергенов пользователя
     */
    suspend fun getProductByBarcode(barcode: String, userAllergens: List<String>): ProductScanResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Запрос к barcode-list.ru для штрих-кода: $barcode")
            val response = api.getProductByBarcode(barcode)
            
            if (response.isSuccessful) {
                val productResponse = response.body()
                
                if (productResponse?.status == "success" && !productResponse.products.isNullOrEmpty()) {
                    // Продукт найден
                    val barcodeListProduct = productResponse.products[0]
                    val product = mapToProduct(barcodeListProduct)
                    
                    Log.d(TAG, "Продукт найден: ${product.name}")
                    
                    // Проверяем на наличие аллергенов пользователя
                    val allergenWarnings = findAllergenWarnings(product, userAllergens)
                    
                    return@withContext if (allergenWarnings.isNotEmpty()) {
                        ProductScanResult(
                            status = ScanStatus.CONTAINS_ALLERGENS,
                            product = product,
                            allergenWarnings = allergenWarnings
                        )
                    } else {
                        ProductScanResult(
                            status = ScanStatus.SUCCESS,
                            product = product
                        )
                    }
                } else {
                    // Продукт не найден
                    Log.d(TAG, "Продукт не найден: ${productResponse?.message ?: "Unknown error"}")
                    return@withContext ProductScanResult(
                        status = ScanStatus.NOT_FOUND,
                        message = "Продукт с таким штрих-кодом не найден в российской базе данных"
                    )
                }
            } else {
                // Ошибка запроса
                Log.e(TAG, "Ошибка запроса: ${response.code()}")
                return@withContext ProductScanResult(
                    status = ScanStatus.NETWORK_ERROR,
                    message = "Ошибка при получении данных (${response.code()})"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при запросе: ${e.message}", e)
            // Ошибка сети или другая ошибка
            return@withContext ProductScanResult(
                status = ScanStatus.NETWORK_ERROR,
                message = "Ошибка: ${e.message}"
            )
        }
    }
    
    /**
     * Преобразует ответ API в модель продукта
     */
    private fun mapToProduct(barcodeProduct: BarcodeListProduct): Product {
        // Парсим ингредиенты
        val ingredients = barcodeProduct.ingredients?.split(",")?.map { it.trim() } ?: emptyList()
        
        // Парсим аллергены
        val allergens = barcodeProduct.allergens ?: emptyList()
        
        return Product(
            id = barcodeProduct.barcode,
            barcode = barcodeProduct.barcode,
            name = barcodeProduct.name,
            brand = barcodeProduct.brand,
            description = barcodeProduct.description,
            ingredients = ingredients,
            allergens = allergens,
            imageUrl = barcodeProduct.image,
            nutriScore = null // В данном API нет информации о пищевой ценности
        )
    }
    
    /**
     * Находит предупреждения об аллергенах для пользователя
     */
    private fun findAllergenWarnings(product: Product, userAllergens: List<String>): List<String> {
        if (userAllergens.isEmpty()) {
            return emptyList()
        }
        
        val warnings = mutableListOf<String>()
        
        // Проверяем явные аллергены
        for (allergen in product.allergens) {
            for (userAllergen in userAllergens) {
                if (allergen.contains(userAllergen, ignoreCase = true) || 
                    userAllergen.contains(allergen, ignoreCase = true)) {
                    warnings.add("Содержит аллерген: $allergen")
                    break
                }
            }
        }
        
        // Дополнительно проверяем ингредиенты на наличие аллергенов
        for (ingredient in product.ingredients) {
            for (userAllergen in userAllergens) {
                if (ingredient.contains(userAllergen, ignoreCase = true) && 
                    !warnings.any { it.contains(userAllergen, ignoreCase = true) }) {
                    warnings.add("Может содержать аллерген в составе: $userAllergen (найдено в '$ingredient')")
                    break
                }
            }
        }
        
        return warnings
    }
    
    companion object {
        @Volatile
        private var INSTANCE: BarcodeListRuService? = null
        
        fun getInstance(): BarcodeListRuService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BarcodeListRuService().also {
                    INSTANCE = it
                }
            }
        }
    }
} 