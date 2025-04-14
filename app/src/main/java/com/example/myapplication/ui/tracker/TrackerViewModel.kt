package com.example.myapplication.ui.tracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.data.UserManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

data class AllergyReaction(
    val id: Long,
    val date: Date,
    val symptoms: List<String>,
    val possibleTriggers: List<String>,
    val notes: String
)

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val userManager = UserManager.getInstance(application.applicationContext)
    private val gson = Gson()
    private val sharedPreferences = application.getSharedPreferences("allergy_prefs", 0)
    
    private val _reactionRecords = MutableLiveData<List<AllergyReaction>>()
    val reactionRecords: LiveData<List<AllergyReaction>> = _reactionRecords
    
    init {
        loadReactions()
    }
    
    private fun loadReactions() {
        val reactionsJson = sharedPreferences.getString(KEY_REACTIONS, null)
        val savedReactions = if (reactionsJson != null) {
            val type = object : TypeToken<List<AllergyReaction>>() {}.type
            gson.fromJson<List<AllergyReaction>>(reactionsJson, type)
        } else {
            // Если нет сохраненных данных, используем тестовые
            getTestData()
        }
        
        _reactionRecords.value = savedReactions
    }

    private fun getTestData(): List<AllergyReaction> {
        // Тестовые данные для демонстрации
        return listOf(
            AllergyReaction(
                1,
                Date(),
                listOf("Чихание", "Заложенность носа"),
                listOf("Пыльца", "Пыль"),
                "Симптомы появились после прогулки в парке"
            ),
            AllergyReaction(
                2,
                Date(System.currentTimeMillis() - 86400000), // вчера
                listOf("Сыпь", "Зуд"),
                listOf("Орехи", "Мед"),
                "Проявилось после завтрака"
            )
        )
    }

    fun addReaction(reaction: AllergyReaction) {
        val currentList = _reactionRecords.value?.toMutableList() ?: mutableListOf()
        currentList.add(0, reaction) // Добавляем в начало списка
        _reactionRecords.value = currentList
        saveReactions(currentList)
    }
    
    private fun saveReactions(reactions: List<AllergyReaction>) {
        val reactionsJson = gson.toJson(reactions)
        sharedPreferences.edit().putString(KEY_REACTIONS, reactionsJson).apply()
    }
    
    companion object {
        private const val KEY_REACTIONS = "saved_reactions"
    }
} 