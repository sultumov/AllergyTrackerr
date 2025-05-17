package com.example.myapplication.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.model.Recipe
import com.example.myapplication.databinding.DialogAddRecipeBinding
import com.example.myapplication.databinding.FragmentRecipesBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecipesFragment : Fragment() {

    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RecipesViewModel
    private lateinit var apiRecipesAdapter: ApiRecipesAdapter
    private lateinit var customRecipesAdapter: CustomRecipesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(RecipesViewModel::class.java)

        _binding = FragmentRecipesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupTabLayout()
        setupRecyclerViews()
        setupSearchView()
        setupAddRecipeFab()
        setupObservers()

        // Загружаем безопасные рецепты при запуске
        viewModel.findSafeRecipes()

        return root
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> { // Безопасные рецепты (API)
                        showApiRecipes()
                        viewModel.findSafeRecipes()
                    }
                    1 -> { // Поиск рецептов
                        showApiRecipes()
                        showSearchView(true)
                    }
                    2 -> { // Мои рецепты
                        showCustomRecipes()
                        showSearchView(false)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun setupRecyclerViews() {
        // Настройка адаптера для API рецептов
        apiRecipesAdapter = ApiRecipesAdapter { recipe ->
            viewModel.getRecipeDetails(recipe.id)
            // Здесь можно добавить код для показа подробной информации о рецепте
            showRecipeDetailsDialog(recipe)
        }
        
        // Настройка адаптера для пользовательских рецептов
        customRecipesAdapter = CustomRecipesAdapter()
        
        binding.recyclerRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = apiRecipesAdapter // По умолчанию показываем API рецепты
        }
    }
    
    private fun setupSearchView() {
        binding.searchRecipes.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isNotEmpty()) {
                    viewModel.searchRecipes(query)
                }
                return true
            }
            
            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
    }
    
    private fun setupAddRecipeFab() {
        binding.fabAddRecipe.setOnClickListener {
            showAddRecipeDialog()
        }
    }
    
    private fun setupObservers() {
        // Наблюдение за API рецептами
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            apiRecipesAdapter.updateRecipes(recipes)
            binding.textNoRecipes.visibility = if (recipes.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Наблюдение за пользовательскими рецептами
        viewModel.customRecipes.observe(viewLifecycleOwner) { recipes ->
            customRecipesAdapter.updateRecipes(recipes)
        }
        
        // Наблюдение за состоянием загрузки
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Наблюдение за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showApiRecipes() {
        binding.recyclerRecipes.adapter = apiRecipesAdapter
    }
    
    private fun showCustomRecipes() {
        binding.recyclerRecipes.adapter = customRecipesAdapter
    }
    
    private fun showSearchView(show: Boolean) {
        binding.searchRecipes.visibility = if (show) View.VISIBLE else View.GONE
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
            
            viewModel.addCustomRecipe(title, ingredients, instructions, allergens)
            Toast.makeText(context, "Рецепт успешно добавлен", Toast.LENGTH_SHORT).show()
            
            // Переключаемся на вкладку с пользовательскими рецептами
            binding.tabLayout.getTabAt(2)?.select()
            
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }
    
    private fun showRecipeDetailsDialog(recipe: Recipe) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_recipe_details, null)
        
        // Находим все View в диалоге
        val titleTextView = view.findViewById<TextView>(R.id.text_recipe_title)
        val summaryTextView = view.findViewById<TextView>(R.id.text_recipe_summary)
        val servingsTextView = view.findViewById<TextView>(R.id.text_recipe_servings)
        val readyTimeTextView = view.findViewById<TextView>(R.id.text_recipe_ready_time)
        val dishTypesTextView = view.findViewById<TextView>(R.id.text_recipe_dish_types)
        
        // Заполняем информацией
        titleTextView.text = recipe.title
        summaryTextView.text = recipe.summary ?: "Нет описания"
        servingsTextView.text = "Порций: ${recipe.servings}"
        readyTimeTextView.text = "Время приготовления: ${recipe.readyInMinutes} мин"
        
        val dishTypes = recipe.dishTypes?.joinToString(", ") ?: "Не указано"
        dishTypesTextView.text = "Тип блюда: $dishTypes"
        
        // Наблюдаем за детальной информацией
        viewModel.recipeDetail.observe(viewLifecycleOwner) { recipeInfo ->
            if (recipeInfo != null && recipeInfo.id == recipe.id) {
                // Обновляем информацию, когда получаем детали
                summaryTextView.text = recipeInfo.summary
                
                // Добавляем информацию о диетических свойствах
                val dietInfo = buildString {
                    if (recipeInfo.glutenFree) append("✓ Без глютена\n")
                    if (recipeInfo.dairyFree) append("✓ Без молочных продуктов\n")
                    if (recipeInfo.vegetarian) append("✓ Вегетарианское\n")
                    if (recipeInfo.vegan) append("✓ Веганское\n")
                }
                if (dietInfo.isNotEmpty()) {
                    view.findViewById<TextView>(R.id.text_recipe_diet_info)?.apply {
                        text = dietInfo
                        visibility = View.VISIBLE
                    }
                }
            }
        }
        
        dialogBuilder.setView(view)
            .setPositiveButton("Закрыть") { dialog, _ ->
                dialog.dismiss()
            }
        
        dialogBuilder.create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Адаптер для отображения API рецептов
class ApiRecipesAdapter(
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<ApiRecipesAdapter.ViewHolder>() {

    private var recipes: List<Recipe> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.text_recipe_title)
        val readyTimeTextView: TextView = view.findViewById(R.id.text_recipe_ready_time)
        val servingsTextView: TextView = view.findViewById(R.id.text_recipe_servings)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_api_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.titleTextView.text = recipe.title
        holder.readyTimeTextView.text = "Время приготовления: ${recipe.readyInMinutes} мин"
        holder.servingsTextView.text = "Порций: ${recipe.servings}"
        
        holder.itemView.setOnClickListener {
            onRecipeClick(recipe)
        }
    }

    override fun getItemCount() = recipes.size
    
    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}

// Адаптер для отображения пользовательских рецептов
class CustomRecipesAdapter : RecyclerView.Adapter<CustomRecipesAdapter.ViewHolder>() {

    private var recipes: List<CustomRecipe> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.text_recipe_title)
        val ingredientsTextView: TextView = view.findViewById(R.id.text_recipe_ingredients)
        val instructionsTextView: TextView = view.findViewById(R.id.text_recipe_instructions)
        val allergensTextView: TextView = view.findViewById(R.id.text_recipe_allergens)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_custom_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.titleTextView.text = recipe.title
        holder.ingredientsTextView.text = "Ингредиенты: ${recipe.ingredients.joinToString(", ")}"
        holder.instructionsTextView.text = recipe.instructions
        
        if (recipe.allergens.isNotEmpty()) {
            holder.allergensTextView.visibility = View.VISIBLE
            holder.allergensTextView.text = "Содержит аллергены: ${recipe.allergens.joinToString(", ")}"
        } else {
            holder.allergensTextView.visibility = View.GONE
        }
    }

    override fun getItemCount() = recipes.size
    
    fun updateRecipes(newRecipes: List<CustomRecipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
} 