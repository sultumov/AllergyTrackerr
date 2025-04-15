package com.example.myapplication.ui.allergens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.model.Allergen
import com.example.myapplication.data.model.getLocalizedName
import com.example.myapplication.databinding.DialogAllergenDetailBinding
import com.example.myapplication.databinding.FragmentAllergensBinding

class AllergensFragment : Fragment() {

    private var _binding: FragmentAllergensBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AllergensViewModel
    private lateinit var allergensAdapter: AllergensAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(AllergensViewModel::class.java)

        _binding = FragmentAllergensBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupCategorySpinner()
        setupSearchView()
        setupObservers()

        return root
    }
    
    private fun setupRecyclerView() {
        allergensAdapter = AllergensAdapter { allergen ->
            showAllergenDetailDialog(allergen)
        }
        
        binding.recyclerAllergens.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = allergensAdapter
        }
    }
    
    private fun setupCategorySpinner() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val categoryNames = categories.map { it.getLocalizedName() }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryNames
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            
            binding.spinnerCategories.apply {
                this.adapter = adapter
                setSelection(0, false)

                setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position == 0) {
                            viewModel.loadAllAllergens()
                        } else {
                            viewModel.filterByCategory(categories[position - 1])
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // Ничего не делаем или реализуем необходимую логику
                    }
                })
            }
        }
    }
    
    private fun setupSearchView() {
        binding.searchAllergens.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchAllergens(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.searchAllergens(it) }
                return true
            }
        })
    }
    
    private fun setupObservers() {
        // Наблюдение за списком аллергенов
        viewModel.allergens.observe(viewLifecycleOwner) { allergens ->
            allergensAdapter.updateAllergens(allergens)
            binding.textNoAllergens.visibility = if (allergens.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Наблюдение за состоянием загрузки
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Наблюдение за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAllergenDetailDialog(allergen: Allergen) {
        viewModel.selectAllergen(allergen)
        
        val dialogBinding = DialogAllergenDetailBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
        
        // Заполняем основную информацию
        dialogBinding.textAllergenName.text = allergen.name
        dialogBinding.textAllergenCategory.text = allergen.category.getLocalizedName()
        dialogBinding.textAllergenDescription.text = allergen.description
        
        // Симптомы
        val symptomsText = allergen.symptoms.joinToString("\n• ", prefix = "• ")
        dialogBinding.textAllergenSymptoms.text = symptomsText
        
        // Рекомендации
        val recommendationsText = allergen.avoidanceRecommendations.joinToString("\n• ", prefix = "• ")
        dialogBinding.textAllergenRecommendations.text = recommendationsText
        
        // Связанные аллергены
        if (allergen.relatedAllergens.isNotEmpty()) {
            dialogBinding.layoutRelatedAllergens.visibility = View.VISIBLE
            dialogBinding.textRelatedAllergens.text = allergen.relatedAllergens.joinToString(", ")
        } else {
            dialogBinding.layoutRelatedAllergens.visibility = View.GONE
        }
        
        // Научное название
        if (allergen.scientificName != null) {
            dialogBinding.layoutScientificName.visibility = View.VISIBLE
            dialogBinding.textScientificName.text = allergen.scientificName
        } else {
            dialogBinding.layoutScientificName.visibility = View.GONE
        }
        
        // Дополнительная информация из API
        viewModel.scientificInfo.observe(viewLifecycleOwner) { articles ->
            if (articles.isNotEmpty()) {
                dialogBinding.layoutApiInfo.visibility = View.VISIBLE
                val article = articles.first()
                dialogBinding.textArticleTitle.text = article.title
                dialogBinding.textArticleAbstract.text = article.abstract
                dialogBinding.textArticleSource.text = "Источник: ${article.source}"
            } else {
                dialogBinding.layoutApiInfo.visibility = View.GONE
            }
        }
        
        viewModel.wikipediaInfo.observe(viewLifecycleOwner) { wikiInfo ->
            if (wikiInfo != null) {
                dialogBinding.layoutWikipediaInfo.visibility = View.VISIBLE
                dialogBinding.textWikipediaTitle.text = wikiInfo.title
                dialogBinding.textWikipediaContent.text = wikiInfo.content
            } else {
                dialogBinding.layoutWikipediaInfo.visibility = View.GONE
            }
        }
        
        // Показываем диалог
        alertDialog.show()
        
        // При закрытии диалога очищаем выбранный аллерген
        alertDialog.setOnDismissListener {
            viewModel.clearSelectedAllergen()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Адаптер для отображения аллергенов
class AllergensAdapter(
    private val onAllergenClick: (Allergen) -> Unit
) : RecyclerView.Adapter<AllergensAdapter.ViewHolder>() {

    private var allergens: List<Allergen> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.text_allergen_name)
        val categoryTextView: TextView = view.findViewById(R.id.text_allergen_category)
        val descriptionTextView: TextView = view.findViewById(R.id.text_allergen_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_allergen, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val allergen = allergens[position]
        holder.nameTextView.text = allergen.name
        holder.categoryTextView.text = allergen.category.getLocalizedName()
        holder.descriptionTextView.text = allergen.description
        
        holder.itemView.setOnClickListener {
            onAllergenClick(allergen)
        }
    }

    override fun getItemCount() = allergens.size
    
    fun updateAllergens(newAllergens: List<Allergen>) {
        allergens = newAllergens
        notifyDataSetChanged()
    }
} 