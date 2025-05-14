package com.example.myapplication.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.data.model.Medication
import com.example.myapplication.data.model.MedicationFrequency
import com.example.myapplication.data.model.MedicationTime
import com.example.myapplication.notifications.MedicationNotificationManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Date

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("medication_prefs", 0)
    private val gson = Gson()
    private val notificationManager = MedicationNotificationManager.getInstance(application)

    private val _medications = MutableLiveData<List<Medication>>()
    val medications: LiveData<List<Medication>> = _medications
    
    private val _upcomingMedications = MutableLiveData<List<MedicationWithTime>>()
    val upcomingMedications: LiveData<List<MedicationWithTime>> = _upcomingMedications
    
    // Текущее сообщение для пустого состояния
    private val _emptyStateMessage = MutableLiveData<String>().apply {
        value = "У вас пока нет добавленных лекарств"
    }
    val emptyStateMessage: LiveData<String> = _emptyStateMessage

    init {
        loadMedications()
        updateUpcomingMedications()
    }
    
    /**
     * Загружает список лекарств из SharedPreferences
     */
    private fun loadMedications() {
        val medicationsJson = sharedPreferences.getString(KEY_MEDICATIONS, null)
        val savedMedications = if (medicationsJson != null) {
            val type = object : TypeToken<List<Medication>>() {}.type
            gson.fromJson<List<Medication>>(medicationsJson, type)
        } else {
            // Если нет сохраненных данных, используем тестовые для демо
            getTestMedications()
        }
        
        _medications.value = savedMedications
    }
    
    /**
     * Сохраняет список лекарств в SharedPreferences
     */
    private fun saveMedications(medications: List<Medication>) {
        val medicationsJson = gson.toJson(medications)
        sharedPreferences.edit().putString(KEY_MEDICATIONS, medicationsJson).apply()
    }
    
    /**
     * Добавляет новое лекарство в список
     */
    fun addMedication(medication: Medication) {
        val currentList = _medications.value?.toMutableList() ?: mutableListOf()
        currentList.add(medication)
        _medications.value = currentList
        saveMedications(currentList)
        updateUpcomingMedications()
        
        // Планируем уведомления для нового лекарства
        if (medication.isActive) {
            for (time in medication.times) {
                if (time.isEnabled) {
                    notificationManager.scheduleMedicationReminder(medication, time)
                }
            }
        }
    }
    
    /**
     * Обновляет лекарство в списке
     */
    fun updateMedication(medication: Medication) {
        val currentList = _medications.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.id == medication.id }
        if (index != -1) {
            // Получаем старую версию лекарства для отмены уведомлений
            val oldMedication = currentList[index]
            
            // Отменяем старые уведомления
            for (time in oldMedication.times) {
                notificationManager.cancelMedicationReminder(oldMedication, time)
            }
            
            // Обновляем лекарство в списке
            currentList[index] = medication
            _medications.value = currentList
            saveMedications(currentList)
            updateUpcomingMedications()
            
            // Планируем новые уведомления, если лекарство активно
            if (medication.isActive) {
                for (time in medication.times) {
                    if (time.isEnabled) {
                        notificationManager.scheduleMedicationReminder(medication, time)
                    }
                }
            }
        }
    }
    
    /**
     * Удаляет лекарство из списка
     */
    fun deleteMedication(medicationId: Long) {
        val currentList = _medications.value?.toMutableList() ?: mutableListOf()
        
        // Находим лекарство для отмены уведомлений перед удалением
        val medicationToDelete = currentList.find { it.id == medicationId }
        if (medicationToDelete != null) {
            // Отменяем уведомления
            for (time in medicationToDelete.times) {
                notificationManager.cancelMedicationReminder(medicationToDelete, time)
            }
        }
        
        // Удаляем лекарство из списка
        val newList = currentList.filter { it.id != medicationId }
        _medications.value = newList
        saveMedications(newList)
        updateUpcomingMedications()
    }
    
    /**
     * Обновляет список ближайших напоминаний о приеме лекарств
     */
    fun updateUpcomingMedications() {
        val activeMedications = _medications.value?.filter { 
            it.isActive && (it.endDate == null || it.endDate.after(Date())) 
        } ?: emptyList()
        
        if (activeMedications.isEmpty()) {
            _upcomingMedications.value = emptyList()
            return
        }
        
        val now = Calendar.getInstance()
        val upcomingList = mutableListOf<MedicationWithTime>()
        
        for (medication in activeMedications) {
            for (time in medication.times) {
                if (!time.isEnabled) continue
                
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, time.hour)
                calendar.set(Calendar.MINUTE, time.minute)
                
                // Если время уже прошло сегодня, берем завтрашнее время
                if (calendar.before(now)) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                
                // Проверяем, нужно ли принимать лекарство в этот день
                // в зависимости от частоты приема
                val daysToAdd = when (medication.frequency) {
                    MedicationFrequency.EVERY_OTHER_DAY -> {
                        val daysSinceStart = daysBetween(medication.startDate, calendar.time)
                        if (daysSinceStart % 2 == 0) 0 else 1
                    }
                    MedicationFrequency.WEEKLY -> {
                        val daysSinceStart = daysBetween(medication.startDate, calendar.time)
                        val daysToNextDose = 7 - (daysSinceStart % 7)
                        if (daysToNextDose == 7) 0 else daysToNextDose
                    }
                    else -> 0
                }
                
                if (daysToAdd > 0) {
                    calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
                }
                
                upcomingList.add(MedicationWithTime(medication, calendar.time))
            }
        }
        
        // Сортируем по времени (ближайшие первыми)
        upcomingList.sortBy { it.time }
        
        // Берем только ближайшие 5 напоминаний
        _upcomingMedications.value = upcomingList.take(5)
    }
    
    /**
     * Возвращает количество дней между двумя датами
     */
    private fun daysBetween(startDate: Date, endDate: Date): Int {
        val millisecondsPerDay = 24 * 60 * 60 * 1000L
        return ((endDate.time - startDate.time) / millisecondsPerDay).toInt()
    }
    
    /**
     * Перепланирует все уведомления
     */
    fun rescheduleAllNotifications() {
        val medications = _medications.value ?: emptyList()
        notificationManager.rescheduleAllMedicationReminders(medications)
    }
    
    /**
     * Создает тестовые данные для демонстрации
     */
    private fun getTestMedications(): List<Medication> {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        // Первое лекарство - утром и вечером
        calendar.add(Calendar.DAY_OF_MONTH, 7) // курс на неделю
        val endDate = calendar.time
        
        val morningTime = MedicationTime(8, 0)
        val eveningTime = MedicationTime(20, 0)
        
        return listOf(
            Medication(
                id = 1,
                name = "Цетиризин",
                dosage = "10 мг",
                frequency = MedicationFrequency.TWICE_DAILY,
                startDate = today,
                endDate = endDate,
                times = listOf(morningTime, eveningTime),
                notes = "Принимать после еды"
            ),
            Medication(
                id = 2,
                name = "Преднизолон",
                dosage = "5 мг",
                frequency = MedicationFrequency.ONCE_DAILY,
                startDate = today,
                times = listOf(MedicationTime(9, 0)),
                notes = "Строго по назначению врача"
            )
        )
    }
    
    companion object {
        private const val KEY_MEDICATIONS = "saved_medications"
    }
}

/**
 * Класс для хранения лекарства и времени его приема
 */
data class MedicationWithTime(
    val medication: Medication,
    val time: Date
)