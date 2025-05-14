package com.example.myapplication.data.api

import com.example.myapplication.data.model.ApiProduct
import com.example.myapplication.data.model.BarcodeListProduct
import com.example.myapplication.data.model.BarcodeListResponse
import com.example.myapplication.data.model.Ingredient
import com.example.myapplication.data.model.ProductResponse
import com.example.myapplication.data.model.ScanStatus
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProductServiceTest {
    
    private lateinit var mockWebServerRu: MockWebServer
    private lateinit var mockWebServerOff: MockWebServer
    private lateinit var barcodeListRuApi: BarcodeListRuApi
    private lateinit var openFoodFactsApi: OpenFoodFactsApi
    private lateinit var gson: Gson
    
    // Тестовый сервис, который будет использовать мок-API вместо реальных
    private val testService by lazy {
        object : ProductService() {
            override val barcodeListRuService = object : BarcodeListRuService() {
                override val api: BarcodeListRuApi = this@ProductServiceTest.barcodeListRuApi
            }
            
            override val openFoodFactsApi: OpenFoodFactsApi = this@ProductServiceTest.openFoodFactsApi
        }
    }
    
    @Before
    fun setup() {
        // Настройка MockWebServer для российского API
        mockWebServerRu = MockWebServer()
        mockWebServerRu.start()
        
        // Настройка MockWebServer для OpenFoodFacts
        mockWebServerOff = MockWebServer()
        mockWebServerOff.start()
        
        // Создаем Gson для сериализации/десериализации JSON
        gson = Gson()
        
        // Настраиваем Retrofit для российского API
        val retrofitRu = Retrofit.Builder()
            .baseUrl(mockWebServerRu.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        // Настраиваем Retrofit для OpenFoodFacts
        val retrofitOff = Retrofit.Builder()
            .baseUrl(mockWebServerOff.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        // Создаем тестовые реализации API
        barcodeListRuApi = retrofitRu.create(BarcodeListRuApi::class.java)
        openFoodFactsApi = retrofitOff.create(OpenFoodFactsApi::class.java)
    }
    
    @After
    fun tearDown() {
        mockWebServerRu.shutdown()
        mockWebServerOff.shutdown()
    }
    
    @Test
    fun `test getProductByBarcode with Russian barcode found in Russian DB`() = runBlocking {
        // Подготавливаем тестовые данные для российского API
        val testBarcode = "4600123456789"
        val testProduct = BarcodeListProduct(
            barcode = testBarcode,
            name = "Тестовый российский продукт",
            fullName = null,
            brand = "ТестБренд",
            manufacturer = "ТестПроизводитель",
            country = "Россия",
            category = "Продукты",
            subcategory = null,
            image = null,
            description = null,
            ingredients = "молоко, сахар",
            allergens = listOf("молоко")
        )
        
        val responseJson = gson.toJson(BarcodeListResponse(
            status = "success",
            message = null,
            products = listOf(testProduct)
        ))
        
        // Настраиваем мок-сервер для российского API
        mockWebServerRu.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
        )
        
        // Выполняем тестируемый метод
        val result = testService.getProductByBarcode(testBarcode, emptyList())
        
        // Проверяем результат
        assertEquals(ScanStatus.SUCCESS, result.status)
        assertEquals("Тестовый российский продукт", result.product?.name)
        
        // Проверяем, что запрос был отправлен только на российский API
        assertEquals(1, mockWebServerRu.requestCount)
        assertEquals(0, mockWebServerOff.requestCount)
    }
    
    @Test
    fun `test getProductByBarcode with Russian barcode not found in Russian DB but found in OpenFoodFacts`() = runBlocking {
        val testBarcode = "4600123456789"
        
        // Ответ от российского API - продукт не найден
        val responseRuJson = gson.toJson(BarcodeListResponse(
            status = "error",
            message = "Product not found",
            products = null
        ))
        
        // Настраиваем мок-сервер для российского API
        mockWebServerRu.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseRuJson)
        )
        
        // Ответ от OpenFoodFacts - продукт найден
        val apiProduct = ApiProduct(
            id = testBarcode,
            productName = "Тестовый продукт в OpenFoodFacts",
            brands = "ТестБренд",
            categories = "Молочные продукты",
            ingredients = listOf(Ingredient("1", "молоко", 1)),
            allergensText = "молоко",
            allergensTags = listOf("en:milk"),
            imageUrl = null,
            nutrients = null,
            nutriScore = null
        )
        
        val responseOffJson = gson.toJson(ProductResponse(
            status = 1,
            code = testBarcode,
            product = apiProduct,
            statusVerbose = "product found"
        ))
        
        // Настраиваем мок-сервер для OpenFoodFacts
        mockWebServerOff.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseOffJson)
        )
        
        // Выполняем тестируемый метод
        val result = testService.getProductByBarcode(testBarcode, emptyList())
        
        // Проверяем результат
        assertEquals(ScanStatus.SUCCESS, result.status)
        assertEquals("Тестовый продукт в OpenFoodFacts", result.product?.name)
        
        // Проверяем, что запросы были отправлены на оба API
        assertEquals(1, mockWebServerRu.requestCount)
        assertEquals(1, mockWebServerOff.requestCount)
    }
    
    @Test
    fun `test getProductByBarcode with non-Russian barcode goes directly to OpenFoodFacts`() = runBlocking {
        val testBarcode = "3800123456789" // Не российский штрих-код
        
        // Ответ от OpenFoodFacts - продукт найден
        val apiProduct = ApiProduct(
            id = testBarcode,
            productName = "Иностранный продукт",
            brands = "ForeignBrand",
            categories = "Snacks",
            ingredients = listOf(Ingredient("1", "wheat flour", 1)),
            allergensText = "wheat",
            allergensTags = listOf("en:wheat"),
            imageUrl = null,
            nutrients = null,
            nutriScore = null
        )
        
        val responseOffJson = gson.toJson(ProductResponse(
            status = 1,
            code = testBarcode,
            product = apiProduct,
            statusVerbose = "product found"
        ))
        
        // Настраиваем мок-сервер для OpenFoodFacts
        mockWebServerOff.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseOffJson)
        )
        
        // Выполняем тестируемый метод
        val result = testService.getProductByBarcode(testBarcode, emptyList())
        
        // Проверяем результат
        assertEquals(ScanStatus.SUCCESS, result.status)
        assertEquals("Иностранный продукт", result.product?.name)
        
        // Проверяем, что запрос был отправлен только на OpenFoodFacts
        assertEquals(0, mockWebServerRu.requestCount)
        assertEquals(1, mockWebServerOff.requestCount)
    }
    
    @Test
    fun `test getProductByBarcode with allergens returns CONTAINS_ALLERGENS`() = runBlocking {
        val testBarcode = "3800123456789" // Не российский штрих-код
        
        // Ответ от OpenFoodFacts - продукт с аллергеном
        val apiProduct = ApiProduct(
            id = testBarcode,
            productName = "Аллергенный продукт",
            brands = "AllergyBrand",
            categories = "Snacks",
            ingredients = listOf(Ingredient("1", "peanuts", 1)),
            allergensText = "peanuts",
            allergensTags = listOf("en:peanuts"),
            imageUrl = null,
            nutrients = null,
            nutriScore = null
        )
        
        val responseOffJson = gson.toJson(ProductResponse(
            status = 1,
            code = testBarcode,
            product = apiProduct,
            statusVerbose = "product found"
        ))
        
        // Настраиваем мок-сервер для OpenFoodFacts
        mockWebServerOff.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseOffJson)
        )
        
        // Пользователь с аллергией на арахис
        val userAllergens = listOf("арахис")
        
        // Выполняем тестируемый метод
        val result = testService.getProductByBarcode(testBarcode, userAllergens)
        
        // Проверяем результат
        assertEquals(ScanStatus.CONTAINS_ALLERGENS, result.status)
        assertEquals("Аллергенный продукт", result.product?.name)
        assertEquals(1, result.allergenWarnings.size)
    }
    
    @Test
    fun `test getProductByBarcode when product not found in any database`() = runBlocking {
        val testBarcode = "3800123456789" // Не российский штрих-код
        
        // Ответ от OpenFoodFacts - продукт не найден
        val responseOffJson = gson.toJson(ProductResponse(
            status = 0,
            code = testBarcode,
            product = null,
            statusVerbose = "product not found"
        ))
        
        // Настраиваем мок-сервер для OpenFoodFacts
        mockWebServerOff.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseOffJson)
        )
        
        // Выполняем тестируемый метод
        val result = testService.getProductByBarcode(testBarcode, emptyList())
        
        // Проверяем результат
        assertEquals(ScanStatus.NOT_FOUND, result.status)
        assertEquals("Продукт с таким штрих-кодом не найден ни в одной базе данных", result.message)
    }
} 