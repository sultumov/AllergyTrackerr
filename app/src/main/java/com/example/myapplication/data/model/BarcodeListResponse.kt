package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * Модель ответа от API barcode-list.ru
 */
data class BarcodeListResponse(
    val status: String,
    val message: String?,
    val products: List<BarcodeListProduct>?
)

/**
 * Модель продукта из barcode-list.ru
 */
data class BarcodeListProduct(
    val barcode: String,
    val name: String,
    @SerializedName("full_name")
    val fullName: String?,
    val brand: String?,
    val manufacturer: String?,
    val country: String?,
    val category: String?,
    val subcategory: String?,
    val image: String?,
    val description: String?,
    val ingredients: String?,
    val allergens: List<String>?
) 