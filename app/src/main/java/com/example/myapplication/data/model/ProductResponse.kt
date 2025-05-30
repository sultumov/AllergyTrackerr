package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName
import com.example.myapplication.data.model.Ingredient

data class ProductResponse(
    val status: Int,
    val code: String,
    val product: ApiProduct?,
    @SerializedName("status_verbose")
    val statusVerbose: String
)

data class ApiProduct(
    val id: String,
    @SerializedName("product_name")
    val productName: String,
    val brands: String?,
    val categories: String?,
    val ingredients: List<Ingredient>?,
    @SerializedName("allergens")
    val allergensText: String?,
    @SerializedName("allergens_tags")
    val allergensTags: List<String>?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("nutriments")
    val nutrients: Nutrients?,
    @SerializedName("nutrition_grades")
    val nutriScore: String?
)

data class Nutrients(
    val energy: Double?,
    @SerializedName("energy_unit")
    val energyUnit: String?,
    val proteins: Double?,
    val fat: Double?,
    @SerializedName("carbohydrates")
    val carbs: Double?,
    val sugars: Double?,
    val fiber: Double?,
    val salt: Double?
)

data class ProductSearchResponse(
    val count: Int,
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int,
    val products: List<ApiProduct>,
    val skip: Int
) 