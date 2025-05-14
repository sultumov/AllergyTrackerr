package com.example.myapplication.data.api

import com.example.myapplication.data.model.BarcodeListProduct
import com.example.myapplication.data.model.BarcodeListResponse
import com.example.myapplication.data.model.ScanStatus
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class BarcodeListRuServiceTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: BarcodeListRuApi
    private lateinit var gson: Gson
    
    // Тестовый сервис, который будет использовать мок-API вместо реального
    private val testService by lazy {
        object : BarcodeListRuService() {
            override val api: BarcodeListRuApi = this@BarcodeListRuServiceTest.api
        }
    }
    
    @Before
    fun setup() {
        // Настройка MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        // Создаем Gson для сериализации/десериализации JSON
        gson = Gson()
        
        // Настраиваем Retrofit для работы с MockWebServer
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        // Создаем тестовую реализацию API
        api = retrofit.create(BarcodeListRuApi::class.java)
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `test getProductByBarcode when product exists returns SUCCESS`() = runBlocking {
        // Подготавливаем тестовые данные
        val testBarcode = "4600123456789"
        val testProduct = BarcodeListProduct(
            barcode = testBarcode,
            name = "Тестовый продукт",
            fullName = "Тестовый продукт полное название",
            brand = "ТестБренд",
            manufacturer = "ТестПроизводитель",
            country = "Россия",
            category = "Продукты",
            subcategory = "Молочные",
            image = "https://example.com/image.jpg",
            description = "Описание тестового продукта",
            ingredients = "молоко, сахар, крахмал",
            allergens = listOf("молоко")
        )
        
        val responseJson = gson.toJson(BarcodeListResponse(
            status = "success",
            message = null,
            products = listOf(testProduct)
        ))
        
        // Настраиваем мок-сервер для отправки подготовленного ответа
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .setBodyDelay(100, TimeUnit.MILLISECONDS) // имитируем небольшую задержку сети
        )
        
        // Выполняем тестируемый метод
        val result = testService.getProductByBarcode(testBarcode, emptyList())
        
        // Проверяем, что запрос был выполнен правильно
        val request = mockWebServer.takeRequest()
        assertEquals("/api/v1/barcode/?barcode=$testBarcode", request.path)
        
        // Проверяем, что результат соответствует ожиданиям
        assertEquals(ScanStatus.SUCCESS, result.status)
        assertNotNull(result.product)
        assertEquals(testBarcode, result.product?.barcode)
        assertEquals("Тестовый продукт", result.product?.name)
        assertEquals("ТестБренд", result.product?.brand)
    }
    
    @Test
    fun `test getProductByBarcode when product contains allergens returns CONTAINS_ALLERGENS`() = runBlocking {
        // Подготавливаем тестовые данные
        val testBarcode = "4600123456789"
        val testProduct = BarcodeListProduct(
            barcode = testBarcode,
            name = "Тестовый продукт",
            fullName = "Тестовый продукт полное название",
            brand = "ТестБренд",
            manufacturer = "ТестПроизводитель",
            country = "Россия",
            category = "Продукты",
            subcategory = "Молочные",
            image = "https://example.com/image.jpg",
            description = "Описание тестового продукта",
            ingredients = "молоко, сахар, крахмал, арахис",
            allergens = listOf("молоко", "арахис")
        )
        
        val responseJson = gson.toJson(BarcodeListResponse(
            status = "success",
            message = null,
            products = listOf(testProduct)
        ))
        
        // Настраиваем мок-сервер для отправки подготовленного ответа
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
        )
        
        // Пользователь с аллергией на арахис
        val userAllergens = listOf("арахис")
        
        // Выполняем тестируемый метод
        val result = testService.getProductByBarcode(testBarcode, userAllergens)
        
        // Проверяем результат
        assertEquals(ScanStatus.CONTAINS_ALLERGENS, result.status)
        assertNotNull(result.product)
        assertEquals(1, result.allergenWarnings.size)
        assertEquals("Содержит аллерген: арахис", result.allergenWarnings[0])
    }
    
    @Test
    fun `test getProductByBarcode when product not found returns NOT_FOUND`() = runBlocking {
        // Подготавливаем ответ с ошибкой
        val responseJson = gson.toJson(BarcodeListResponse(
            status = "error",
            message = "Product not found",
            products = null
        ))
        
        // Настраиваем мок-сервер
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
        )
        
        // Выполняем тестируемый метод
        val result = testService.getProductByBarcode("4600111111111", emptyList())
        
        // Проверяем результат
        assertEquals(ScanStatus.NOT_FOUND, result.status)
        assertEquals("Продукт с таким штрих-кодом не найден в российской базе данных", result.message)
    }
    
    @Test
    fun `test getProductByBarcode when network error returns NETWORK_ERROR`() = runBlocking {
        // Настраиваем мок-сервер на ответ с ошибкой
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\":\"Internal Server Error\"}")
        )
        
        // Выполняем тестируемый метод
        val result = testService.getProductByBarcode("4600111111111", emptyList())
        
        // Проверяем результат
        assertEquals(ScanStatus.NETWORK_ERROR, result.status)
        assertEquals("Ошибка при получении данных (500)", result.message)
    }
} 