package com.example.myapplication.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.MainActivity
import com.example.myapplication.data.model.Medication
import com.example.myapplication.data.model.MedicationTime
import java.util.Calendar
import java.util.Date

/**
 * Менеджер для создания и планирования уведомлений о приеме лекарств
 */
class MedicationNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    /**
     * Создает канал для уведомлений (требуется для Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о лекарствах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для напоминаний о приеме лекарств"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Планирует уведомление для приема лекарства в заданное время
     */
    fun scheduleMedicationReminder(medication: Medication, medicationTime: MedicationTime) {
        // Получаем время для уведомления
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, medicationTime.hour)
        calendar.set(Calendar.MINUTE, medicationTime.minute)
        calendar.set(Calendar.SECOND, 0)
        
        // Если время уже прошло сегодня, планируем на завтра
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // Уникальный ID для уведомления и отложенного интента
        val notificationId = (medication.id + medicationTime.hour * 60 + medicationTime.minute).toInt()
        
        // Создаем интент для запуска получателя уведомлений
        val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
            putExtra(MedicationReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_NAME, medication.name)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_DOSAGE, medication.dosage)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_NOTES, medication.notes)
        }
        
        // Создаем отложенный интент для передачи в AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Настраиваем будильник для запуска в нужное время
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
    
    /**
     * Отменяет запланированное уведомление
     */
    fun cancelMedicationReminder(medication: Medication, medicationTime: MedicationTime) {
        val notificationId = (medication.id + medicationTime.hour * 60 + medicationTime.minute).toInt()
        
        val intent = Intent(context, MedicationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Отменяем будильник
        alarmManager.cancel(pendingIntent)
        
        // Отменяем уведомление, если оно уже показано
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Создание и показ уведомления о приеме лекарства
     */
    fun showMedicationReminder(notificationId: Int, medicationName: String, dosage: String, notes: String = "") {
        // Intent для открытия приложения при клике на уведомление
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Создаем текст уведомления
        val title = "Пора принять лекарство"
        val text = "$medicationName, $dosage" + if (notes.isNotEmpty()) "\n$notes" else ""
        
        // Создаем само уведомление
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Показываем уведомление
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Перепланирует все уведомления для всех активных лекарств
     */
    fun rescheduleAllMedicationReminders(medications: List<Medication>) {
        // Сначала отменяем все существующие уведомления
        cancelAllMedicationReminders()
        
        // Затем планируем новые для активных лекарств
        val activeMedications = medications.filter { 
            it.isActive && (it.endDate == null || it.endDate.after(Date())) 
        }
        
        for (medication in activeMedications) {
            for (time in medication.times) {
                if (time.isEnabled) {
                    scheduleMedicationReminder(medication, time)
                }
            }
        }
    }
    
    /**
     * Отменяет все запланированные уведомления
     */
    fun cancelAllMedicationReminders() {
        // Здесь нельзя отменить все сразу, поэтому нужно хранить информацию
        // о запланированных уведомлениях где-то в SharedPreferences или базе данных
        // и отменять их по отдельности
    }
    
    companion object {
        const val CHANNEL_ID = "medication_reminders_channel"
        
        @Volatile
        private var INSTANCE: MedicationNotificationManager? = null
        
        fun getInstance(context: Context): MedicationNotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MedicationNotificationManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
} 