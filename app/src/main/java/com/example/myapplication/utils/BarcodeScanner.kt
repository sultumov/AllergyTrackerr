package com.example.myapplication.utils

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Утилита для сканирования штрих-кодов с помощью ML Kit и CameraX
 */
class BarcodeScanner(
    private val previewView: PreviewView,
    private val lifecycleOwner: LifecycleOwner,
    private val onBarcodeDetected: (String) -> Unit
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var processCameraProvider: ProcessCameraProvider? = null
    private var barcodeScanner: BarcodeScanner
    private val TAG = "BarcodeScanner"
    
    init {
        // Расширяем набор поддерживаемых форматов
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS  // Поддержка всех форматов
            )
            .build()
        
        barcodeScanner = BarcodeScanning.getClient(options)
        Log.d(TAG, "BarcodeScanner initialized")
    }
    
    /**
     * Запускает сканирование
     */
    fun startScanning() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
        
        cameraProviderFuture.addListener({
            try {
                processCameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                Log.d(TAG, "Camera provider initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(previewView.context))
    }
    
    /**
     * Останавливает сканирование
     */
    fun stopScanning() {
        try {
            processCameraProvider?.unbindAll()
            Log.d(TAG, "Camera scanning stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera: ${e.message}", e)
        }
    }
    
    /**
     * Настраивает камеру для сканирования
     */
    private fun bindCameraUseCases() {
        val cameraProvider = processCameraProvider ?: return
        
        try {
            // Настройка Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            // Настройка ImageAnalysis для анализа изображений с более высоким разрешением
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            
            imageAnalysis.setAnalyzer(cameraExecutor, BarcodeAnalyzer())
            
            // Отключаем все предыдущие биндинги
            cameraProvider.unbindAll()
            
            // Связываем камеру с жизненным циклом
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
            
            Log.d(TAG, "Camera use cases bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed: ${e.message}", e)
        }
    }
    
    /**
     * Анализатор изображений для обнаружения штрих-кодов
     */
    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            try {
                val mediaImage = imageProxy.image ?: run {
                    imageProxy.close()
                    Log.d(TAG, "Skipping frame - no mediaImage")
                    return
                }
                
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes[0]
                            barcode.rawValue?.let { rawValue ->
                                Log.d(TAG, "Barcode detected: $rawValue")
                                onBarcodeDetected(rawValue)
                            }
                        } else {
                            Log.d(TAG, "No barcodes detected in this frame")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Barcode scanning failed: ${e.message}", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing image: ${e.message}", e)
                imageProxy.close()
            }
        }
    }
    
    /**
     * Освобождает ресурсы
     */
    fun shutdown() {
        try {
            cameraExecutor.shutdown()
            barcodeScanner.close()
            Log.d(TAG, "BarcodeScanner resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down: ${e.message}", e)
        }
    }
} 