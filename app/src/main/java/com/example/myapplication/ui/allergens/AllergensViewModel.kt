package com.example.myapplication.ui.allergens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Allergen
import com.example.myapplication.data.model.AllergenCategory
import com.example.myapplication.data.repository.AllergenRepository
import com.example.myapplication.data.repository.ArticleInfo
import com.example.myapplication.data.repository.WikipediaInfo
import kotlinx.coroutines.launch

class AllergensViewModel : ViewModel() {

    private val allergenRepository = AllergenRepository()
    
    private val _allergens = MutableLiveData<List<Allergen>>()
    val allergens: LiveData<List<Allergen>> = _allergens
    
    private val _categories = MutableLiveData<List<AllergenCategory>>()
    val categories: LiveData<List<AllergenCategory>> = _categories
    
    private val _selectedAllergen = MutableLiveData<Allergen>()
    val selectedAllergen: LiveData<Allergen> = _selectedAllergen
    
    private val _scientificInfo = MutableLiveData<List<ArticleInfo>>()
    val scientificInfo: LiveData<List<ArticleInfo>> = _scientificInfo
    
    private val _wikipediaInfo = MutableLiveData<WikipediaInfo?>()
    val wikipediaInfo: LiveData<WikipediaInfo?> = _wikipediaInfo
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private var currentCategory: AllergenCategory? = null
    private var currentQuery: String = ""
    
    init {
        loadAllAllergens()
        loadCategories()
    }
    
    /**
     * Загрузка всех аллергенов
     */
    fun loadAllAllergens() {
        val allAllergens = allergenRepository.getAllAllergens()
        _allergens.value = allAllergens
        currentCategory = null
        currentQuery = ""
    }
    
    /**
     * Загрузка категорий аллергенов
     */
    fun loadCategories() {
        val allCategories = allergenRepository.getAllCategories()
        _categories.value = allCategories
    }
    
    /**
     * Фильтрация аллергенов по категории
     */
    fun filterByCategory(category: AllergenCategory) {
        val filteredAllergens = allergenRepository.getAllergensForCategory(category)
        _allergens.value = filteredAllergens
        currentCategory = category
    }
    
    /**
     * Поиск аллергенов по запросу
     */
    fun searchAllergens(query: String) {
        currentQuery = query
        
        if (query.isEmpty() && currentCategory != null) {
            // Если запрос пустой, но выбрана категория - показываем аллергены этой категории
            filterByCategory(currentCategory!!)
        } else if (query.isEmpty()) {
            // Если запрос пустой и категория не выбрана - показываем все аллергены
            loadAllAllergens()
        } else {
            // Поиск аллергенов по запросу
            val searchResults = allergenRepository.searchAllergensByName(query)
            _allergens.value = searchResults
        }
    }
    
    /**
     * Выбор аллергена для просмотра дополнительной информации
     */
    fun selectAllergen(allergen: Allergen) {
        _selectedAllergen.value = allergen
        loadAdditionalInfo(allergen)
    }
    
    /**
     * Загрузка дополнительной информации из API
     */
    private fun loadAdditionalInfo(allergen: Allergen) {
        _isLoading.value = true
        
        viewModelScope.launch {
            // Загрузка научной информации из NCBI
            allergenRepository.getScientificInfo(allergen)
                .onSuccess { articles ->
                    _scientificInfo.value = articles
                }
                .onFailure { exception ->
                    _error.value = "Ошибка загрузки научной информации: ${exception.message}"
                }
            
            // Загрузка информации из Википедии
            allergenRepository.getWikipediaInfo(allergen)
                .onSuccess { info ->
                    _wikipediaInfo.value = info
                }
                .onFailure { exception ->
                    _error.value = "Ошибка загрузки информации из Википедии: ${exception.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Очистка информации о выбранном аллергене
     */
    fun clearSelectedAllergen() {
        _selectedAllergen.value = null
        _scientificInfo.value = emptyList()
        _wikipediaInfo.value = null
    }
} 