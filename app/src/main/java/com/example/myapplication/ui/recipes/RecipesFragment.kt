package com.example.myapplication.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.DialogAddRecipeBinding
import com.example.myapplication.databinding.FragmentRecipesBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RecipesFragment : Fragment() {

    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RecipesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(RecipesViewModel::class.java)

        _binding = FragmentRecipesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Setup RecyclerView
        val recyclerView: RecyclerView = binding.recyclerRecipes
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Setup Add Recipe FAB
        val fabAddRecipe: FloatingActionButton = binding.fabAddRecipe
        fabAddRecipe.setOnClickListener {
            showAddRecipeDialog()
        }
        
        // Observe recipes list
        viewModel.safeRecipes.observe(viewLifecycleOwner) { recipes ->
            recyclerView.adapter = RecipesAdapter(recipes)
        }

        return root
    }
    
    private fun showAddRecipeDialog() {
        val dialogBinding = DialogAddRecipeBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
            
        // Настройка кнопок
        dialogBinding.buttonCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        
        dialogBinding.buttonSave.setOnClickListener {
            val title = dialogBinding.editRecipeTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(context, "Пожалуйста, введите название рецепта", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val ingredientsText = dialogBinding.editIngredients.text.toString().trim()
            if (ingredientsText.isEmpty()) {
                Toast.makeText(context, "Пожалуйста, введите ингредиенты", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ingredients = ingredientsText.split(",").map { it.trim() }
            
            val instructions = dialogBinding.editInstructions.text.toString().trim()
            if (instructions.isEmpty()) {
                Toast.makeText(context, "Пожалуйста, введите способ приготовления", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val allergensText = dialogBinding.editAllergens.text.toString().trim()
            val allergens = if (allergensText.isNotEmpty()) {
                allergensText.split(",").map { it.trim().lowercase() }
            } else {
                emptyList()
            }
            
            viewModel.addRecipe(title, ingredients, instructions, allergens)
            Toast.makeText(context, "Рецепт успешно добавлен", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }
    
    override fun onResume() {
        super.onResume()
        // Перезагружаем список рецептов при возвращении к фрагменту
        // для учета возможных изменений в профиле пользователя
        viewModel.loadSafeRecipes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Адаптер для отображения рецептов
class RecipesAdapter(private val recipes: List<Recipe>) : 
    RecyclerView.Adapter<RecipesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.text_recipe_title)
        val ingredientsTextView: TextView = view.findViewById(R.id.text_recipe_ingredients)
        val instructionsTextView: TextView = view.findViewById(R.id.text_recipe_instructions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.titleTextView.text = recipe.title
        holder.ingredientsTextView.text = recipe.ingredients.joinToString(", ")
        holder.instructionsTextView.text = recipe.instructions
    }

    override fun getItemCount() = recipes.size
} 