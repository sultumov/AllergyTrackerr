package com.example.myapplication.data.model

data class Ingredient(
    val id: String,
    val name: String,
    val text: String? = null,
    val rank: Int? = null,
    val localizedName: String? = null,
    val image: String? = null
) 