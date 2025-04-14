package com.example.myapplication.ui.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.UserManager
import com.example.myapplication.data.model.Product
import com.example.myapplication.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductsViewModel(application: Application) : AndroidViewModel(application) {

    private val userManager = UserManager.getInstance(application.applicationContext)
    private val productRepository = ProductRepository()
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _productCheckResult = MutableLiveData<String>()
    val productCheckResult: LiveData<String> = _productCheckResult
    
    private val _searchResults = MutableLiveData<List<Product>>()
    val searchResults: LiveData<List<Product>> = _searchResults
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Получение продукта по штрих-коду
    fun getProductByBarcode(barcode: String) {
        _loading.value = true
        viewModelScope.launch {
            productRepository.getProductByBarcode(barcode)
                .onSuccess { product ->
                    if (product != null) {
                        checkProduct(product)
                    } else {
                        _productCheckResult.value = "Продукт с штрих-кодом '${barcode}' не найден в базе данных."
                    }
                }
                .onFailure { exception ->
                    _error.value = "Ошибка при получении данных: ${exception.message}"
                    _productCheckResult.value = "Не удалось получить информацию о продукте. Пожалуйста, попробуйте снова."
                }
            _loading.value = false
        }
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
        
        // Получаем список аллергенов продукта
        val productAllergens = product.allergensTags?.map { 
            // Обычно аллергены имеют формат "en:gluten", "en:milk" и т.д.
            it.substringAfter(":") 
        } ?: emptyList()
        
        // Находим опасные аллергены для пользователя
        val dangerousAllergens = productAllergens.filter { allergen ->
            userAllergens.any { userAllergen -> 
                allergen.contains(userAllergen, ignoreCase = true) 
            }
        }
        
        if (dangerousAllergens.isEmpty()) {
            _productCheckResult.value = """
                Продукт: ${product.productName}
                
                Состав: ${product.ingredients?.joinToString(", ") { it.text } ?: "Информация отсутствует"}
                
                Вывод: Этот продукт безопасен для вас.
            """.trimIndent()
        } else {
            _productCheckResult.value = """
                Продукт: ${product.productName}
                
                Состав: ${product.ingredients?.joinToString(", ") { it.text } ?: "Информация отсутствует"}
                
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