package com.example.myapplication.ui.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentTrackerBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TrackerFragment : Fragment() {

    private var _binding: FragmentTrackerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TrackerViewModel

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
            // TODO: Implement add record dialog
            Toast.makeText(context, "Добавление записи будет реализовано", Toast.LENGTH_SHORT).show()
        }

        // Observe reaction records
        viewModel.reactionRecords.observe(viewLifecycleOwner) { records ->
            recyclerView.adapter = ReactionAdapter(records)
        }

        return root
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