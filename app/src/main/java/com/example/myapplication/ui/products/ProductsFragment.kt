package com.example.myapplication.ui.products

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.model.Product
import com.example.myapplication.data.model.ScanStatus
import com.example.myapplication.databinding.FragmentProductsBinding
import com.example.myapplication.utils.BarcodeScanner
import com.google.android.material.tabs.TabLayout
import com.bumptech.glide.Glide
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat.getMainExecutor

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProductsViewModel
    private lateinit var productAdapter: ProductAdapter
    private var barcodeScanner: BarcodeScanner? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanning()
        } else {
            Toast.makeText(context, "Разрешение на использование камеры необходимо для сканирования штрих-кодов", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ProductsViewModel::class.java)

        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupButtons()
        setupObservers()

        return root
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            viewModel.checkProduct(product)
            showProductInfo(product, emptyList())
        }

        binding.recentProductsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    private fun setupButtons() {
        // Запуск сканирования штрих-кода
        binding.buttonStartScan.setOnClickListener {
            checkCameraPermission()
        }
        
        // Кнопка повторного сканирования
        binding.buttonScanAgain.setOnClickListener {
            showScannerView()
        }
        
        // Кнопка включения/выключения фонарика
        binding.fabFlashlight.setOnClickListener {
            // Реализация включения фонарика будет добавлена позже
            Toast.makeText(context, "Функция управления фонариком будет доступна в следующей версии", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startScanning()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    context,
                    "Разрешение на использование камеры необходимо для сканирования штрих-кодов",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startScanning() {
        showScannerView()
        
        barcodeScanner = BarcodeScanner(
            previewView = binding.viewFinder,
            lifecycleOwner = viewLifecycleOwner,
            onBarcodeDetected = { barcode ->
                // Остановка сканирования и обработка результата
                stopScanning()
                processBarcode(barcode)
            }
        )
        
        barcodeScanner?.startScanning()
        viewModel.setScanning(true)
    }
    
    private fun stopScanning() {
        barcodeScanner?.stopScanning()
        viewModel.setScanning(false)
    }
    
    private fun processBarcode(barcode: String) {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.getProductByBarcode(barcode)
    }
    
    private fun showScannerView() {
        binding.apply {
            recentProductsContainer.visibility = View.GONE
            resultCard.visibility = View.GONE
            viewFinder.visibility = View.VISIBLE
            scannerOverlay.visibility = View.VISIBLE
            scanningInstructions.visibility = View.VISIBLE
            fabFlashlight.visibility = View.VISIBLE
        }
    }
    
    private fun showResultView() {
        binding.apply {
            viewFinder.visibility = View.GONE
            scannerOverlay.visibility = View.GONE
            scanningInstructions.visibility = View.GONE
            fabFlashlight.visibility = View.GONE
            resultCard.visibility = View.VISIBLE
        }
    }
    
    private fun showRecentProductsView() {
        binding.apply {
            viewFinder.visibility = View.GONE
            scannerOverlay.visibility = View.GONE
            scanningInstructions.visibility = View.GONE
            fabFlashlight.visibility = View.GONE
            resultCard.visibility = View.GONE
            recentProductsContainer.visibility = View.VISIBLE
        }
        
        viewModel.loadRecentProducts()
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.recentProducts.observe(viewLifecycleOwner) { products ->
            if (products.isEmpty()) {
                binding.textEmptyRecentProducts.visibility = View.VISIBLE
            } else {
                binding.textEmptyRecentProducts.visibility = View.GONE
                // Обновляем список продуктов в RecyclerView
                productAdapter.updateProducts(products)
            }
        }
        
        viewModel.scanResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                when (result.status) {
                    ScanStatus.SUCCESS -> {
                        showProductInfo(result.product!!, emptyList())
                    }
                    ScanStatus.CONTAINS_ALLERGENS -> {
                        showProductInfo(result.product!!, result.allergenWarnings)
                    }
                    ScanStatus.NOT_FOUND -> {
                        Toast.makeText(context, "Продукт не найден: ${result.message}", Toast.LENGTH_SHORT).show()
                        showRecentProductsView()
                    }
                    ScanStatus.NETWORK_ERROR, ScanStatus.SCAN_ERROR -> {
                        Toast.makeText(context, "Ошибка: ${result.message}", Toast.LENGTH_SHORT).show()
                        showRecentProductsView()
                    }
                }
            }
        }
        
        viewModel.isScanning.observe(viewLifecycleOwner) { isScanning ->
            if (!isScanning && binding.viewFinder.visibility == View.VISIBLE) {
                showRecentProductsView()
            }
        }
    }
    
    private fun showProductInfo(product: Product, allergenWarnings: List<String>) {
        binding.apply {
            textProductName.text = product.name
            textProductBrand.text = product.brand ?: "Неизвестный бренд"
            
            if (allergenWarnings.isEmpty()) {
                imageProductStatus.setImageResource(android.R.drawable.ic_dialog_info)
                textProductStatus.text = "Продукт безопасен для вас"
                textProductStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            } else {
                imageProductStatus.setImageResource(android.R.drawable.ic_dialog_alert)
                textProductStatus.text = "Продукт содержит аллергены!"
                textProductStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            }
            
            // Отображение ингредиентов
            textIngredients.text = if (product.ingredients.isNotEmpty()) {
                product.ingredients.joinToString(", ")
            } else {
                "Нет данных о составе"
            }
            
            // Загрузка изображения с помощью Glide (если есть)
            if (product.imageUrl != null) {
                Glide.with(requireContext())
                    .load(product.imageUrl)
                    .into(imageProduct)
                imageProduct.visibility = View.VISIBLE
            } else {
                imageProduct.visibility = View.GONE
            }
            
            // Отображение списка аллергенов
            if (product.allergens.isNotEmpty()) {
                // Здесь можно реализовать отображение аллергенов в RecyclerView
                // Для простоты просто показываем текст
                textNoAllergens.visibility = View.GONE
                
                // В реальном приложении здесь был бы адаптер для списка аллергенов
                val allergensText = product.allergens.joinToString("\n") { 
                    val isUserAllergen = allergenWarnings.any { warning -> warning.contains(it, ignoreCase = true) }
                    val prefix = if (isUserAllergen) "⚠️ " else "• "
                    prefix + it
                }
                
                // Временное решение - отображаем текст вместо списка
                textIngredients.text = "Состав: ${product.ingredients.joinToString(", ")}\n\n" +
                                       "Аллергены:\n$allergensText"
            } else {
                textNoAllergens.visibility = View.VISIBLE
            }
        }
        
        showResultView()
    }

    override fun onDestroyView() {
        barcodeScanner?.shutdown()
        barcodeScanner = null
        super.onDestroyView()
        _binding = null
    }
} 