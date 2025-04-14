package com.example.myapplication.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.ui.tracker.AllergyReaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Date

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("allergy_prefs", 0)
    private val gson = Gson()
    
    // Модель данных для графика частоты
    data class FrequencyData(val date: Date, val count: Int)
    
    // Модель данных для графика симптомов
    data class SymptomData(val name: String, val count: Int)
    
    // Модель данных для графика триггеров
    data class TriggerData(val name: String, val count: Int)
    
    // Модель данных для анализа тенденций
    data class TrendAnalysis(
        val totalReactions: Int,
        val averagePerWeek: Float,
        val changePercent: Float,
        val mostFrequentSymptom: String,
        val mostFrequentTrigger: String,
        val isIncreasing: Boolean
    )
    
    // LiveData для графиков
    private val _frequencyData = MutableLiveData<List<FrequencyData>>()
    val frequencyData: LiveData<List<FrequencyData>> = _frequencyData
    
    private val _symptomData = MutableLiveData<List<SymptomData>>()
    val symptomData: LiveData<List<SymptomData>> = _symptomData
    
    private val _triggerData = MutableLiveData<List<TriggerData>>()
    val triggerData: LiveData<List<TriggerData>> = _triggerData
    
    // LiveData для анализа тенденций
    private val _trendAnalysis = MutableLiveData<TrendAnalysis?>()
    val trendAnalysis: LiveData<TrendAnalysis?> = _trendAnalysis
    
    // Периоды для фильтрации
    enum class Period { WEEK, MONTH, ALL }
    
    init {
        loadStatistics(Period.WEEK)
    }
    
    fun loadStatistics(period: Period) {
        val reactions = loadReactions()
        val filteredReactions = filterReactionsByPeriod(reactions, period)
        
        processFrequencyData(filteredReactions, period)
        processSymptomsData(filteredReactions)
        processTriggersData(filteredReactions)
        analyzeReactionTrends(reactions)
    }
    
    private fun loadReactions(): List<AllergyReaction> {
        val reactionsJson = sharedPreferences.getString("saved_reactions", null)
        return if (reactionsJson != null) {
            val type = object : TypeToken<List<AllergyReaction>>() {}.type
            gson.fromJson(reactionsJson, type)
        } else {
            emptyList()
        }
    }
    
    private fun filterReactionsByPeriod(reactions: List<AllergyReaction>, period: Period): List<AllergyReaction> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        return when (period) {
            Period.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.timeInMillis
                reactions.filter { it.date.time >= weekAgo }
            }
            Period.MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                val monthAgo = calendar.timeInMillis
                reactions.filter { it.date.time >= monthAgo }
            }
            Period.ALL -> reactions
        }
    }
    
    private fun processFrequencyData(reactions: List<AllergyReaction>, period: Period) {
        val dateMap = mutableMapOf<Long, Int>()
        val calendar = Calendar.getInstance()
        
        // Группировка реакций по дням
        reactions.forEach { reaction ->
            calendar.time = reaction.date
            resetTimeToMidnight(calendar)
            val dayTimestamp = calendar.timeInMillis
            
            dateMap[dayTimestamp] = (dateMap[dayTimestamp] ?: 0) + 1
        }
        
        // Создание полного диапазона дат в зависимости от периода
        val dateRange = when (period) {
            Period.WEEK -> createDateRange(7)
            Period.MONTH -> createDateRange(30)
            Period.ALL -> {
                if (reactions.isEmpty()) {
                    createDateRange(7) // Если нет данных, показываем неделю
                } else {
                    // Находим самую раннюю дату реакции
                    val earliestReaction = reactions.minByOrNull { it.date.time }
                    if (earliestReaction != null) {
                        val daysBetween = calculateDaysBetween(earliestReaction.date, Date())
                        createDateRange(daysBetween)
                    } else {
                        createDateRange(7)
                    }
                }
            }
        }
        
        // Объединение данных
        val result = dateRange.map { date ->
            calendar.time = date
            resetTimeToMidnight(calendar)
            val dayTimestamp = calendar.timeInMillis
            FrequencyData(date, dateMap[dayTimestamp] ?: 0)
        }
        
        _frequencyData.value = result
    }
    
    private fun processSymptomsData(reactions: List<AllergyReaction>) {
        val symptomMap = mutableMapOf<String, Int>()
        
        // Подсчет симптомов
        reactions.forEach { reaction ->
            reaction.symptoms.forEach { symptom ->
                symptomMap[symptom] = (symptomMap[symptom] ?: 0) + 1
            }
        }
        
        // Сортировка по частоте и преобразование в список данных
        val result = symptomMap.entries
            .sortedByDescending { it.value }
            .take(10)  // Ограничиваем 10 наиболее частыми симптомами
            .map { SymptomData(it.key, it.value) }
        
        _symptomData.value = result
    }
    
    private fun processTriggersData(reactions: List<AllergyReaction>) {
        val triggerMap = mutableMapOf<String, Int>()
        
        // Подсчет триггеров
        reactions.forEach { reaction ->
            reaction.possibleTriggers.forEach { trigger ->
                triggerMap[trigger] = (triggerMap[trigger] ?: 0) + 1
            }
        }
        
        // Сортировка по частоте и преобразование в список данных
        val result = triggerMap.entries
            .sortedByDescending { it.value }
            .take(10)  // Ограничиваем 10 наиболее частыми триггерами
            .map { TriggerData(it.key, it.value) }
        
        _triggerData.value = result
    }
    
    private fun analyzeReactionTrends(reactions: List<AllergyReaction>) {
        // Если реакций меньше 5, не выполняем анализ тенденций
        if (reactions.size < 5) {
            _trendAnalysis.value = null
            return
        }
        
        // Сортируем реакции по дате
        val sortedReactions = reactions.sortedBy { it.date.time }
        
        // Находим общее количество
        val totalReactions = reactions.size
        
        // Находим все симптомы и их частоту
        val symptomsMap = mutableMapOf<String, Int>()
        reactions.forEach { reaction ->
            reaction.symptoms.forEach { symptom ->
                symptomsMap[symptom] = (symptomsMap[symptom] ?: 0) + 1
            }
        }
        val mostFrequentSymptom = symptomsMap.maxByOrNull { it.value }?.key ?: ""
        
        // Находим все триггеры и их частоту
        val triggersMap = mutableMapOf<String, Int>()
        reactions.forEach { reaction ->
            reaction.possibleTriggers.forEach { trigger ->
                triggersMap[trigger] = (triggersMap[trigger] ?: 0) + 1
            }
        }
        val mostFrequentTrigger = triggersMap.maxByOrNull { it.value }?.key ?: ""
        
        // Вычисляем средний показатель за неделю
        val oldestReactionDate = sortedReactions.first().date
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        val totalDays = (now - oldestReactionDate.time) / (1000 * 60 * 60 * 24)
        val totalWeeks = totalDays / 7f
        val averagePerWeek = if (totalWeeks > 0) totalReactions / totalWeeks else totalReactions.toFloat()
        
        // Вычисляем процент изменения, если данных достаточно
        var changePercent = 0f
        var isIncreasing = false
        
        if (totalReactions >= 10) {
            // Разделяем данные на две половины для сравнения
            val halfSize = totalReactions / 2
            val firstHalf = sortedReactions.take(halfSize)
            val secondHalf = sortedReactions.takeLast(halfSize)
            
            val firstHalfCount = firstHalf.size
            val secondHalfCount = secondHalf.size
            
            // Определяем количество дней в каждой половине
            val firstHalfDays = (secondHalf.first().date.time - firstHalf.first().date.time) / (1000 * 60 * 60 * 24)
            val secondHalfDays = (now - secondHalf.first().date.time) / (1000 * 60 * 60 * 24)
            
            val firstHalfRate = if (firstHalfDays > 0) firstHalfCount / firstHalfDays.toFloat() else 0f
            val secondHalfRate = if (secondHalfDays > 0) secondHalfCount / secondHalfDays.toFloat() else 0f
            
            if (firstHalfRate > 0) {
                changePercent = ((secondHalfRate - firstHalfRate) / firstHalfRate) * 100
                isIncreasing = secondHalfRate > firstHalfRate
            }
        }
        
        // Создаем объект анализа тенденций
        val trend = TrendAnalysis(
            totalReactions = totalReactions,
            averagePerWeek = averagePerWeek,
            changePercent = changePercent,
            mostFrequentSymptom = mostFrequentSymptom,
            mostFrequentTrigger = mostFrequentTrigger,
            isIncreasing = isIncreasing
        )
        
        _trendAnalysis.value = trend
    }
    
    private fun createDateRange(daysCount: Int): List<Date> {
        val result = mutableListOf<Date>()
        val calendar = Calendar.getInstance()
        
        // Начинаем с сегодняшнего дня и двигаемся назад
        for (i in 0 until daysCount) {
            resetTimeToMidnight(calendar)
            result.add(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        return result.reversed()  // Возвращаем в хронологическом порядке
    }
    
    private fun resetTimeToMidnight(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }
    
    private fun calculateDaysBetween(startDate: Date, endDate: Date): Int {
        val millisecondsPerDay = 24 * 60 * 60 * 1000L
        val diff = endDate.time - startDate.time
        return (diff / millisecondsPerDay).toInt() + 1  // +1, чтобы включить текущий день
    }
} 