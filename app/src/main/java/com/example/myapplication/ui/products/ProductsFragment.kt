package com.example.myapplication.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.model.Product
import com.example.myapplication.databinding.FragmentProductsBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProductsViewModel
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ProductsViewModel::class.java)

        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupTabLayout()
        setupButtons()
        setupObservers()

        return root
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            viewModel.checkProduct(product)
            showProductResult()
        }

        binding.recyclerProducts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> switchToSearchByName()
                    1 -> switchToSearchByBarcode()
                    2 -> {
                        switchToSearchByName()
                        viewModel.findSafeProducts()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun switchToSearchByName() {
        binding.apply {
            searchLayout.visibility = View.VISIBLE
            barcodeLayout.visibility = View.GONE
            textProductResult.visibility = View.GONE
            recyclerProducts.visibility = View.GONE
        }
    }

    private fun switchToSearchByBarcode() {
        binding.apply {
            searchLayout.visibility = View.GONE
            barcodeLayout.visibility = View.VISIBLE
            textProductResult.visibility = View.GONE
            recyclerProducts.visibility = View.GONE
        }
    }

    private fun showProductsList() {
        binding.apply {
            textProductResult.visibility = View.GONE
            recyclerProducts.visibility = View.VISIBLE
        }
    }

    private fun showProductResult() {
        binding.apply {
            recyclerProducts.visibility = View.GONE
            textProductResult.visibility = View.VISIBLE
        }
    }

    private fun setupButtons() {
        // Поиск по названию
        binding.buttonCheckProduct.setOnClickListener {
            val productName = binding.editProductName.text.toString().trim()
            if (productName.isNotEmpty()) {
                viewModel.searchProducts(productName)
            } else {
                Toast.makeText(context, "Пожалуйста, введите название продукта", Toast.LENGTH_SHORT).show()
            }
        }

        // Поиск по штрих-коду
        binding.buttonCheckBarcode.setOnClickListener {
            val barcode = binding.editBarcode.text.toString().trim()
            if (barcode.isNotEmpty()) {
                viewModel.getProductByBarcode(barcode)
            } else {
                Toast.makeText(context, "Пожалуйста, введите штрих-код продукта", Toast.LENGTH_SHORT).show()
            }
        }

        // Сканирование штрих-кода (заглушка, для полной реализации потребуется дополнительная библиотека)
        binding.buttonScanBarcode.setOnClickListener {
            Toast.makeText(context, "Функция сканирования штрих-кода будет доступна в следующей версии", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.productCheckResult.observe(viewLifecycleOwner) { result ->
            binding.textProductResult.text = result
            showProductResult()
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { products ->
            productAdapter.updateProducts(products)
            showProductsList()
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            binding.textError.apply {
                text = errorMessage
                visibility = View.VISIBLE
            }
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 