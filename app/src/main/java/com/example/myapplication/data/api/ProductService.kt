package com.example.myapplication.data.api

import com.example.myapplication.data.model.ApiProduct
import com.example.myapplication.data.model.Product
import com.example.myapplication.data.model.ProductResponse
import com.example.myapplication.data.model.ProductScanResult
import com.example.myapplication.data.model.ScanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Сервис для получения информации о продуктах
 */
class ProductService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val api = retrofit.create(OpenFoodFactsApi::class.java)
    
    /**
     * Получает информацию о продукте по штрих-коду
     */
    suspend fun getProductByBarcode(barcode: String, userAllergens: List<String>): ProductScanResult = withContext(Dispatchers.IO) {
        try {
            val response = api.getProductByBarcode(barcode)
            
            if (response.isSuccessful) {
                val productResponse = response.body()
                if (productResponse?.status == 1 && productResponse.product != null) {
                    // Продукт найден
                    val product = mapApiResponseToProduct(productResponse)
                    
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
                    return@withContext ProductScanResult(
                        status = ScanStatus.NOT_FOUND,
                        message = "Продукт с таким штрих-кодом не найден"
                    )
                }
            } else {
                // Ошибка запроса
                return@withContext ProductScanResult(
                    status = ScanStatus.NETWORK_ERROR,
                    message = "Ошибка при получении данных (${response.code()})"
                )
            }
        } catch (e: Exception) {
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
    private fun mapApiResponseToProduct(response: ProductResponse): Product {
        val apiProduct = response.product
        
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
            ingredients = ingredients,
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