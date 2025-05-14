package com.example.myapplication.ui.notifications

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.model.Medication
import com.example.myapplication.data.model.MedicationFrequency
import com.example.myapplication.data.model.MedicationTime
import com.example.myapplication.data.model.getLocalizedName
import com.example.myapplication.databinding.DialogAddEditMedicationBinding
import com.example.myapplication.databinding.FragmentNotificationsBinding
import com.example.myapplication.databinding.ItemTimeSlotBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NotificationsViewModel
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    // Запрос разрешения на показ уведомлений (только для Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Разрешение получено, планируем уведомления
            viewModel.rescheduleAllNotifications()
            Snackbar.make(binding.root, "Напоминания активированы", Snackbar.LENGTH_SHORT).show()
        } else {
            // Разрешение не получено, показываем сообщение
            Snackbar.make(binding.root, "Для работы напоминаний необходимо разрешение", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Проверяем и запрашиваем разрешения
        checkNotificationPermission()
        
        // Настройка RecyclerView для ближайших напоминаний
        val upcomingRecyclerView = binding.recyclerUpcoming
        upcomingRecyclerView.layoutManager = LinearLayoutManager(context)
        
        // Настройка RecyclerView для списка лекарств
        val medicationsRecyclerView = binding.recyclerMedications
        medicationsRecyclerView.layoutManager = LinearLayoutManager(context)
        
        // Настройка кнопки добавления лекарства
        binding.fabAddMedication.setOnClickListener {
            showAddEditMedicationDialog()
        }
        
        // Наблюдение за списком лекарств
        viewModel.medications.observe(viewLifecycleOwner) { medications ->
            medicationsRecyclerView.adapter = MedicationsAdapter(medications) { medication ->
                showAddEditMedicationDialog(medication)
            }
            
            binding.textEmptyMedications.visibility = 
                if (medications.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Наблюдение за списком ближайших напоминаний
        viewModel.upcomingMedications.observe(viewLifecycleOwner) { upcomingMedications ->
            upcomingRecyclerView.adapter = UpcomingAdapter(upcomingMedications)
            
            binding.textEmptyUpcoming.visibility = 
                if (upcomingMedications.isEmpty()) View.VISIBLE else View.GONE
        }
        
        return root
    }
    
    /**
     * Проверяет наличие разрешения на показ уведомлений
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Разрешение уже есть
                    viewModel.rescheduleAllNotifications()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Показываем объяснение, зачем нужно разрешение
                    AlertDialog.Builder(requireContext())
                        .setTitle("Требуется разрешение")
                        .setMessage("Для работы напоминаний о приеме лекарств необходимо разрешение на показ уведомлений.")
                        .setPositiveButton("Разрешить") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
                else -> {
                    // Запрашиваем разрешение
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Для более старых версий Android разрешение не требуется
            viewModel.rescheduleAllNotifications()
        }
    }

    /**
     * Показывает диалог добавления/редактирования лекарства
     */
    private fun showAddEditMedicationDialog(medication: Medication? = null) {
        val isEditing = medication != null
        val dialogBinding = DialogAddEditMedicationBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
            
        // Настройка заголовка диалога
        dialogBinding.dialogTitle.text = if (isEditing) "Изменить лекарство" else "Добавить лекарство"
        
        // Заполнение полей данными редактируемого лекарства
        if (isEditing) {
            dialogBinding.editMedicationName.setText(medication!!.name)
            dialogBinding.editMedicationDosage.setText(medication.dosage)
            dialogBinding.dropdownFrequency.setText(medication.frequency.getLocalizedName())
            dialogBinding.editStartDate.setText(dateFormat.format(medication.startDate))
            medication.endDate?.let { dialogBinding.editEndDate.setText(dateFormat.format(it)) }
            dialogBinding.editMedicationNotes.setText(medication.notes)
        } else {
            // Установка текущей даты для нового лекарства
            dialogBinding.editStartDate.setText(dateFormat.format(Date()))
        }
        
        // Настройка выпадающего списка частоты приема
        val frequencyOptions = MedicationFrequency.values().map { it.getLocalizedName() }
        val frequencyAdapter = ArrayAdapter(requireContext(), 
            android.R.layout.simple_dropdown_item_1line, frequencyOptions)
        dialogBinding.dropdownFrequency.setAdapter(frequencyAdapter)
        
        // Настройка выбора даты начала
        dialogBinding.editStartDate.setOnClickListener {
            showDatePicker(dialogBinding.editStartDate)
        }
        
        // Настройка выбора даты окончания
        dialogBinding.editEndDate.setOnClickListener {
            showDatePicker(dialogBinding.editEndDate, isEndDate = true)
        }
        
        // Добавление времен приема из существующего лекарства или создание пустого
        if (isEditing) {
            for (time in medication!!.times) {
                addTimeSlot(dialogBinding.containerTimeSlots, time)
            }
        } else {
            // Для нового лекарства добавляем одно время по умолчанию (9:00)
            addTimeSlot(dialogBinding.containerTimeSlots, MedicationTime(9, 0))
        }
        
        // Настройка кнопки добавления времени
        dialogBinding.buttonAddTime.setOnClickListener {
            addTimeSlot(dialogBinding.containerTimeSlots)
        }
        
        // Настройка кнопки отмены
        dialogBinding.buttonCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        
        // Настройка кнопки сохранения
        dialogBinding.buttonSave.setOnClickListener {
            // Проверка заполнения обязательных полей
            val name = dialogBinding.editMedicationName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(context, "Укажите название лекарства", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val dosage = dialogBinding.editMedicationDosage.text.toString().trim()
            if (dosage.isEmpty()) {
                Toast.makeText(context, "Укажите дозировку", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val frequencyText = dialogBinding.dropdownFrequency.text.toString()
            if (frequencyText.isEmpty()) {
                Toast.makeText(context, "Выберите частоту приема", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Преобразование строки в enum
            val frequency = MedicationFrequency.values().first { 
                it.getLocalizedName() == frequencyText 
            }
            
            // Парсинг дат
            val startDateText = dialogBinding.editStartDate.text.toString()
            if (startDateText.isEmpty()) {
                Toast.makeText(context, "Укажите дату начала приема", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val startDate = dateFormat.parse(startDateText) ?: Date()
            val endDateText = dialogBinding.editEndDate.text.toString()
            val endDate = if (endDateText.isEmpty()) null else dateFormat.parse(endDateText)
            
            // Сбор времен приема
            val times = mutableListOf<MedicationTime>()
            for (i in 0 until dialogBinding.containerTimeSlots.childCount) {
                val timeSlotView = dialogBinding.containerTimeSlots.getChildAt(i)
                val timeEditText = timeSlotView.findViewById<TextInputEditText>(R.id.edit_time)
                val timeText = timeEditText.text.toString()
                
                if (timeText.isEmpty()) {
                    Toast.makeText(context, "Укажите время приема", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val timeParts = timeText.split(":")
                if (timeParts.size == 2) {
                    val hour = timeParts[0].toInt()
                    val minute = timeParts[1].toInt()
                    times.add(MedicationTime(hour, minute))
                }
            }
            
            if (times.isEmpty()) {
                Toast.makeText(context, "Добавьте хотя бы одно время приема", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val notes = dialogBinding.editMedicationNotes.text.toString().trim()
            
            // Создание или обновление лекарства
            val updatedMedication = if (isEditing) {
                medication!!.copy(
                    name = name,
                    dosage = dosage,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate,
                    times = times,
                    notes = notes
                )
            } else {
                Medication(
                    id = System.currentTimeMillis(),
                    name = name,
                    dosage = dosage,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate,
                    times = times,
                    notes = notes
                )
            }
            
            // Сохранение в ViewModel
            if (isEditing) {
                viewModel.updateMedication(updatedMedication)
            } else {
                viewModel.addMedication(updatedMedication)
            }
            
            alertDialog.dismiss()
            Toast.makeText(context, "Лекарство сохранено", Toast.LENGTH_SHORT).show()
        }
        
        alertDialog.show()
    }
    
    /**
     * Добавляет слот для времени в контейнер
     */
    private fun addTimeSlot(
        container: ViewGroup, 
        existingTime: MedicationTime? = null
    ) {
        val timeSlotBinding = ItemTimeSlotBinding.inflate(layoutInflater, container, false)
        
        // Если передано существующее время, отображаем его
        if (existingTime != null) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, existingTime.hour)
            calendar.set(Calendar.MINUTE, existingTime.minute)
            timeSlotBinding.editTime.setText(timeFormat.format(calendar.time))
        }
        
        // Настройка выбора времени по клику
        timeSlotBinding.editTime.setOnClickListener {
            showTimePicker(timeSlotBinding.editTime)
        }
        
        // Настройка кнопки удаления времени
        timeSlotBinding.buttonRemoveTime.setOnClickListener {
            container.removeView(timeSlotBinding.root)
        }
        
        container.addView(timeSlotBinding.root)
    }
    
    /**
     * Показывает диалог выбора даты
     */
    private fun showDatePicker(
        dateEditText: TextInputEditText,
        isEndDate: Boolean = false
    ) {
        // Определяем текущую дату или берем из поля
        val dateText = dateEditText.text.toString()
        if (dateText.isNotEmpty()) {
            try {
                val date = dateFormat.parse(dateText)
                calendar.time = date ?: Date()
            } catch (e: Exception) {
                calendar.time = Date()
            }
        } else {
            calendar.time = Date()
        }
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                dateEditText.setText(dateFormat.format(calendar.time))
            },
            year,
            month,
            day
        )
        
        // Для даты окончания не устанавливаем минимальную дату
        if (!isEndDate) {
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        }
        
        datePickerDialog.show()
    }
    
    /**
     * Показывает диалог выбора времени
     */
    private fun showTimePicker(timeEditText: TextInputEditText) {
        // Определяем текущее время или берем из поля
        val timeText = timeEditText.text.toString()
        if (timeText.isNotEmpty()) {
            try {
                val time = timeFormat.parse(timeText)
                if (time != null) {
                    calendar.time = time
                }
            } catch (e: Exception) {
                calendar.time = Date()
            }
        } else {
            calendar.time = Date()
        }
        
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                timeEditText.setText(timeFormat.format(calendar.time))
            },
            hour,
            minute,
            true
        )
        
        timePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * Адаптер для списка лекарств
     */
    inner class MedicationsAdapter(
        private val medications: List<Medication>,
        private val onEditClick: (Medication) -> Unit
    ) : RecyclerView.Adapter<MedicationsAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTextView: TextView = view.findViewById(R.id.text_medication_name)
            val dosageTextView: TextView = view.findViewById(R.id.text_medication_dosage)
            val frequencyTextView: TextView = view.findViewById(R.id.text_medication_frequency)
            val timeTextView: TextView = view.findViewById(R.id.text_medication_time)
            val notesTextView: TextView = view.findViewById(R.id.text_medication_notes)
            val activeSwitch: SwitchMaterial = view.findViewById(R.id.switch_active)
            val editButton: Button = view.findViewById(R.id.button_edit)
            val deleteButton: Button = view.findViewById(R.id.button_delete)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_medication, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val medication = medications[position]
            
            holder.nameTextView.text = medication.name
            holder.dosageTextView.text = "Дозировка: ${medication.dosage}"
            holder.frequencyTextView.text = "Частота: ${medication.frequency.getLocalizedName()}"
            
            // Форматирование времен приема
            val times = medication.times.joinToString(", ") { 
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, it.hour)
                calendar.set(Calendar.MINUTE, it.minute)
                timeFormat.format(calendar.time)
            }
            holder.timeTextView.text = "Время: $times"
            
            // Отображаем примечания только если они есть
            if (medication.notes.isNotEmpty()) {
                holder.notesTextView.text = medication.notes
                holder.notesTextView.visibility = View.VISIBLE
            } else {
                holder.notesTextView.visibility = View.GONE
            }
            
            // Настройка переключателя активности
            holder.activeSwitch.isChecked = medication.isActive
            holder.activeSwitch.setOnCheckedChangeListener { _, isChecked ->
                val updatedMedication = medication.copy(isActive = isChecked)
                viewModel.updateMedication(updatedMedication)
            }
            
            // Настройка кнопки редактирования
            holder.editButton.setOnClickListener {
                onEditClick(medication)
            }
            
            // Настройка кнопки удаления
            holder.deleteButton.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Удаление лекарства")
                    .setMessage("Вы уверены, что хотите удалить ${medication.name}?")
                    .setPositiveButton("Удалить") { _, _ ->
                        viewModel.deleteMedication(medication.id)
                        Toast.makeText(context, "Лекарство удалено", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }
        
        override fun getItemCount() = medications.size
    }
    
    /**
     * Адаптер для списка ближайших напоминаний
     */
    inner class UpcomingAdapter(
        private val upcomingMedications: List<MedicationWithTime>
    ) : RecyclerView.Adapter<UpcomingAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTextView: TextView = view.findViewById(R.id.text_upcoming_name)
            val dosageTextView: TextView = view.findViewById(R.id.text_upcoming_dosage)
            val notesTextView: TextView = view.findViewById(R.id.text_upcoming_notes)
            val timeTextView: TextView = view.findViewById(R.id.text_upcoming_time)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_upcoming_medication, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val upcomingMed = upcomingMedications[position]
            val medication = upcomingMed.medication
            
            holder.nameTextView.text = medication.name
            holder.dosageTextView.text = medication.dosage
            
            // Отображаем примечания только если они есть
            if (medication.notes.isNotEmpty()) {
                holder.notesTextView.text = medication.notes
                holder.notesTextView.visibility = View.VISIBLE
            } else {
                holder.notesTextView.visibility = View.GONE
            }
            
            // Форматирование времени
            holder.timeTextView.text = timeFormat.format(upcomingMed.time)
        }
        
        override fun getItemCount() = upcomingMedications.size
    }
}