package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.data.UserManager
import com.example.myapplication.data.api.ProductService
import com.example.myapplication.data.model.Product
import com.example.myapplication.data.model.ProductScanResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с продуктами
 */
class ProductRepository(private val context: Context) {
    private val productService = ProductService.getInstance()
    private val userManager = UserManager.getInstance(context)
    private val gson = Gson()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Получает продукт по штрих-коду
     */
    suspend fun getProductByBarcode(barcode: String): ProductScanResult {
        // Проверяем, есть ли продукт в локальном хранилище
        val localProduct = getLocalProduct(barcode)
        
        if (localProduct != null) {
            // Если продукт найден локально, проверяем на аллергены
            val userAllergens = userManager.getUserAllergens()
            val allergenWarnings = findAllergenWarnings(localProduct, userAllergens)
            
            return if (allergenWarnings.isNotEmpty()) {
                ProductScanResult(
                    status = com.example.myapplication.data.model.ScanStatus.CONTAINS_ALLERGENS,
                    product = localProduct,
                    allergenWarnings = allergenWarnings
                )
                } else {
                ProductScanResult(
                    status = com.example.myapplication.data.model.ScanStatus.SUCCESS,
                    product = localProduct
                )
            }
        }
        
        // Если продукт не найден локально, ищем через API
        val userAllergens = userManager.getUserAllergens()
        val result = productService.getProductByBarcode(barcode, userAllergens)
        
        // Если продукт успешно получен, сохраняем его локально
        if (result.status == com.example.myapplication.data.model.ScanStatus.SUCCESS || 
            result.status == com.example.myapplication.data.model.ScanStatus.CONTAINS_ALLERGENS) {
            result.product?.let { saveProductLocally(it) }
        }
        
        return result
    }
    
    /**
     * Получает недавно отсканированные продукты
     */
    fun getRecentProducts(): List<Product> {
        val json = sharedPreferences.getString(KEY_RECENT_PRODUCTS, null) ?: return emptyList()
        val type = object : TypeToken<List<Product>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * Получает продукт из локального хранилища
     */
    private fun getLocalProduct(barcode: String): Product? {
        val recentProducts = getRecentProducts()
        return recentProducts.find { it.barcode == barcode }
    }
    
    /**
     * Сохраняет продукт в локальное хранилище
     */
    private fun saveProductLocally(product: Product) {
        var recentProducts = getRecentProducts().toMutableList()
        
        // Удаляем продукт, если он уже был в списке
        recentProducts.removeIf { it.barcode == product.barcode }
        
        // Добавляем продукт в начало списка
        recentProducts.add(0, product)
        
        // Ограничиваем список до 20 продуктов
        if (recentProducts.size > MAX_RECENT_PRODUCTS) {
            recentProducts = recentProducts.subList(0, MAX_RECENT_PRODUCTS)
        }
        
        // Сохраняем обновленный список
        val json = gson.toJson(recentProducts)
        sharedPreferences.edit().putString(KEY_RECENT_PRODUCTS, json).apply()
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
    
    /**
     * Поиск продуктов по названию
     */
    suspend fun searchProducts(query: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            // Поиск в локальном хранилище
            val localProducts = getRecentProducts().filter {
                it.name.contains(query, ignoreCase = true) ||
                it.brand?.contains(query, ignoreCase = true) == true
            }
            
            // Если нашли продукты локально, возвращаем их
            if (localProducts.isNotEmpty()) {
                return@withContext Result.success(localProducts)
            }
            
            // Временное решение - пока нет API для поиска по названию,
            // просто возвращаем пустой список
            // В реальном приложении здесь был бы запрос к API
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Поиск продуктов без указанного аллергена
     */
    suspend fun searchProductsWithoutAllergen(allergen: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            // Поиск в локальном хранилище
            val safeProducts = getRecentProducts().filter { product ->
                !product.allergens.any { it.contains(allergen, ignoreCase = true) } &&
                !product.ingredients.any { it.contains(allergen, ignoreCase = true) }
            }
            
            Result.success(safeProducts)
            } catch (e: Exception) {
                Result.failure(e)
        }
    }
    
    companion object {
        private const val PREFS_NAME = "products_prefs"
        private const val KEY_RECENT_PRODUCTS = "recent_products"
        private const val MAX_RECENT_PRODUCTS = 20
        
        @Volatile
        private var INSTANCE: ProductRepository? = null
        
        fun getInstance(context: Context): ProductRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProductRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
} 