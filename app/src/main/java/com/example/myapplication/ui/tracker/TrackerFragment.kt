package com.example.myapplication.ui.tracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.DialogAddReactionBinding
import com.example.myapplication.databinding.FragmentTrackerBinding
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrackerFragment : Fragment() {

    private var _binding: FragmentTrackerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TrackerViewModel
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(TrackerViewModel::class.java)

        _binding = FragmentTrackerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Setup RecyclerView
        val recyclerView: RecyclerView = binding.recyclerRecords
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Setup Add Record button
        val buttonAddRecord: Button = binding.buttonAddRecord
        buttonAddRecord.setOnClickListener {
            showAddReactionDialog()
        }

        // Observe reaction records
        viewModel.reactionRecords.observe(viewLifecycleOwner) { records ->
            recyclerView.adapter = ReactionAdapter(records)
        }

        return root
    }

    private fun showAddReactionDialog() {
        val dialogBinding = DialogAddReactionBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Установка текущей даты
        calendar.time = Date()
        dialogBinding.editReactionDate.setText(dateFormat.format(calendar.time))
        
        // Настройка выбора даты по клику
        dialogBinding.editReactionDate.setOnClickListener {
            showDatePicker(dialogBinding.editReactionDate)
        }
        
        // Настройка кнопок
        dialogBinding.buttonCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        
        dialogBinding.buttonSave.setOnClickListener {
            val symptoms = mutableListOf<String>()
            
            // Собираем выбранные симптомы из чипов
            for (i in 0 until dialogBinding.chipGroupSymptoms.childCount) {
                val chip = dialogBinding.chipGroupSymptoms.getChildAt(i) as Chip
                if (chip.isChecked) {
                    symptoms.add(chip.text.toString())
                }
            }
            
            // Добавляем другие симптомы, если они указаны
            val otherSymptom = dialogBinding.editOtherSymptom.text.toString().trim()
            if (otherSymptom.isNotEmpty()) {
                symptoms.add(otherSymptom)
            }
            
            // Если симптомы не указаны, показываем сообщение
            if (symptoms.isEmpty()) {
                Toast.makeText(context, "Пожалуйста, укажите хотя бы один симптом", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Получаем список триггеров, разделенных запятыми
            val triggersText = dialogBinding.editTriggers.text.toString().trim()
            val triggers = if (triggersText.isNotEmpty()) {
                triggersText.split(",").map { it.trim() }
            } else {
                listOf("Неизвестно")
            }
            
            // Получаем заметки
            val notes = dialogBinding.editNotes.text.toString().trim()
            
            // Создаем и сохраняем объект реакции
            val newReaction = AllergyReaction(
                id = System.currentTimeMillis(), // Используем timestamp как ID
                date = calendar.time,
                symptoms = symptoms,
                possibleTriggers = triggers,
                notes = notes
            )
            
            viewModel.addReaction(newReaction)
            alertDialog.dismiss()
            Toast.makeText(context, "Запись сохранена", Toast.LENGTH_SHORT).show()
        }
        
        alertDialog.show()
    }
    
    private fun showDatePicker(dateEditText: TextInputEditText) {
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
        
        // Установка максимальной даты (сегодня)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ReactionAdapter(private val reactions: List<AllergyReaction>) : 
    RecyclerView.Adapter<ReactionAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.text_reaction_date)
        val symptomsTextView: TextView = view.findViewById(R.id.text_reaction_symptoms)
        val triggersTextView: TextView = view.findViewById(R.id.text_reaction_triggers)
        val notesTextView: TextView = view.findViewById(R.id.text_reaction_notes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reaction = reactions[position]
        holder.dateTextView.text = "Дата: ${dateFormat.format(reaction.date)}"
        holder.symptomsTextView.text = reaction.symptoms.joinToString(", ")
        holder.triggersTextView.text = reaction.possibleTriggers.joinToString(", ")
        holder.notesTextView.text = "Примечание: ${reaction.notes}"
    }

    override fun getItemCount() = reactions.size
} 