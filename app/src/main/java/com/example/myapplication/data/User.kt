package com.example.myapplication.data

data class User(
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val allergens: MutableList<String> = mutableListOf(),
    val medicalNotes: String = ""
) 