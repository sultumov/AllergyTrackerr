package com.example.myapplication.ui.statistics

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsFragment : Fragment() {

    private lateinit var viewModel: StatisticsViewModel
    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var radioGroup: RadioGroup
    private lateinit var exportButton: Button
    private lateinit var trendAnalysisText: TextView
    
    // Хранение данных для форматтера
    private var frequencyDataList: List<StatisticsViewModel.FrequencyData> = emptyList()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportStatsToPdf()
        } else {
            Toast.makeText(requireContext(), "Разрешение на запись необходимо для экспорта PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_statistics, container, false)

        // Инициализация элементов интерфейса
        lineChart = root.findViewById(R.id.frequency_chart)
        pieChart = root.findViewById(R.id.symptoms_chart)
        barChart = root.findViewById(R.id.triggers_chart)
        radioGroup = root.findViewById(R.id.period_radio_group)
        exportButton = root.findViewById(R.id.button_export)
        trendAnalysisText = root.findViewById(R.id.text_trend_analysis)

        setupCharts()
        
        exportButton.setOnClickListener {
            checkPermissionAndExport()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[StatisticsViewModel::class.java]

        // Настройка обработчика выбора периода
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val period = when (checkedId) {
                R.id.radio_week -> StatisticsViewModel.Period.WEEK
                R.id.radio_month -> StatisticsViewModel.Period.MONTH
                R.id.radio_all -> StatisticsViewModel.Period.ALL
                else -> StatisticsViewModel.Period.WEEK
            }
            viewModel.loadStatistics(period)
        }

        // Наблюдение за изменениями данных
        viewModel.frequencyData.observe(viewLifecycleOwner) { data ->
            frequencyDataList = data
            updateFrequencyChart(data)
        }

        viewModel.symptomData.observe(viewLifecycleOwner) { data ->
            updateSymptomsChart(data)
        }

        viewModel.triggerData.observe(viewLifecycleOwner) { data ->
            updateTriggersChart(data)
        }
        
        viewModel.trendAnalysis.observe(viewLifecycleOwner) { analysis ->
            updateTrendAnalysis(analysis)
        }

        // Загрузка данных за неделю по умолчанию
        viewModel.loadStatistics(StatisticsViewModel.Period.WEEK)
    }
    
    private fun updateTrendAnalysis(analysis: StatisticsViewModel.TrendAnalysis?) {
        if (analysis == null) {
            trendAnalysisText.visibility = View.GONE
            return
        }
        
        // Формируем текст анализа
        val trendText = StringBuilder()
        trendText.append("Всего зафиксировано: ${analysis.totalReactions} реакций\n")
        trendText.append("Среднее в неделю: %.1f\n".format(analysis.averagePerWeek))
        
        if (analysis.changePercent != 0f) {
            val trendDirection = if (analysis.isIncreasing) "увеличение" else "снижение"
            trendText.append("Тенденция: $trendDirection на %.1f%%\n".format(Math.abs(analysis.changePercent)))
        }
        
        if (analysis.mostFrequentSymptom.isNotEmpty()) {
            trendText.append("Самый частый симптом: ${analysis.mostFrequentSymptom}\n")
        }
        
        if (analysis.mostFrequentTrigger.isNotEmpty() && analysis.mostFrequentTrigger != "Неизвестно") {
            trendText.append("Самый частый триггер: ${analysis.mostFrequentTrigger}")
        }
        
        trendAnalysisText.text = trendText.toString()
        trendAnalysisText.visibility = View.VISIBLE
    }
    
    private fun checkPermissionAndExport() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), writePermission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(writePermission)
            } else {
                exportStatsToPdf()
            }
        } else {
            // Android 10+ использует scoped storage, разрешения не требуются
            exportStatsToPdf()
        }
    }
    
    private fun exportStatsToPdf() {
        try {
            // Создаем документ PDF
            val document = Document()
            
            // Определяем выходной поток в зависимости от версии Android
            val outputStream: OutputStream
            val fileName = "Аллергия_статистика_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = requireContext().contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: throw Exception("Не удалось создать файл")
                
                outputStream = requireContext().contentResolver.openOutputStream(uri)
                    ?: throw Exception("Не удалось открыть поток")
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
            }
            
            // Создаем писателя PDF
            PdfWriter.getInstance(document, outputStream)
            document.open()
            
            // Добавляем заголовок
            document.add(Paragraph("Статистика аллергических реакций"))
            document.add(Paragraph("Дата создания: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())}"))
            document.add(Paragraph(" "))
            
            // Добавляем данные анализа тенденций
            viewModel.trendAnalysis.value?.let { analysis ->
                document.add(Paragraph("Анализ тенденций:"))
                document.add(Paragraph("Всего зафиксировано: ${analysis.totalReactions} реакций"))
                document.add(Paragraph("Среднее в неделю: %.1f".format(analysis.averagePerWeek)))
                
                if (analysis.changePercent != 0f) {
                    val trendDirection = if (analysis.isIncreasing) "увеличение" else "снижение"
                    document.add(Paragraph("Тенденция: $trendDirection на %.1f%%".format(Math.abs(analysis.changePercent))))
                }
                
                if (analysis.mostFrequentSymptom.isNotEmpty()) {
                    document.add(Paragraph("Самый частый симптом: ${analysis.mostFrequentSymptom}"))
                }
                
                if (analysis.mostFrequentTrigger.isNotEmpty() && analysis.mostFrequentTrigger != "Неизвестно") {
                    document.add(Paragraph("Самый частый триггер: ${analysis.mostFrequentTrigger}"))
                }
                
                document.add(Paragraph(" "))
            }
            
            // Добавляем графики
            document.add(Paragraph("Графики:"))
            
            // График частоты
            if (lineChart.data != null && lineChart.data.dataSetCount > 0) {
                document.add(Paragraph("Частота реакций:"))
                val lineChartBitmap = lineChart.chartBitmap
                val lineChartImage = Image.getInstance(bitmapToByteArray(lineChartBitmap))
                lineChartImage.scaleToFit(500f, 250f)
                document.add(lineChartImage)
                document.add(Paragraph(" "))
            }
            
            // График симптомов
            if (pieChart.data != null && pieChart.data.dataSetCount > 0) {
                document.add(Paragraph("Распределение симптомов:"))
                val pieChartBitmap = pieChart.chartBitmap
                val pieChartImage = Image.getInstance(bitmapToByteArray(pieChartBitmap))
                pieChartImage.scaleToFit(500f, 250f)
                document.add(pieChartImage)
                document.add(Paragraph(" "))
            }
            
            // График триггеров
            if (barChart.data != null && barChart.data.dataSetCount > 0) {
                document.add(Paragraph("Частые триггеры:"))
                val barChartBitmap = barChart.chartBitmap
                val barChartImage = Image.getInstance(bitmapToByteArray(barChartBitmap))
                barChartImage.scaleToFit(500f, 250f)
                document.add(barChartImage)
            }
            
            document.close()
            outputStream.close()
            
            Toast.makeText(requireContext(), "Отчет сохранен в папке Загрузки", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка при создании отчета: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun setupCharts() {
        // Настройка графика частоты
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setPinchZoom(true)
            
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = object : IndexAxisValueFormatter() {
                private val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
                
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    if (index >= 0 && index < frequencyDataList.size) {
                        val date = frequencyDataList[index].date
                        return dateFormat.format(date)
                    }
                    return value.toString()
                }
            }
        }

        // Настройка графика симптомов
        pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            setCenterText("Симптомы")
            setRotationAngle(0f)
            setRotationEnabled(true)
            setEntryLabelColor(Color.BLACK)
            animateY(1400, Easing.EaseInOutQuad)
            legend.isEnabled = true
        }

        // Настройка графика триггеров
        barChart.apply {
            description.isEnabled = false
            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawGridBackground(false)
            
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false
            
            legend.isEnabled = true
            setTouchEnabled(true)
            animateY(1400, Easing.EaseInOutQuad)
        }
    }

    private fun updateFrequencyChart(data: List<StatisticsViewModel.FrequencyData>) {
        if (data.isEmpty()) {
            lineChart.setNoDataText("Нет данных о реакциях")
            lineChart.invalidate()
            return
        }

        val entries = data.mapIndexed { index, frequencyData ->
            val entry = Entry(index.toFloat(), frequencyData.count.toFloat())
            entry.data = frequencyData // Сохраняем дату для форматтера
            entry
        }

        val dataSet = LineDataSet(entries, "Частота реакций").apply {
            color = resources.getColor(R.color.purple_500, null)
            lineWidth = 2f
            setCircleColor(resources.getColor(R.color.purple_500, null))
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
            setDrawFilled(true)
            fillColor = resources.getColor(R.color.purple_200, null)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.animateX(1500)
        lineChart.invalidate()
    }

    private fun updateSymptomsChart(data: List<StatisticsViewModel.SymptomData>) {
        if (data.isEmpty()) {
            pieChart.setNoDataText("Нет данных о симптомах")
            pieChart.invalidate()
            return
        }

        val entries = data.map { PieEntry(it.count.toFloat(), it.name) }
        val dataSet = PieDataSet(entries, "Симптомы").apply {
            setColors(*ColorTemplate.COLORFUL_COLORS)
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            sliceSpace = 3f
            selectionShift = 5f
        }

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.invalidate()
    }

    private fun updateTriggersChart(data: List<StatisticsViewModel.TriggerData>) {
        if (data.isEmpty()) {
            barChart.setNoDataText("Нет данных о триггерах")
            barChart.invalidate()
            return
        }

        val entries = data.mapIndexed { index, triggerData ->
            BarEntry(index.toFloat(), triggerData.count.toFloat())
        }

        val dataSet = BarDataSet(entries, "Частые триггеры").apply {
            setColors(*ColorTemplate.MATERIAL_COLORS)
            valueTextSize = 10f
        }

        val labels = data.map { it.name }
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelRotationAngle = 45f

        val barData = BarData(dataSet)
        barChart.data = barData
        barChart.setFitBars(true)
        barChart.invalidate()
    }
} 