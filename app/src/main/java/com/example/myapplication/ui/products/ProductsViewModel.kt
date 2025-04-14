package com.example.myapplication.ui.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.data.UserManager

data class Product(
    val name: String,
    val ingredients: List<String>,
    val allergens: List<String>
)

class ProductsViewModel(application: Application) : AndroidViewModel(application) {

    private val userManager = UserManager.getInstance(application.applicationContext)
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

    fun checkProduct(productName: String) {
        val product = products[productName.lowercase()]
        
        if (product != null) {
            // Получаем аллергены пользователя из профиля
            val userAllergens = userManager.getAllergens()
            
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