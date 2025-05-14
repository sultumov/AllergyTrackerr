package com.example.myapplication.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.USDARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllergenViewModel @Inject constructor(
    private val usdaRepository: USDARepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<String>>(emptyList())
    val searchResults: StateFlow<List<String>> = _searchResults

    private val _allergens = MutableStateFlow<List<String>>(emptyList())
    val allergens: StateFlow<List<String>> = _allergens

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun searchFood(query: String) {
        viewModelScope.launch {
            try {
                val result = usdaRepository.searchFood(query)
                result.onSuccess { response ->
                    _searchResults.value = response.foods.map { it.description }
                }.onFailure { exception ->
                    _error.value = "Ошибка поиска: ${exception.message}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun getFoodAllergens(fdcId: String) {
        viewModelScope.launch {
            try {
                val result = usdaRepository.getFoodDetails(fdcId)
                result.onSuccess { response ->
                    _allergens.value = usdaRepository.getAllergenInfo(response)
                }.onFailure { exception ->
                    _error.value = "Ошибка получения информации об аллергенах: ${exception.message}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }
} 