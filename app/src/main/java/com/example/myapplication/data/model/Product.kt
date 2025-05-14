package com.example.myapplication.data.model

/**
 * Модель данных для продукта
 */
data class Product(
    val id: String,
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val description: String? = null,
    val ingredients: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    val imageUrl: String? = null,
    val nutriScore: String? = null, // A, B, C, D, E
    val nutritionalInfo: NutritionalInfo? = null
)

/**
 * Модель данных для пищевой ценности продукта
 */
data class NutritionalInfo(
    val calories: Float? = null, // kcal per 100g
    val fat: Float? = null, // g per 100g
    val saturatedFat: Float? = null, // g per 100g
    val carbohydrates: Float? = null, // g per 100g
    val sugars: Float? = null, // g per 100g
    val proteins: Float? = null, // g per 100g
    val salt: Float? = null, // g per 100g
    val fiber: Float? = null // g per 100g
)

/**
 * Результат сканирования продукта
 */
data class ProductScanResult(
    val status: ScanStatus,
    val product: Product? = null,
    val allergenWarnings: List<String> = emptyList(),
    val message: String? = null
)

/**
 * Статус результата сканирования
 */
enum class ScanStatus {
    SUCCESS,        // Продукт найден в базе данных
    NOT_FOUND,      // Штрих-код отсканирован, но продукт не найден в базе
    SCAN_ERROR,     // Ошибка при сканировании штрих-кода
    NETWORK_ERROR,  // Ошибка сети при получении данных о продукте
    CONTAINS_ALLERGENS  // Продукт содержит аллергены пользователя
} 