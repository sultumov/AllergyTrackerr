package com.example.myapplication.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver для обработки уведомлений о приеме лекарств
 */
class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: ""
        val medicationDosage = intent.getStringExtra(EXTRA_MEDICATION_DOSAGE) ?: ""
        val medicationNotes = intent.getStringExtra(EXTRA_MEDICATION_NOTES) ?: ""
        
        // Показываем уведомление
        val notificationManager = MedicationNotificationManager.getInstance(context)
        notificationManager.showMedicationReminder(
            notificationId, 
            medicationName, 
            medicationDosage, 
            medicationNotes
        )
        
        // Здесь также можно добавить логику для повторного планирования уведомления
        // на следующий день, если лекарство принимается регулярно
    }
    
    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_MEDICATION_DOSAGE = "medication_dosage"
        const val EXTRA_MEDICATION_NOTES = "medication_notes"
    }
} 