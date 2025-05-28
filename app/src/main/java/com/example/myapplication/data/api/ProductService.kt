package com.example.myapplication.data.api

import android.util.Log
import com.example.myapplication.data.model.ApiProduct
import com.example.myapplication.data.model.Product
import com.example.myapplication.data.model.ProductResponse
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
 * Сервис для получения информации о продуктах из различных источников
 */
open class ProductService {
    private val TAG = "ProductService"
    
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
    
    // OpenFoodFacts API
    private val openFoodFactsRetrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Делаем API открытым для возможности переопределения в тестах
    open val openFoodFactsApi: OpenFoodFactsApi = openFoodFactsRetrofit.create(OpenFoodFactsApi::class.java)
    
    // Российский сервис штрих-кодов
    open val barcodeListRuService = BarcodeListRuService.getInstance()
    
    /**
     * Получает информацию о продукте по штрих-коду из различных источников
     * @param barcode Штрих-код продукта
     * @param userAllergens Список аллергенов пользователя
     */
    suspend fun getProductByBarcode(barcode: String, userAllergens: List<String>): ProductScanResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Поиск продукта по штрих-коду: $barcode")
        
        // Проверяем, российский ли это товар (штрих-код начинается с 460-469)
        val isRussianBarcode = barcode.startsWith("46")
        
        if (isRussianBarcode) {
            Log.d(TAG, "Обнаружен российский штрих-код, сначала ищем в barcode-list.ru")
            // Сначала ищем в российской базе
            val russianResult = barcodeListRuService.getProductByBarcode(barcode, userAllergens)
            
            // Если нашли в российской базе, возвращаем
            if (russianResult.status == ScanStatus.SUCCESS || russianResult.status == ScanStatus.CONTAINS_ALLERGENS) {
                Log.d(TAG, "Продукт найден в российской базе: ${russianResult.product?.name}")
                return@withContext russianResult
            }
            
            // Если не нашли, пробуем искать в OpenFoodFacts
            Log.d(TAG, "Продукт не найден в российской базе, ищем в OpenFoodFacts")
        }
        
        // Поиск в OpenFoodFacts
        return@withContext searchInOpenFoodFacts(barcode, userAllergens)
    }
    
    /**
     * Поиск продукта в базе OpenFoodFacts
     */
    internal suspend fun searchInOpenFoodFacts(barcode: String, userAllergens: List<String>): ProductScanResult {
        try {
            Log.d(TAG, "Запрос к OpenFoodFacts для штрих-кода: $barcode")
            val response = openFoodFactsApi.getProductByBarcode(barcode)
            
            if (response.isSuccessful) {
                val productResponse = response.body()
                if (productResponse?.status == 1 && productResponse.product != null) {
                    // Продукт найден
                    val product = mapApiResponseToProduct(productResponse)
                    Log.d(TAG, "Продукт найден в OpenFoodFacts: ${product.name}")
                    
                    // Проверяем на наличие аллергенов пользователя
                    val allergenWarnings = findAllergenWarnings(product, userAllergens)
                    
                    return if (allergenWarnings.isNotEmpty()) {
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
                    Log.d(TAG, "Продукт не найден в OpenFoodFacts")
                    return ProductScanResult(
                        status = ScanStatus.NOT_FOUND,
                        message = "Продукт с таким штрих-кодом не найден ни в одной базе данных"
                    )
                }
            } else {
                // Ошибка запроса
                Log.e(TAG, "Ошибка запроса к OpenFoodFacts: ${response.code()}")
                return ProductScanResult(
                    status = ScanStatus.NETWORK_ERROR,
                    message = "Ошибка при получении данных (${response.code()})"
                )
            }
        } catch (e: Exception) {
            // Ошибка сети или другая ошибка
            Log.e(TAG, "Исключение при запросе к OpenFoodFacts: ${e.message}", e)
            return ProductScanResult(
                status = ScanStatus.NETWORK_ERROR,
                message = "Ошибка: ${e.message}"
            )
        }
    }
    
    /**
     * Преобразует ответ API в модель продукта
     */
    private fun mapApiResponseToProduct(response: ProductResponse): Product {
        val apiProduct = response.product ?: throw IllegalArgumentException("ApiProduct cannot be null")
        
        // Парсим ингредиенты
        val ingredients = apiProduct.ingredients?.map { it.text } ?: emptyList()
        
        // Парсим аллергены
        val allergens = apiProduct.allergensTags?.map { 
            it.removePrefix("en:").replace('-', ' ').trim()
        } ?: emptyList()
        
        return Product(
            id = response.code,
            barcode = response.code,
            name = apiProduct.productName ?: "Неизвестный продукт",
            brand = apiProduct.brands,
            imageUrl = apiProduct.imageUrl,
            ingredients = ingredients as List<String>,
            allergens = allergens,
            nutriScore = apiProduct.nutriScore,
            nutritionalInfo = null // Для простоты пропускаем пищевую ценность
        )
    }
    
    /**
     * Находит предупреждения об аллергенах для пользователя
     */
    private fun findAllergenWarnings(product: Product, userAllergens: List<String>): List<String> {
        if (userAllergens.isEmpty() || product.allergens.isEmpty()) {
            return emptyList()
        }
        
        val warnings = mutableListOf<String>()
        
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
        private var INSTANCE: ProductService? = null
        
        fun getInstance(): ProductService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProductService().also {
                    INSTANCE = it
                }
            }
        }
    }
} 