package com.example.myapplication.ui.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.UserManager
import com.example.myapplication.data.model.Product
import com.example.myapplication.data.model.ProductScanResult
import com.example.myapplication.data.model.ScanStatus
import com.example.myapplication.data.repository.ProductRepository
import kotlinx.coroutines.launch

/**
 * ViewModel для работы с продуктами
 */
class ProductsViewModel(application: Application) : AndroidViewModel(application) {

    private val userManager = UserManager.getInstance(application.applicationContext)
    private val productRepository = ProductRepository.getInstance(application)
    
    private val _scanResult = MutableLiveData<ProductScanResult>()
    val scanResult: LiveData<ProductScanResult> = _scanResult
    
    private val _isScanning = MutableLiveData<Boolean>()
    val isScanning: LiveData<Boolean> = _isScanning
    
    private val _recentProducts = MutableLiveData<List<Product>>()
    val recentProducts: LiveData<List<Product>> = _recentProducts
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _productCheckResult = MutableLiveData<String>()
    val productCheckResult: LiveData<String> = _productCheckResult
    
    private val _searchResults = MutableLiveData<List<Product>>()
    val searchResults: LiveData<List<Product>> = _searchResults
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadRecentProducts()
    }
    
    /**
     * Загружает недавно отсканированные продукты
     */
    fun loadRecentProducts() {
        viewModelScope.launch {
            _recentProducts.value = productRepository.getRecentProducts()
        }
    }
    
    /**
     * Получает информацию о продукте по штрих-коду
     */
    fun getProductByBarcode(barcode: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = productRepository.getProductByBarcode(barcode)
                _scanResult.value = result
            } catch (e: Exception) {
                _scanResult.value = ProductScanResult(
                    status = ScanStatus.NETWORK_ERROR,
                    message = "Ошибка: ${e.message}"
                )
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Обновляет состояние сканирования
     */
    fun setScanning(scanning: Boolean) {
        _isScanning.value = scanning
    }
    
    /**
     * Очищает результаты сканирования
     */
    fun clearScanResult() {
        _scanResult.value = null
    }
    
    // Поиск продуктов по названию
    fun searchProducts(query: String) {
        _loading.value = true
        viewModelScope.launch {
            productRepository.searchProducts(query)
                .onSuccess { products ->
                    if (products.isNotEmpty()) {
                        _searchResults.value = products
                    } else {
                        _error.value = "Продукты по запросу '$query' не найдены."
                    }
                }
                .onFailure { exception ->
                    _error.value = "Ошибка при поиске: ${exception.message}"
                }
            _loading.value = false
        }
    }
    
    // Проверка безопасности продукта для пользователя
    fun checkProduct(product: Product) {
        // Получаем аллергены пользователя из профиля
        val userAllergens = userManager.getAllergens()
        
        // Находим опасные аллергены для пользователя
        val dangerousAllergens = product.allergens.filter { allergen ->
            userAllergens.any { userAllergen -> 
                allergen.contains(userAllergen, ignoreCase = true) 
            }
        }
        
        if (dangerousAllergens.isEmpty()) {
            _productCheckResult.value = """
                Продукт: ${product.name}
                
                Состав: ${product.ingredients.joinToString(", ") ?: "Информация отсутствует"}
                
                Вывод: Этот продукт безопасен для вас.
            """.trimIndent()
        } else {
            _productCheckResult.value = """
                Продукт: ${product.name}
                
                Состав: ${product.ingredients.joinToString(", ") ?: "Информация отсутствует"}
                
                Внимание! Продукт содержит опасные для вас аллергены:
                ${dangerousAllergens.joinToString(", ")}
                
                Не рекомендуется к употреблению!
            """.trimIndent()
        }
    }
    
    // Поиск безопасных продуктов (без аллергенов пользователя)
    fun findSafeProducts() {
        val userAllergens = userManager.getAllergens()
        if (userAllergens.isEmpty()) {
            _error.value = "Не указаны аллергены в профиле. Пожалуйста, добавьте их в настройках."
            return
        }
        
        _loading.value = true
        viewModelScope.launch {
            // Для простоты берем первый аллерген из профиля пользователя
            productRepository.searchProductsWithoutAllergen(userAllergens.first())
                .onSuccess { products ->
                    if (products.isNotEmpty()) {
                        _searchResults.value = products
                    } else {
                        _error.value = "Безопасные продукты не найдены."
                    }
                }
                .onFailure { exception ->
                    _error.value = "Ошибка при поиске безопасных продуктов: ${exception.message}"
                }
            _loading.value = false
        }
    }
} 