package com.example.myapplication.data.model

data class Allergen(
    val id: String,
    val name: String,
    val category: AllergenCategory,
    val description: String,
    val symptoms: List<String>,
    val avoidanceRecommendations: List<String>,
    val relatedAllergens: List<String> = emptyList(),
    val imageUrl: String? = null,
    val scientificName: String? = null,
    val additionalInfo: String? = null
)

enum class AllergenCategory {
    FOOD,
    POLLEN,
    ANIMAL,
    INSECT,
    DRUG,
    MOLD,
    LATEX,
    DUST,
    CHEMICAL,
    OTHER
}

// Преобразование строки в категорию аллергена
fun String.toAllergenCategory(): AllergenCategory {
    return when (this.lowercase()) {
        "food", "пища", "еда", "продукты" -> AllergenCategory.FOOD
        "pollen", "пыльца", "растения" -> AllergenCategory.POLLEN
        "animal", "животные" -> AllergenCategory.ANIMAL
        "insect", "насекомые" -> AllergenCategory.INSECT
        "drug", "лекарства", "медикаменты" -> AllergenCategory.DRUG
        "mold", "плесень", "грибок" -> AllergenCategory.MOLD
        "latex", "латекс" -> AllergenCategory.LATEX
        "dust", "пыль" -> AllergenCategory.DUST
        "chemical", "химия", "химикаты" -> AllergenCategory.CHEMICAL
        else -> AllergenCategory.OTHER
    }
}

// Получение локализованного названия категории
fun AllergenCategory.getLocalizedName(): String {
    return when (this) {
        AllergenCategory.FOOD -> "Пищевые аллергены"
        AllergenCategory.POLLEN -> "Пыльца растений"
        AllergenCategory.ANIMAL -> "Животные аллергены"
        AllergenCategory.INSECT -> "Аллергены насекомых"
        AllergenCategory.DRUG -> "Лекарственные аллергены"
        AllergenCategory.MOLD -> "Плесень и грибки"
        AllergenCategory.LATEX -> "Латекс"
        AllergenCategory.DUST -> "Пыль и пылевые клещи"
        AllergenCategory.CHEMICAL -> "Химические вещества"
        AllergenCategory.OTHER -> "Другие аллергены"
    }
} 