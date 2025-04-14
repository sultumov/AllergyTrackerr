package com.example.myapplication.ui.allergens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class Allergen(
    val id: Long,
    val name: String,
    val category: String,
    val symptoms: List<String>,
    val recommendations: String
)

class AllergensViewModel : ViewModel() {

    private val allAllergens = getTestAllergens()
    private val _allergens = MutableLiveData<List<Allergen>>().apply {
        value = allAllergens
    }
    
    val allergens: LiveData<List<Allergen>> = _allergens

    fun searchAllergens(query: String) {
        if (query.isEmpty()) {
            _allergens.value = allAllergens
        } else {
            _allergens.value = allAllergens.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.category.contains(query, ignoreCase = true)
            }
        }
    }

    private fun getTestAllergens(): List<Allergen> {
        // Тестовые данные для демонстрации
        return listOf(
            Allergen(
                1,
                "Пыльца деревьев",
                "Природные аллергены",
                listOf("Насморк", "Чихание", "Зуд в глазах", "Слезотечение"),
                "Избегайте прогулок в ветреную погоду, держите окна закрытыми в сезон цветения."
            ),
            Allergen(
                2,
                "Молоко",
                "Пищевые аллергены",
                listOf("Сыпь", "Проблемы с пищеварением", "Отек", "Затрудненное дыхание"),
                "Исключите из рациона молочные продукты, внимательно читайте состав продуктов."
            ),
            Allergen(
                3,
                "Пылевые клещи",
                "Бытовые аллергены",
                listOf("Насморк", "Чихание", "Кашель", "Затрудненное дыхание"),
                "Регулярно проводите влажную уборку, используйте противоаллергенные наволочки."
            ),
            Allergen(
                4,
                "Арахис",
                "Пищевые аллергены",
                listOf("Отек", "Сыпь", "Анафилактический шок", "Зуд"),
                "Полностью исключите арахис и продукты, содержащие его. Всегда имейте при себе антигистаминные препараты."
            ),
            Allergen(
                5,
                "Шерсть животных",
                "Бытовые аллергены",
                listOf("Насморк", "Чихание", "Затрудненное дыхание", "Зуд в глазах"),
                "Ограничьте контакт с животными, регулярно проводите влажную уборку."
            )
        )
    }
} 