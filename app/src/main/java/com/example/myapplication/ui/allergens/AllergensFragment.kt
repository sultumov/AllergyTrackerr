package com.example.myapplication.ui.allergens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentAllergensBinding

class AllergensFragment : Fragment() {

    private var _binding: FragmentAllergensBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AllergensViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(AllergensViewModel::class.java)

        _binding = FragmentAllergensBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Setup RecyclerView
        val recyclerView: RecyclerView = binding.recyclerAllergens
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Setup Search functionality
        val searchView: SearchView = binding.searchAllergens
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchAllergens(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.searchAllergens(it) }
                return true
            }
        })

        // Observe allergens list
        viewModel.allergens.observe(viewLifecycleOwner) { allergens ->
            recyclerView.adapter = AllergensAdapter(allergens)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Адаптер для отображения аллергенов
class AllergensAdapter(private val allergens: List<Allergen>) : 
    RecyclerView.Adapter<AllergensAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.text_allergen_name)
        val categoryTextView: TextView = view.findViewById(R.id.text_allergen_category)
        val symptomsTextView: TextView = view.findViewById(R.id.text_allergen_symptoms)
        val recommendationsTextView: TextView = view.findViewById(R.id.text_allergen_recommendations)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_allergen, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val allergen = allergens[position]
        holder.nameTextView.text = allergen.name
        holder.categoryTextView.text = "Категория: ${allergen.category}"
        holder.symptomsTextView.text = "Симптомы: ${allergen.symptoms.joinToString(", ")}"
        holder.recommendationsTextView.text = "Рекомендации: ${allergen.recommendations}"
    }

    override fun getItemCount() = allergens.size
} 