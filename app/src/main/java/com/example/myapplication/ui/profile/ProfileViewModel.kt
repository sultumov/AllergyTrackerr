package com.example.myapplication.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.data.User
import com.example.myapplication.data.UserManager

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    
    private val userManager = UserManager.getInstance(application.applicationContext)
    
    private val _user = MutableLiveData<User>().apply {
        value = userManager.getUser()
    }
    val user: LiveData<User> = _user
    
    private val _allergens = MutableLiveData<List<String>>().apply {
        value = userManager.getAllergens()
    }
    val allergens: LiveData<List<String>> = _allergens
    
    fun saveUser(name: String, age: Int, gender: String, medicalNotes: String) {
        val updatedUser = User(
            name = name,
            age = age,
            gender = gender,
            allergens = _allergens.value?.toMutableList() ?: mutableListOf(),
            medicalNotes = medicalNotes
        )
        userManager.saveUser(updatedUser)
        _user.value = updatedUser
    }
    
    fun addAllergen(allergen: String) {
        // Проверяем, что аллерген не пустой и его еще нет в списке
        val allergenNormalized = allergen.lowercase().trim()
        if (allergenNormalized.isNotBlank() && !(_allergens.value?.map { it.lowercase().trim() }?.contains(allergenNormalized) ?: false)) {
            val updatedAllergens = _allergens.value?.toMutableList() ?: mutableListOf()
            updatedAllergens.add(allergenNormalized)
            userManager.saveAllergens(updatedAllergens)
            _allergens.value = updatedAllergens
            
            // Обновляем аллергены в пользователе
            _user.value?.let {
                val updatedUser = it.copy(allergens = updatedAllergens)
                _user.value = updatedUser
            }
        }
    }
    
    fun removeAllergen(allergen: String) {
        val updatedAllergens = _allergens.value?.toMutableList() ?: mutableListOf()
        updatedAllergens.remove(allergen)
        userManager.saveAllergens(updatedAllergens)
        _allergens.value = updatedAllergens
        
        // Обновляем аллергены в пользователе
        _user.value?.let {
            val updatedUser = it.copy(allergens = updatedAllergens)
            _user.value = updatedUser
        }
    }
} 