package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName
import com.example.myapplication.data.model.Ingredient

data class RecipeSearchResponse(
    val results: List<Recipe>,
    val offset: Int,
    val number: Int,
    val totalResults: Int
)

data class Recipe(
    val id: Int,
    val title: String,
    val image: String?,
    val imageType: String?,
    val servings: Int,
    @SerializedName("readyInMinutes")
    val readyInMinutes: Int,
    val sourceUrl: String?,
    val summary: String?,
    val cuisines: List<String>?,
    val dishTypes: List<String>?,
    val diets: List<String>?,
    val instructions: String?,
    val analyzedInstructions: List<AnalyzedInstruction>?,
    val extendedIngredients: List<String>?
)

data class AnalyzedInstruction(
    val name: String?,
    val steps: List<Step>
)

data class Step(
    val number: Int,
    val step: String,
    val ingredients: List<Ingredient>?,
    val equipment: List<Equipment>?
)

data class Equipment(
    val id: Int,
    val name: String,
    val localizedName: String,
    val image: String
)

data class RecipeInformation(
    val id: Int,
    val title: String,
    val image: String?,
    val servings: Int,
    @SerializedName("readyInMinutes")
    val readyInMinutes: Int,
    val sourceUrl: String?,
    val summary: String?,
    val cuisines: List<String>?,
    val dishTypes: List<String>?,
    val diets: List<String>?,
    val occasions: List<String>?,
    val instructions: String?,
    val analyzedInstructions: List<AnalyzedInstruction>?,
    val extendedIngredients: List<String>?,
    val glutenFree: Boolean,
    val dairyFree: Boolean,
    val vegetarian: Boolean,
    val vegan: Boolean,
    val sustainable: Boolean,
    val cheap: Boolean,
    val veryHealthy: Boolean,
    val veryPopular: Boolean
) 