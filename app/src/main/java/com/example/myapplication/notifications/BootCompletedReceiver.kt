package com.example.myapplication.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.model.Medication
import com.example.myapplication.ui.notifications.NotificationsViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * BroadcastReceiver для восстановления уведомлений после перезагрузки устройства
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Загружаем сохраненные лекарства
            val sharedPreferences = context.getSharedPreferences("medication_prefs", 0)
            val medicationsJson = sharedPreferences.getString(KEY_MEDICATIONS, null)
            val medications = if (medicationsJson != null) {
                val gson = Gson()
                val type = object : TypeToken<List<Medication>>() {}.type
                gson.fromJson<List<Medication>>(medicationsJson, type)
            } else {
                emptyList()
            }
            
            // Восстанавливаем уведомления
            val notificationManager = MedicationNotificationManager.getInstance(context)
            notificationManager.rescheduleAllMedicationReminders(medications)
        }
    }
    
    companion object {
        private const val KEY_MEDICATIONS = "saved_medications"
    }
} 