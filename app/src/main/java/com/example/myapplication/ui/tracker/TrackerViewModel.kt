package com.example.myapplication.ui.tracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Date

data class AllergyReaction(
    val id: Long,
    val date: Date,
    val symptoms: List<String>,
    val possibleTriggers: List<String>,
    val notes: String
)

class TrackerViewModel : ViewModel() {

    private val _reactionRecords = MutableLiveData<List<AllergyReaction>>().apply {
        // Здесь будем загружать данные из локального хранилища
        // Пока используем тестовые данные
        value = getTestData()
    }
    
    val reactionRecords: LiveData<List<AllergyReaction>> = _reactionRecords

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
        currentList.add(reaction)
        _reactionRecords.value = currentList
        // TODO: Save to local storage
    }
} 