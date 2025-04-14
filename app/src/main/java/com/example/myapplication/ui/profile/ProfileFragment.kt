package com.example.myapplication.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.data.User
import com.example.myapplication.databinding.FragmentProfileBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupViews()
        setupObservers()

        return root
    }

    private fun setupViews() {
        // Настраиваем обработчик для добавления аллергена
        binding.buttonAddAllergen.setOnClickListener {
            val allergen = binding.editAddAllergen.text.toString().trim()
            if (allergen.isNotBlank()) {
                viewModel.addAllergen(allergen)
                binding.editAddAllergen.text?.clear()
            } else {
                Toast.makeText(context, "Введите название аллергена", Toast.LENGTH_SHORT).show()
            }
        }

        // Настраиваем обработчик для сохранения профиля
        binding.buttonSaveProfile.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun setupObservers() {
        // Наблюдаем за изменениями данных пользователя
        viewModel.user.observe(viewLifecycleOwner) { user ->
            updateUserInterface(user)
        }

        // Наблюдаем за изменениями списка аллергенов
        viewModel.allergens.observe(viewLifecycleOwner) { allergens ->
            updateAllergensChips(allergens)
        }
    }

    private fun updateUserInterface(user: User) {
        binding.editName.setText(user.name)
        binding.editAge.setText(if (user.age > 0) user.age.toString() else "")
        
        when (user.gender) {
            "мужской" -> binding.radioMale.isChecked = true
            "женский" -> binding.radioFemale.isChecked = true
        }
        
        binding.editMedicalNotes.setText(user.medicalNotes)
    }

    private fun updateAllergensChips(allergens: List<String>) {
        binding.chipGroupAllergens.removeAllViews()
        
        allergens.forEach { allergen ->
            val chip = Chip(context).apply {
                text = allergen
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    viewModel.removeAllergen(allergen)
                }
            }
            binding.chipGroupAllergens.addView(chip)
        }
    }

    private fun saveUserProfile() {
        val name = binding.editName.text.toString().trim()
        val ageText = binding.editAge.text.toString().trim()
        val age = if (ageText.isNotEmpty()) ageText.toInt() else 0
        
        val gender = when (binding.radioGroupGender.checkedRadioButtonId) {
            R.id.radio_male -> "мужской"
            R.id.radio_female -> "женский"
            else -> ""
        }
        
        val medicalNotes = binding.editMedicalNotes.text.toString().trim()
        
        viewModel.saveUser(name, age, gender, medicalNotes)
        Toast.makeText(context, "Профиль сохранен", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 