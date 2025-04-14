package com.example.myapplication.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentRecipesBinding

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
        
        // Observe recipes list
        viewModel.safeRecipes.observe(viewLifecycleOwner) { recipes ->
            recyclerView.adapter = RecipesAdapter(recipes)
        }

        return root
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