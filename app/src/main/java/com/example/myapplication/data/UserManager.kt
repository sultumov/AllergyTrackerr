package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_USER = "user_data"
        private const val KEY_ALLERGENS = "user_allergens"
        
        @Volatile
        private var instance: UserManager? = null
        
        fun getInstance(context: Context): UserManager {
            return instance ?: synchronized(this) {
                instance ?: UserManager(context).also { instance = it }
            }
        }
    }
    
    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        sharedPreferences.edit().putString(KEY_USER, userJson).apply()
    }
    
    fun getUser(): User {
        val userJson = sharedPreferences.getString(KEY_USER, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else {
            User()
        }
    }
    
    fun saveAllergens(allergens: List<String>) {
        val allergensJson = gson.toJson(allergens)
        sharedPreferences.edit().putString(KEY_ALLERGENS, allergensJson).apply()
    }
    
    fun getAllergens(): List<String> {
        val allergensJson = sharedPreferences.getString(KEY_ALLERGENS, null)
        
        // Если аллергены еще не были сохранены, устанавливаем значения по умолчанию
        // и сохраняем их в SharedPreferences
        if (allergensJson == null) {
            val defaultAllergens = listOf("молоко", "арахис")
            saveAllergens(defaultAllergens)
            return defaultAllergens
        }
        
        // Иначе возвращаем сохраненные аллергены
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(allergensJson, type)
    }
    
    // Алиас для getAllergens() для согласованности с репозиторием продуктов
    fun getUserAllergens() = getAllergens()
    
    fun clearUserData() {
        sharedPreferences.edit().clear().apply()
    }
} 