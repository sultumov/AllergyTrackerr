package com.example.myapplication.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.FragmentProductsBinding

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProductsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ProductsViewModel::class.java)

        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val editProductName: EditText = binding.editProductName
        val buttonCheckProduct: Button = binding.buttonCheckProduct
        val textProductResult: TextView = binding.textProductResult

        // Set up check button
        buttonCheckProduct.setOnClickListener {
            val productName = editProductName.text.toString().trim()
            if (productName.isNotEmpty()) {
                viewModel.checkProduct(productName)
            } else {
                Toast.makeText(context, "Пожалуйста, введите название продукта", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe product check result
        viewModel.productCheckResult.observe(viewLifecycleOwner) { result ->
            textProductResult.text = result
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 