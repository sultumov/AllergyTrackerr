package com.example.myapplication

import com.example.myapplication.data.api.BarcodeListRuService
import com.example.myapplication.data.api.ProductService
import com.example.myapplication.data.model.ScanStatus
import kotlinx.coroutines.runBlocking
import java.util.Scanner

/**
 * Утилита для тестирования API без запуска всего приложения.
 * Можно запустить как обычное Java-приложение с main-методом.
 */
object ApiTester {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("===== Тестирование API для сканирования штрих-кодов =====")
        println("Выберите опцию:")
        println("1. Проверить российский товар (barcode-list.ru)")
        println("2. Проверить товар в OpenFoodFacts")
        println("3. Автоматический поиск по обеим базам")
        println("4. Выход")
        
        val scanner = Scanner(System.`in`)
        var choice: Int
        
        do {
            print("\nВаш выбор (1-4): ")
            choice = scanner.nextInt()
            scanner.nextLine() // Очистка буфера
            
            when (choice) {
                1 -> testRussianApi(scanner)
                2 -> testOpenFoodFactsApi(scanner)
                3 -> testCombinedSearch(scanner)
                4 -> println("Выход из программы...")
                else -> println("Неверный выбор. Пожалуйста, выберите опцию 1-4.")
            }
        } while (choice != 4)
    }
    
    private fun testRussianApi(scanner: Scanner) {
        print("Введите штрих-код российского товара: ")
        val barcode = scanner.nextLine().trim()
        
        print("Введите аллергены через запятую (или оставьте пустым): ")
        val allergensInput = scanner.nextLine().trim()
        val allergens = if (allergensInput.isNotEmpty()) allergensInput.split(",").map { it.trim() } else emptyList()
        
        println("\nПоиск товара в российской базе данных...")
        
        runBlocking {
            try {
                val service = BarcodeListRuService()
                val result = service.getProductByBarcode(barcode, allergens)
                
                println("\n===== Результат =====")
                println("Статус: ${result.status}")
                
                if (result.status == ScanStatus.SUCCESS || result.status == ScanStatus.CONTAINS_ALLERGENS) {
                    println("Найден продукт: ${result.product?.name}")
                    println("Бренд: ${result.product?.brand ?: "Не указан"}")
                    println("Штрих-код: ${result.product?.barcode}")
                    println("Ингредиенты: ${result.product?.ingredients?.joinToString(", ") ?: "Не указаны"}")
                    println("Аллергены: ${result.product?.allergens?.joinToString(", ") ?: "Не указаны"}")
                    
                    if (result.status == ScanStatus.CONTAINS_ALLERGENS) {
                        println("\nВНИМАНИЕ! Продукт содержит аллергены:")
                        result.allergenWarnings.forEach { println("- $it") }
                    }
                } else {
                    println("Сообщение: ${result.message}")
                }
            } catch (e: Exception) {
                println("Ошибка при выполнении запроса: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun testOpenFoodFactsApi(scanner: Scanner) {
        print("Введите штрих-код товара: ")
        val barcode = scanner.nextLine().trim()
        
        print("Введите аллергены через запятую (или оставьте пустым): ")
        val allergensInput = scanner.nextLine().trim()
        val allergens = if (allergensInput.isNotEmpty()) allergensInput.split(",").map { it.trim() } else emptyList()
        
        println("\nПоиск товара в OpenFoodFacts...")
        
        runBlocking {
            try {
                val service = ProductService()
                // Используем модифицированный метод, который ищет только в OpenFoodFacts
                val result = service.searchInOpenFoodFacts(barcode, allergens)
                
                println("\n===== Результат =====")
                println("Статус: ${result.status}")
                
                if (result.status == ScanStatus.SUCCESS || result.status == ScanStatus.CONTAINS_ALLERGENS) {
                    println("Найден продукт: ${result.product?.name}")
                    println("Бренд: ${result.product?.brand ?: "Не указан"}")
                    println("Штрих-код: ${result.product?.barcode}")
                    println("Ингредиенты: ${result.product?.ingredients?.joinToString(", ") ?: "Не указаны"}")
                    println("Аллергены: ${result.product?.allergens?.joinToString(", ") ?: "Не указаны"}")
                    
                    if (result.status == ScanStatus.CONTAINS_ALLERGENS) {
                        println("\nВНИМАНИЕ! Продукт содержит аллергены:")
                        result.allergenWarnings.forEach { println("- $it") }
                    }
                } else {
                    println("Сообщение: ${result.message}")
                }
            } catch (e: Exception) {
                println("Ошибка при выполнении запроса: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun testCombinedSearch(scanner: Scanner) {
        print("Введите штрих-код товара: ")
        val barcode = scanner.nextLine().trim()
        
        print("Введите аллергены через запятую (или оставьте пустым): ")
        val allergensInput = scanner.nextLine().trim()
        val allergens = if (allergensInput.isNotEmpty()) allergensInput.split(",").map { it.trim() } else emptyList()
        
        println("\nПоиск товара в обеих базах данных...")
        
        runBlocking {
            try {
                val service = ProductService()
                val result = service.getProductByBarcode(barcode, allergens)
                
                println("\n===== Результат =====")
                println("Статус: ${result.status}")
                
                if (result.status == ScanStatus.SUCCESS || result.status == ScanStatus.CONTAINS_ALLERGENS) {
                    println("Найден продукт: ${result.product?.name}")
                    println("Бренд: ${result.product?.brand ?: "Не указан"}")
                    println("Штрих-код: ${result.product?.barcode}")
                    println("Ингредиенты: ${result.product?.ingredients?.joinToString(", ") ?: "Не указаны"}")
                    println("Аллергены: ${result.product?.allergens?.joinToString(", ") ?: "Не указаны"}")
                    
                    if (result.status == ScanStatus.CONTAINS_ALLERGENS) {
                        println("\nВНИМАНИЕ! Продукт содержит аллергены:")
                        result.allergenWarnings.forEach { println("- $it") }
                    }
                } else {
                    println("Сообщение: ${result.message}")
                }
            } catch (e: Exception) {
                println("Ошибка при выполнении запроса: ${e.message}")
                e.printStackTrace()
            }
        }
    }
} 