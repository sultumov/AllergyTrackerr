package com.example.myapplication.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class Product(
    val name: String,
    val ingredients: List<String>,
    val allergens: List<String>
)

class ProductsViewModel : ViewModel() {

    private val _productCheckResult = MutableLiveData<String>()
    val productCheckResult: LiveData<String> = _productCheckResult

    // Для демонстрации храним продукты в памяти
    private val products = mapOf(
        "молоко" to Product(
            "Молоко",
            listOf("молоко"),
            listOf("молоко")
        ),
        "хлеб" to Product(
            "Хлеб",
            listOf("мука пшеничная", "вода", "соль", "дрожжи", "сахар"),
            listOf("глютен")
        ),
        "шоколад" to Product(
            "Шоколад",
            listOf("какао-масло", "сахар", "молоко", "соевый лецитин", "ваниль"),
            listOf("молоко", "соя")
        ),
        "яблоко" to Product(
            "Яблоко",
            listOf("яблоко"),
            listOf()
        ),
        "арахисовое масло" to Product(
            "Арахисовое масло",
            listOf("арахис", "соль", "растительное масло"),
            listOf("арахис")
        )
    )

    // Для демонстрации используем фиксированный список аллергенов пользователя
    private val userAllergens = listOf("молоко", "арахис")

    fun checkProduct(productName: String) {
        val product = products[productName.lowercase()]
        
        if (product != null) {
            val dangerousAllergens = product.allergens.filter { it in userAllergens }
            
            if (dangerousAllergens.isEmpty()) {
                _productCheckResult.value = """
                    Продукт: ${product.name}
                    
                    Состав: ${product.ingredients.joinToString(", ")}
                    
                    Вывод: Этот продукт безопасен для вас.
                """.trimIndent()
            } else {
                _productCheckResult.value = """
                    Продукт: ${product.name}
                    
                    Состав: ${product.ingredients.joinToString(", ")}
                    
                    Внимание! Продукт содержит опасные для вас аллергены:
                    ${dangerousAllergens.joinToString(", ")}
                    
                    Не рекомендуется к употреблению!
                """.trimIndent()
            }
        } else {
            _productCheckResult.value = "Продукт '${productName}' не найден в базе данных."
        }
    }
} 