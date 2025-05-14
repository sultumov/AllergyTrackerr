package com.example.myapplication.data.model

import java.util.Date

/**
 * Модель данных для лекарств и напоминаний о приеме
 */
data class Medication(
    val id: Long,
    val name: String,
    val dosage: String,
    val frequency: MedicationFrequency,
    val startDate: Date,
    val endDate: Date? = null,
    val times: List<MedicationTime>,
    val notes: String = "",
    val isActive: Boolean = true
)

enum class MedicationFrequency {
    ONCE_DAILY,
    TWICE_DAILY,
    THREE_TIMES_DAILY,
    FOUR_TIMES_DAILY,
    EVERY_OTHER_DAY,
    WEEKLY,
    AS_NEEDED,
    CUSTOM
}

data class MedicationTime(
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true
)

// Расширение для получения локализованного названия частоты приема
fun MedicationFrequency.getLocalizedName(): String {
    return when (this) {
        MedicationFrequency.ONCE_DAILY -> "Один раз в день"
        MedicationFrequency.TWICE_DAILY -> "Два раза в день"
        MedicationFrequency.THREE_TIMES_DAILY -> "Три раза в день"
        MedicationFrequency.FOUR_TIMES_DAILY -> "Четыре раза в день"
        MedicationFrequency.EVERY_OTHER_DAY -> "Через день"
        MedicationFrequency.WEEKLY -> "Еженедельно"
        MedicationFrequency.AS_NEEDED -> "По необходимости"
        MedicationFrequency.CUSTOM -> "Пользовательский режим"
    }
} 