package com.example.jenmix.jen1

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment
import com.github.mikephil.charting.components.Legend.LegendOrientation
import com.github.mikephil.charting.components.Legend.LegendVerticalAlignment
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.view.ViewTreeObserver
import com.example.jenmix.R
import kotlin.text.contains

class MainActivity1 : AppCompatActivity() {
    // View 元件
    private lateinit var api: ApiService
    private lateinit var userSpinner: AutoCompleteTextView
    private lateinit var dateSpinner: AutoCompleteTextView
    private lateinit var rangeSpinner: AutoCompleteTextView
    private lateinit var btnAnalyze: MaterialButton
    private lateinit var resultView: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var switchToggleView: Switch
    private lateinit var switchShowDetails: Switch
    private lateinit var chart: LineChart
    private var latestXLabels: List<String> = emptyList()
    private lateinit var diseaseSpinner: AutoCompleteTextView
    private var selectedDiseaseFilter: String? = null
    private var customStartDate: String? = null
    private var customEndDate: String? = null
    private var formattedDateLabels: List<String> = emptyList()
    private var lastSelectionType: SelectionType = SelectionType.NONE

    // 資料清單
    private var users: List<String> = emptyList()
    private val ranges = listOf("一週", "一個月", "半年", "一年", "總分析")
    private var selectedRange: String? = null

    enum class SelectionType {
        DATE_RANGE, ANALYSIS_RANGE, NONE
    }

    // 疾病與主要指標對應（用於圖表標記）
    private val diseaseSeriesMapping = mapOf(
        "高血壓" to listOf("systolic", "diastolic"),
        "血壓偏高" to listOf("systolic", "diastolic"),
        "低血壓" to listOf("systolic", "diastolic"),
        "脈搏太高" to listOf("pulse"),
        "高脈搏" to listOf("pulse"),
        "脈搏太低" to listOf("pulse"),
        "低脈搏" to listOf("pulse"),
    )

    // 疾病標記：顏色與說明
    private val diseaseMapping = mapOf(
        "高血壓" to Pair(Color.RED, "收縮壓≥140或舒張壓≥90"),
        "血壓偏高" to Pair(Color.parseColor("#FFA500"), "收縮壓120–130或舒張壓80"),
        "低血壓" to Pair(Color.BLUE, "收縮壓≤90或舒張壓≤60"),
        "脈搏太高" to Pair(Color.MAGENTA, "脈搏>120"),
        "高脈搏" to Pair(Color.YELLOW, "脈搏介於101-120"),
        "脈搏太低" to Pair(Color.CYAN, "脈搏<50"),
        "低脈搏" to Pair(Color.parseColor("#ADD8E6"), "脈搏介於50-59"),
    )

    // 當前使用者性別與身高（用於 MarkerView）
    private var currentGender: String = "male"
    private var currentHeightM: Float = 1.70f

    // -------------------------------
    // 健康評估常數
    // -------------------------------
    private companion object {
        // ✅ 收縮壓（Systolic）
        const val LOW_SYSTOLIC_BP = 90            // 低血壓：≤ 90
        const val NORMAL_SYSTOLIC_MIN = 90
        const val NORMAL_SYSTOLIC_MAX = 120
        const val BORDERLINE_SYSTOLIC_MIN = 120   // 偏高：120–130
        const val BORDERLINE_SYSTOLIC_MAX = 130
        const val PREHIGH_SYSTOLIC_MIN = 130      // 高血壓前期：130–140
        const val PREHIGH_SYSTOLIC_MAX = 140
        const val HIGH_SYSTOLIC_BP = 140          // 高血壓：≥ 140

        // ✅ 舒張壓（Diastolic）
        const val LOW_DIASTOLIC_BP = 60           // 低血壓：≤ 60
        const val NORMAL_DIASTOLIC_MIN = 60
        const val NORMAL_DIASTOLIC_MAX = 80
        const val BORDERLINE_DIASTOLIC = 80       // 偏高 / 前期：= 80
        const val PREHIGH_DIASTOLIC = 90          // 高血壓前期上限 / 高血壓下限
        const val HIGH_DIASTOLIC_BP = 90          // 高血壓：≥ 90

        const val HIGH_PULSE_RATE = 120
        const val HIGH_PULSE_LOWER_BOUND = 101
        const val LOW_PULSE_RATE = 50
        const val LOW_PULSE_UPPER_BOUND = 59
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_analysis)

        bindViews()
        setupRetrofit()
        setupSpinners()
        setupButtons()

        loadUsersFromServer()

        findViewById<Switch>(R.id.switch_expand_chart).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "🔍 放大中...", Toast.LENGTH_SHORT).show()
                showChartInDialog()
            }
        }
    }

    private fun bindViews() {
        userSpinner = findViewById(R.id.user_spinner)
        dateSpinner = findViewById(R.id.date_spinner)
        rangeSpinner = findViewById(R.id.range_spinner)
        diseaseSpinner = findViewById(R.id.disease_spinner)
        btnAnalyze = findViewById(R.id.analyze_btn)
        resultView = findViewById(R.id.result_view)
        loadingIndicator = findViewById(R.id.loading_indicator)
        switchToggleView = findViewById(R.id.switch_toggle_view)
        switchShowDetails = findViewById(R.id.switch_show_details)
        chart = findViewById(R.id.chart)
    }

    private fun setupRetrofit() {
        api = Retrofit.Builder()
            .baseUrl("http://192.168.0.10:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun loadUsersFromServer() {
        RetrofitClient.instance.getUsers().enqueue(object : Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                if (response.isSuccessful) {
                    users = response.body() ?: listOf()
                    val adapter = ArrayAdapter(
                        this@MainActivity1,
                        android.R.layout.simple_list_item_1,
                        users
                    )
                    userSpinner.setAdapter(adapter) // ✅ 設定到 AutoCompleteTextView（userSpinner）

                    userSpinner.setOnClickListener { userSpinner.showDropDown() }
                    userSpinner.setOnItemClickListener { _, _, _, _ ->
                        // 清空其他選項
                        dateSpinner.text = null
                        rangeSpinner.text = null
                        diseaseSpinner.text = null
                        selectedRange = null
                        selectedDiseaseFilter = null
                        resultView.text = ""
                        val selectedUser = userSpinner.text.toString().trim()
                        loadDatesForUser(selectedUser)
                    }

                } else {
                    Toast.makeText(this@MainActivity1, "載入使用者失敗", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                Toast.makeText(this@MainActivity1, "錯誤: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSpinners() {
        setupUserSpinner()
        setupDiseaseSpinner()
        setupDateSpinner()
        setupRangeSpinner()
    }

    private fun setupUserSpinner() {
        userSpinner.apply {
            setAdapter(ArrayAdapter(this@MainActivity1, android.R.layout.simple_list_item_1, users))
            setOnClickListener { showDropDown() }
            setOnItemClickListener { _, _, _, _ ->
                clearSelection()
                val selectedUser = text.toString().trim()
                loadDatesForUser(selectedUser)
                dateSpinner.visibility = View.VISIBLE
                rangeSpinner.visibility = View.VISIBLE
                diseaseSpinner.visibility = View.VISIBLE
            }
        }
    }

    private fun setupDiseaseSpinner() {
        val diseaseOptions = listOf("不篩選", "高血壓", "低血壓", "脈搏異常")
        diseaseSpinner.apply {
            setAdapter(
                ArrayAdapter(
                    this@MainActivity1,
                    android.R.layout.simple_list_item_1,
                    diseaseOptions
                )
            )
            setOnClickListener { showDropDown() }
            setOnItemClickListener { _, _, position, _ ->
                clearSelection(except = "disease")
                selectedDiseaseFilter = when (diseaseOptions[position]) {
                    "不篩選" -> null
                    "高血壓", "低血壓" -> "血壓"
                    "脈搏異常" -> "脈搏"
                    else -> null
                }
                getSelectedUserOrWarn()?.let { drawChartForRecords(it, selectedRangeToDays()) }
            }
        }
    }

    private fun setupDateSpinner() {
        dateSpinner.setOnClickListener { showDateRangeDialog() }
        dateSpinner.setOnItemClickListener { _, _, _, _ -> clearSelection(except = "date") }
    }

    private fun setupRangeSpinner() {
        rangeSpinner.apply {
            setAdapter(
                ArrayAdapter(
                    this@MainActivity1,
                    android.R.layout.simple_list_item_1,
                    ranges
                )
            )
            setOnClickListener { showDropDown() }
            setOnItemClickListener { _, _, position, _ ->
                clearSelection(except = "range")
                selectedRange = ranges[position]
                lastSelectionType = SelectionType.ANALYSIS_RANGE
            }
        }
    }

    private fun setupButtons() {
        btnAnalyze.setOnClickListener { performAnalysis() }

        switchShowDetails.setOnCheckedChangeListener { _, isChecked ->
            if (chart.visibility == View.VISIBLE) {
                getSelectedUserOrWarn()?.let {
                    drawChartForRecords(it, selectedRangeToDays())
                }
            }
        }

        switchToggleView.setOnCheckedChangeListener { _, isChecked ->
            resultView.visibility = if (isChecked) View.GONE else View.VISIBLE
            chart.visibility = if (isChecked) View.VISIBLE else View.GONE

            if (isChecked) {
                getSelectedUserOrWarn()?.let {
                    if (!customStartDate.isNullOrBlank() && !customEndDate.isNullOrBlank()) {
                        drawChartForRecords(it)
                    } else {
                        drawChartForRecords(it, selectedRangeToDays())
                    }
                }
            }
        }
    }

    private fun selectedRangeToDays(): Int? = when (selectedRange) {
        "一週" -> 7
        "一個月" -> 30
        "半年" -> 180
        "一年" -> 365
        else -> null
    }

    private fun clearSelection(except: String = "") {
        if (except != "date") dateSpinner.text = null
        if (except != "range") rangeSpinner.text = null
        if (except != "disease") diseaseSpinner.text = null
        if (except != "range") selectedRange = null
        if (except != "disease") selectedDiseaseFilter = null
        resultView.text = ""
    }

    // 🔽 performAnalysis 方法：邏輯統整化
    private fun performAnalysis() {
        val user = getSelectedUserOrWarn() ?: return

        when {
            // ✅ 自訂區間
            !customStartDate.isNullOrBlank() && !customEndDate.isNullOrBlank() -> {
                if (customStartDate == customEndDate) {
                    setLoading(true, "🔄 分析 ${customStartDate} 當日資料...")
                    api.getSingleAnalysis(user, customStartDate!!)
                        .enqueue(createAnalysisCallback(user))
                } else {
                    setLoading(true, "🔄 分析 ${customStartDate} 到 ${customEndDate} 的資料...")
                    api.getCustomRangeAnalysis(user, customStartDate!!, customEndDate!!)
                        .enqueue(createAnalysisCallback(user))
                }
            }
            // ✅ 選單日（無自訂）
            dateSpinner.text.toString().trim().matches(Regex("""\\d{4}-\\d{2}-\\d{2}""")) -> {
                val date = dateSpinner.text.toString().trim()
                selectedRange = null
                rangeSpinner.text = null
                setLoading(true, "🔄 分析單日資料...")
                api.getSingleAnalysis(user, date).enqueue(createAnalysisCallback(user))
            }
            // ✅ 選擇固定範圍
            !selectedRange.isNullOrBlank() -> {
                when (selectedRange) {
                    "總分析" -> {
                        setLoading(true, "🔄 正在分析所有資料...")
                        api.getAllAggregate(user).enqueue(createAnalysisCallback(user))
                    }

                    else -> analyzeRange(user, selectedRangeToDays() ?: 0)
                }
            }

            else -> {
                showToast("請先選擇日期或範圍")
            }
        }
    }


    // 🔽 createAnalysisCallback 提取重複處理邏輯、保持結構清晰
    private fun createAnalysisCallback(user: String): Callback<AnalysisResult> =
        object : Callback<AnalysisResult> {
            override fun onResponse(
                call: Call<AnalysisResult>,
                response: Response<AnalysisResult>
            ) {
                setLoading(false)
                if (response.isSuccessful) {
                    response.body()?.let { showResult(it) }
                        ?: run {
                            resultView.text = "❌ 查無分析資料"
                            Log.d("HealthAnalysis", "回傳空資料")
                        }
                } else {
                    resultView.text = "❌ 分析失敗"
                    Log.d("HealthAnalysis", "分析失敗，狀態碼：${response.code()}")
                }
            }

            override fun onFailure(call: Call<AnalysisResult>, t: Throwable) {
                setLoading(false)
                resultView.text = "❌ 錯誤：${t.message}"
                Log.d("HealthAnalysis", "API 呼叫失敗：${t.message}")
            }
        }


    // 🔽 showDateRangeDialog 保持功能不變，但加上註解與邏輯清晰
    private fun showDateRangeDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_date_range)

        val btnStart = dialog.findViewById<Button>(R.id.btnStartDate)
        val btnEnd = dialog.findViewById<Button>(R.id.btnEndDate)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        btnStart.setOnClickListener {
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    val date = Calendar.getInstance().apply { set(y, m, d) }
                    customStartDate = sdf.format(date.time)
                    btnStart.text = "開始：$customStartDate"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnEnd.setOnClickListener {
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    val date = Calendar.getInstance().apply { set(y, m, d) }
                    customEndDate = sdf.format(date.time)
                    btnEnd.text = "結束：$customEndDate"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnConfirm.setOnClickListener {
            if (customStartDate != null && customEndDate != null) {
                dateSpinner.setText("$customStartDate 到 $customEndDate", false)
                clearSelection(except = "date")
                lastSelectionType = SelectionType.DATE_RANGE
            } else {
                showToast("請選擇完整區間")
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showResult(res: AnalysisResult) {
        val icon = when (res.風險等級) {
            "高級" -> "🔴"
            "中級" -> "🟡"
            "初級" -> "🟢"
            else -> "⚪"
        }

        val dateToShow = when (lastSelectionType) {
            SelectionType.DATE_RANGE -> {
                if (!customStartDate.isNullOrBlank() && !customEndDate.isNullOrBlank()) {
                    if (customStartDate == customEndDate) customStartDate!!
                    else "$customStartDate 到 $customEndDate"
                } else {
                    res.record_date
                }
            }

            SelectionType.ANALYSIS_RANGE -> selectedRange ?: res.record_date
            SelectionType.NONE -> res.record_date
        }

        val genderText = when (res.gender?.lowercase()) {
            "male", "男" -> "男 ♂️"
            "female", "女" -> "女 ♀️"
            else -> "未知 ⚠️"
        }

        val ageText = res.age?.let { "$it 歲" } ?: "未知 ⚠️"

        resultView.text = buildString {
            append("👤 使用者：${res.user}\n")
            append("🎂 年齡　：$ageText\n")
            append("⚧️ 性別　：$genderText\n")
            append("🗓️ 日期：$dateToShow\n")
            append("📊 風險等級：$icon ${res.風險等級}\n\n")
            append("✅ 分析結果：\n")
            append(if (res.分析結果.isEmpty()) "．無\n\n" else res.分析結果.joinToString("\n") { "．$it" } + "\n\n")
            append("⚠️ 交叉風險：\n")
            append(if (res.交叉狀況.isEmpty()) "．無\n\n" else res.交叉狀況.joinToString("\n") { "．$it" } + "\n\n")
            append("📌 可能病症：\n")
            append(if (res.可能病症.isEmpty()) "．無\n\n" else res.可能病症.joinToString("\n") { "．$it" } + "\n\n")
            append("💡 建議：\n")
            append(if (res.建議.isEmpty()) "．無" else res.建議.joinToString("\n") { "．$it" })
        }
    }

    // 🔽 openSuggestionUrl 統一錯誤處理與提示風格
    private fun openSuggestionUrl(disease: String) {
        Toast.makeText(this, "📖 正在查詢 $disease 的建議...", Toast.LENGTH_SHORT).show()
        api.getSourceUrl(disease).enqueue(object : Callback<UrlResponse> {
            override fun onResponse(call: Call<UrlResponse>, response: Response<UrlResponse>) {
                val url = response.body()?.url
                if (!url.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity1, "✅ 已跳轉至建議網站", Toast.LENGTH_SHORT)
                        .show()
                    WebViewActivity.start(this@MainActivity1, url)
                } else {
                    showToast("❌ 查無建議網址")
                }
            }

            override fun onFailure(call: Call<UrlResponse>, t: Throwable) {
                showToast("❌ 連線失敗：${t.message}")
            }
        })
    }

    // ✅ 更新 drawChartForRecords 只顯示主線 + 可切換 LimitLine（不含疾病點）
    private fun drawChartForRecords(user: String, days: Int? = null) {
        chart.clear()
        api.getRecords(user).enqueue(object : Callback<List<HealthRecord>> {
            override fun onResponse(
                call: Call<List<HealthRecord>>,
                response: Response<List<HealthRecord>>
            ) {
                val records = response.body().orEmpty()
                if (records.isEmpty()) {
                    showToast("無記錄資料")
                    Log.d("HealthAnalysis", "記錄資料為空")
                    return
                }

                val inputFormats = listOf(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                    SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
                    SimpleDateFormat("yyyy/M/d", Locale.getDefault()),
                    SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
                )
                val outputFormat = SimpleDateFormat("M/d", Locale.getDefault())

                val parsedRecords = records.mapNotNull { record ->
                    val parsedDate = inputFormats.firstNotNullOfOrNull { fmt ->
                        runCatching { fmt.parse(record.measure_at) }.getOrNull()
                    }
                    parsedDate?.let { Pair(record, it) }
                }.sortedBy { it.second }

                val sortedRecords = parsedRecords.map { it.first }
                formattedDateLabels = parsedRecords.map { outputFormat.format(it.second) }

                val latestDate = parsedRecords.maxByOrNull { it.second }?.second
                val filteredRecords = when {
                    days != null && latestDate != null -> {
                        val cutoff = Calendar.getInstance().apply {
                            time = latestDate
                            add(Calendar.DAY_OF_YEAR, -days)
                        }.time
                        parsedRecords.filter { it.second.after(cutoff) }.map { it.first }
                    }

                    !customStartDate.isNullOrBlank() && !customEndDate.isNullOrBlank() -> {
                        val start = inputFormats[0].parse(customStartDate!!)!!
                        val end = inputFormats[0].parse(customEndDate!!)!!
                        parsedRecords.filter { it.second in start..end }.map { it.first }
                    }

                    else -> sortedRecords
                }

                if (filteredRecords.isEmpty()) {
                    showToast("符合條件的資料為空")
                    chart.clear()
                    return
                }

                val systolicEntries = mutableListOf<Entry>()
                val diastolicEntries = mutableListOf<Entry>()
                val pulseEntries = mutableListOf<Entry>()
                var gender = "male"
                var height = 1.70f

                filteredRecords.forEachIndexed { index, rec ->
                    val x = index.toFloat()
                    val fixed = rec.copy(user = user)
                    systolicEntries.add(Entry(x, rec.systolic_mmHg.toFloat()).apply {
                        data = fixed
                    })
                    diastolicEntries.add(Entry(x, rec.diastolic_mmHg.toFloat()).apply {
                        data = fixed
                    })
                    pulseEntries.add(Entry(x, rec.pulse_bpm.toFloat()).apply { data = fixed })
                    gender = rec.gender
                    height = rec.height.toFloat() / 100f
                }

                val dataSets = mutableListOf<ILineDataSet>()
                if (selectedDiseaseFilter == null || selectedDiseaseFilter == "血壓") {
                    dataSets.add(createDataSet(systolicEntries, "收縮壓", Color.RED, Color.RED))
                    dataSets.add(createDataSet(diastolicEntries, "舒張壓", Color.BLUE, Color.BLUE))
                }
                if (selectedDiseaseFilter == null || selectedDiseaseFilter == "脈搏") {
                    dataSets.add(createDataSet(pulseEntries, "脈搏", Color.MAGENTA, Color.MAGENTA))
                }

                chart.data = LineData(dataSets)

                val entryCount = chart.data.entryCount
                val showCount = if (entryCount >= 10) 10f else entryCount.toFloat()
                chart.setVisibleXRangeMaximum(showCount)
                chart.moveViewToX(entryCount - showCount)

                // ✅ 動態產生單位
                val unitSet = mutableSetOf<String>()
                chart.data.dataSets.forEach { dataSet ->
                    when {
                        dataSet.label.contains("收縮壓") || dataSet.label.contains("舒張壓") -> unitSet.add("mmHg")
                        dataSet.label.contains("脈搏") -> unitSet.add("bpm")
                    }
                }
                chart.post {
                    chart.description.isEnabled = true
                    chart.description.text = "單位：${unitSet.joinToString(" / ")}"
                    chart.description.textSize = 14f
                    chart.description.textColor = Color.DKGRAY
                    chart.description.setPosition(400f, 60f) // ✅ 進來一點避免被擠出
                    chart.invalidate() // 讓圖表立即刷新畫面
                }

                chart.xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(formattedDateLabels)
                    labelCount = formattedDateLabels.size
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textSize = 14.5f
                    textColor = Color.DKGRAY
                }
                chart.axisLeft.textSize = 14.5f
                chart.axisRight.textSize = 14.5f
                chart.legend.apply {
                    textSize = 14.5f
                    formSize = 14.5f
                    xEntrySpace = 20f
                    yEntrySpace = 20f
                    isWordWrapEnabled = true
                    maxSizePercent = 0.7f
                }
                chart.axisLeft.axisMaximum = chart.yMax + 10f  // ✅ 給上方多一點空間
                chart.setExtraOffsets(0f, 30f, 0f, 20f)         // ✅ 避免內容太擠
                chart.xAxis.setLabelCount(5, true)              // ✅ 避免日期太多壓在一起

                configureChart(chart)
                chart.marker = MyMarkerView(
                    this@MainActivity1,
                    R.layout.custom_marker_view,
                    chart,
                    gender,
                    height
                )

                val abnormalIndicators =
                    setOf("systolic", "diastolic", "pulse").filterByDisease().toSet()
                if (switchShowDetails.isChecked) {
                    addLimitLinesToChart(chart, abnormalIndicators)
                } else {
                    chart.axisLeft.removeAllLimitLines()
                }

                chart.invalidate()
            }

            override fun onFailure(call: Call<List<HealthRecord>>, t: Throwable) {
                showToast("圖表資料載入失敗：${t.message}")
                Log.d("HealthAnalysis", "getRecords 失敗：${t.message}")
            }
        })
    }

    private fun addLimitLinesToChart(targetChart: LineChart, abnormalIndicators: Set<String>) {
        targetChart.axisLeft.removeAllLimitLines()

        fun createLimitLine(value: Float, label: String, color: Int): LimitLine {
            return LimitLine(value, label).apply {
                lineColor = color
                lineWidth = 1.5f
                textColor = color
                textSize = 15f
                enableDashedLine(8f, 6f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP // ✅ 顯示在右上
                // ✅ 這一行加大 Y 偏移，讓標籤往下移動一點
                yOffset = 30f  // ← 原本你應該只有 12f，建議拉到 30f ~ 40f 之間
                xOffset = 5f
            }
        }

        // ✅ 血壓
        if ("systolic" in abnormalIndicators || "diastolic" in abnormalIndicators) {
            // 高血壓
            if ("高血壓" in diseaseMapping) {
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        140f,
                        "高血壓（危險）：收縮壓≥140",
                        Color.RED
                    )
                )
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        90f,
                        "高血壓（危險）：舒張壓≥90",
                        Color.RED
                    )
                )
            }
            if ("高血壓" in diseaseMapping) {
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        130f,
                        "高血壓（前期）：收縮壓130–140",
                        Color.parseColor("#B22222")
                    ) // 深紅
                )
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(80f, "高血壓（前期）：舒張壓80–90", Color.parseColor("#B22222"))
                )
            }
            // 血壓偏高
            if ("血壓偏高" in diseaseMapping) {
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        120f,
                        "血壓偏高：收縮壓120–130",
                        Color.parseColor("#FFA500")
                    )
                )
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        80f,
                        "血壓偏高：舒張壓=80",
                        Color.parseColor("#FFA500")
                    )
                )
            }
            // 低血壓
            if ("低血壓" in diseaseMapping) {
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        90f,
                        "低血壓：收縮壓≤90",
                        Color.BLUE
                    )
                )
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        60f,
                        "低血壓：舒張壓≤60",
                        Color.BLUE
                    )
                )
            }
        }

        // ✅ 脈搏
        if ("pulse" in abnormalIndicators) {
            if ("脈搏太高" in diseaseMapping)
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        120.2f,
                        "脈搏太高：>120",
                        Color.MAGENTA
                    )
                )
            if ("高脈搏" in diseaseMapping)
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        101f,
                        "高脈搏：101–120",
                        Color.parseColor("#FFA000")  // 深黃色
                    )
                )
            if ("脈搏太低" in diseaseMapping)
                targetChart.axisLeft.addLimitLine(createLimitLine(50f, "脈搏太低：<50", Color.CYAN))
            if ("低脈搏" in diseaseMapping)
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        59f,
                        "低脈搏：50–59",
                        Color.parseColor("#1565C0")  // 深藍色
                    )
                )
        }
    }

    // 🔽 configureChart：統一圖表樣式設定
    private fun configureChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setDrawMarkers(true)
            setPinchZoom(true)
            setDoubleTapToZoomEnabled(false)
            setScaleYEnabled(false)

            // ✅ 增加上下 padding，避免擠壓
            setExtraOffsets(0f, 30f, 0f, 20f)

            axisLeft.apply {
                textSize = 14.5f
                textColor = Color.DKGRAY
                axisLineColor = Color.DKGRAY
                gridColor = Color.LTGRAY
                axisMaximum = axisMaximum + 10f  // ✅ 增加 Y 軸最大值避免上面擠壓
            }
            axisRight.apply {
                isEnabled = true
                textSize = 14.5f
                textColor = Color.DKGRAY
                axisLineColor = Color.DKGRAY
                gridColor = Color.TRANSPARENT
            }
            legend.apply {
                verticalAlignment = LegendVerticalAlignment.TOP
                horizontalAlignment = LegendHorizontalAlignment.CENTER
                orientation = LegendOrientation.HORIZONTAL
                setDrawInside(false)
                isWordWrapEnabled = true
                maxSizePercent = 0.7f
                xEntrySpace = 16f
                yEntrySpace = 12f
                textSize = 12f
                formSize = 12f
            }
            // ✅ 避免 X 軸日期擠太多
            xAxis.setLabelCount(5, true)
            setExtraOffsets(10f, 10f, 10f, 20f)
        }
    }

    // 🔽 createDataSet：統一繪圖屬性與格式化
    private fun createDataSet(
        entries: List<Entry>,
        label: String,
        lineColor: Int,
        circleColor: Int,
        lineWidth: Float = 3f,
        circleRadius: Float = 4f,
        valueTextSize: Float = 12f,
        drawValues: Boolean = true,
        unitLabel: String = ""
    ): LineDataSet {
        return LineDataSet(entries, label).apply {
            color = lineColor
            setCircleColor(circleColor)
            this.lineWidth = lineWidth
            this.circleRadius = circleRadius
            setValueTextSize(valueTextSize)
            setDrawValues(drawValues)
            mode = LineDataSet.Mode.LINEAR

            if (label in listOf("收縮壓", "舒張壓", "脈搏")) {
                setDrawCircles(true)                  // ✅ 畫圓點
                setCircleRadius(6f)                  // ✅ 改這裡！讓點變大
                setCircleColor(lineColor)            // ✅ 主線顏色
                setDrawValues(true)                  // ✅ 顯示數值
                setValueTextSize(14f)                 // ✅ 字體大小
                setValueTextColor(Color.BLACK)        // ✅ 字體顏色
                mode = LineDataSet.Mode.LINEAR       // ✅ 線條模式
                this.lineWidth = 3f
            }


            // ✅ 單位格式化（可保留）
            if (unitLabel.isNotBlank()) {
                valueFormatter = object : ValueFormatter() {
                    override fun getPointLabel(entry: Entry?): String {
                        return entry?.y?.toInt()?.toString() ?: ""
                    }
                }
            }
        }
    }

    // 分析記錄中的異常指標
    private fun analyzeRecord(record: HealthRecord): List<String> = listOfNotNull(
        getBloodPressureAnalysis(record),
        getPulseAnalysis(record),
    )

    private fun getBloodPressureAnalysis(record: HealthRecord): String? = when {
        record.systolic_mmHg >= HIGH_SYSTOLIC_BP || record.diastolic_mmHg >= HIGH_DIASTOLIC_BP -> "高血壓"
        record.systolic_mmHg in PREHIGH_SYSTOLIC_MIN until HIGH_SYSTOLIC_BP || record.diastolic_mmHg in BORDERLINE_DIASTOLIC until HIGH_DIASTOLIC_BP -> "高血壓"
        record.systolic_mmHg in BORDERLINE_SYSTOLIC_MIN until PREHIGH_SYSTOLIC_MIN || record.diastolic_mmHg == BORDERLINE_DIASTOLIC -> "血壓偏高"
        record.systolic_mmHg < LOW_SYSTOLIC_BP || record.diastolic_mmHg < LOW_DIASTOLIC_BP -> "低血壓"
        else -> null
    }


    private fun getPulseAnalysis(record: HealthRecord): String? = when {
        record.pulse_bpm > HIGH_PULSE_RATE -> "脈搏太高"
        record.pulse_bpm in HIGH_PULSE_LOWER_BOUND..HIGH_PULSE_RATE -> "高脈搏"
        record.pulse_bpm < LOW_PULSE_RATE -> "脈搏太低"
        record.pulse_bpm in LOW_PULSE_RATE..LOW_PULSE_UPPER_BOUND -> "低脈搏"
        else -> null
    }

    private fun loadDatesForUser(user: String) {
        api.getAvailableDates(user).enqueue(object : Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                response.body()?.let { dates ->
                    dateSpinner.setAdapter(
                        ArrayAdapter(
                            this@MainActivity1,
                            android.R.layout.simple_list_item_1,
                            dates
                        )
                    )
                    dateSpinner.text = null
                }
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                showToast("❌ 載入日期失敗：${t.message}")
                Log.d("HealthAnalysis", "getAvailableDates 失敗：${t.message}")
            }
        })
    }

    private fun analyzeRange(user: String, days: Int) {
        val startDate = getDatabasePastDate(days) ?: run {
            analyzeAllData(user)
            return
        }
        setLoading(true, "🔄 分析最近 $days 天資料中...")
        api.getRangeAnalysis(user, startDate).enqueue(createAnalysisCallback(user))
    }

    private fun analyzeAllData(user: String) {
        setLoading(true, "🔄 正在分析所有資料...")
        api.getAllAggregate(user).enqueue(createAnalysisCallback(user))
    }

    private fun getDatabasePastDate(days: Int): String? {
        val adapter = dateSpinner.adapter ?: return null
        return if (adapter.count > 0) {
            val latestDateStr = adapter.getItem(0).toString()
            runCatching {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val latestDate = sdf.parse(latestDateStr)
                Calendar.getInstance().apply {
                    time = latestDate ?: return null
                    add(Calendar.DAY_OF_YEAR, -days)
                }.let { sdf.format(it.time) }
            }.getOrNull()
        } else null
    }

    private fun getSelectedUserOrWarn(): String? {
        val user = userSpinner.text.toString().trim()
        return if (user.isEmpty()) {
            showToast("⚠️ 請先選擇使用者")
            null
        } else user
    }

    private fun showChartInDialog() {
        if (chart.data == null || chart.data.dataSetCount == 0) {
            showToast("目前沒有可用的圖表資料")
            return
        }

        Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(R.layout.dialog_chart_fullscreen)
            val fullChart = findViewById<LineChart>(R.id.dialog_chart)
            val closeBtn = findViewById<ImageButton>(R.id.btnCloseDialog)

            // ✅ 複製主圖資料並過濾：只保留主線 (收縮壓、舒張壓、脈搏)
            val filteredDataSets = chart.data.dataSets.filter {
                it.label in listOf("收縮壓", "舒張壓", "脈搏")
            }
            fullChart.data = LineData(filteredDataSets)

            // ✅ 顯示最多 10 筆並自動滾到最新
            val entryCount = fullChart.data.entryCount
            val showCount = if (entryCount >= 10) 10f else entryCount.toFloat()
            fullChart.setVisibleXRangeMaximum(showCount)
            fullChart.moveViewToX(entryCount - showCount)

            // ✅ 補上單位描述
            val unitSet = mutableSetOf<String>()
            fullChart.data.dataSets.forEach { dataSet ->
                when {
                    dataSet.label.contains("收縮壓") || dataSet.label.contains("舒張壓") -> unitSet.add(
                        "mmHg"
                    )

                    dataSet.label.contains("脈搏") -> unitSet.add("bpm")
                }
            }
            fullChart.post {
                fullChart.description.isEnabled = true
                fullChart.description.text = "單位：${unitSet.joinToString(" / ")}"
                fullChart.description.textSize = 16f
                fullChart.description.textColor = Color.DKGRAY
                fullChart.description.setPosition(
                    450f,
                    fullChart.viewPortHandler.contentTop() - 40f
                )
                fullChart.invalidate()
            }

            // ✅ 套用單位格式化器
            fullChart.data.dataSets.forEach { dataSet ->
                val unit = when {
                    dataSet.label.contains("脈搏") -> "bpm"
                    dataSet.label.contains("收縮壓") || dataSet.label.contains("舒張壓") -> "mmHg"
                    else -> ""
                }
                if (unit.isNotEmpty()) {
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getPointLabel(entry: Entry?): String {
                            return entry?.y?.toInt()?.toString() ?: ""
                        }
                    }
                }
            }

            // ✅ 加入 LimitLine（根據 switch）
            val abnormalIndicators = mutableSetOf<String>().apply {
                if (filteredDataSets.any { it.label.contains("收縮壓") }) add("systolic")
                if (filteredDataSets.any { it.label.contains("舒張壓") }) add("diastolic")
                if (filteredDataSets.any { it.label.contains("脈搏") }) add("pulse")
            }.filterByDisease().toSet()

            if (switchShowDetails.isChecked) {
                addLimitLinesToChart(fullChart, abnormalIndicators)
            } else {
                fullChart.axisLeft.removeAllLimitLines()
            }

            // ✅ 樣式設置與 MarkerView
            configureChart(fullChart)
            fullChart.marker = MyMarkerView(
                this@MainActivity1,
                R.layout.custom_marker_view,
                fullChart,
                currentGender,
                currentHeightM
            )

            // ✅ 顯示使用者資訊
            val infoText = findViewById<TextView>(R.id.user_info_text)
            val record =
                chart.data?.dataSets?.firstOrNull()?.getEntryForIndex(0)?.data as? HealthRecord
            val genderText = when (record?.gender?.lowercase()) {
                "male", "男" -> "男"
                "female", "女" -> "女"
                else -> "⚠️未知"
            }
            val ageText = record?.age?.let { "$it 歲" } ?: "未知"
            infoText.text =
                "👤 ${record?.user}｜⚧️ 性別：$genderText｜🎂 年齡：$ageText｜📏 身高：${record?.height?.toInt()}cm｜⚖️ 體重：${record?.weight}kg｜"

            // ✅ X 軸設定
            fullChart.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(formattedDateLabels)
                setDrawLabels(true)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelRotationAngle = 0f
                textSize = 16.5f
                textColor = Color.DKGRAY
                setLabelCount(formattedDateLabels.size, true)
                gridColor = Color.LTGRAY
                gridLineWidth = 1.2f
                axisLineColor = Color.DKGRAY
                axisLineWidth = 1.5f
            }

            // ✅ Y 軸與圖例
            fullChart.axisLeft.textSize = 16.5f
            fullChart.axisRight.textSize = 16.5f
            fullChart.legend.apply {
                textSize = 16.5f
                formSize = 16.5f
                xEntrySpace = 12f
                yEntrySpace = 12f
                verticalAlignment = LegendVerticalAlignment.TOP
                horizontalAlignment = LegendHorizontalAlignment.CENTER
                orientation = LegendOrientation.HORIZONTAL
                setDrawInside(false)
                yOffset = 30f
            }
            fullChart.setExtraOffsets(0f, 24f, 0f, 30f)

            // ✅ 點選資料後開啟建議（保留互動）
            fullChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e == null || h == null || fullChart.data == null) return
                    val record = e.data as? HealthRecord ?: return
                    val label = fullChart.data.getDataSetByIndex(h.dataSetIndex).label ?: return
                    val disease = when (label) {
                        "收縮壓", "舒張壓" -> getBloodPressureAnalysis(record)
                        "脈搏" -> getPulseAnalysis(record)
                        else -> null
                    } ?: return

                    if (disease != "正常值" && disease != "未知") {
                        Toast.makeText(
                            this@MainActivity1,
                            "📖 正在開啟：$disease 建議...",
                            Toast.LENGTH_SHORT
                        ).show()
                        openSuggestionUrl(disease)
                    }
                }

                override fun onNothingSelected() {
                    fullChart.highlightValue(null)
                }
            })

            // ✅ 手勢控制（可選）
            fullChart.setOnChartGestureListener(object : OnChartGestureListener {
                override fun onChartGestureStart(
                    me: MotionEvent?,
                    lastPerformedGesture: ChartTouchListener.ChartGesture?
                ) {
                }

                override fun onChartGestureEnd(
                    me: MotionEvent?,
                    lastPerformedGesture: ChartTouchListener.ChartGesture?
                ) {
                }

                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartSingleTapped(me: MotionEvent?) {}
                override fun onChartFling(
                    me1: MotionEvent?,
                    me2: MotionEvent?,
                    velocityX: Float,
                    velocityY: Float
                ) {
                }

                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
            })

            closeBtn.setOnClickListener {
                (this@MainActivity1.findViewById<Switch>(R.id.switch_expand_chart)).isChecked = false
                dismiss()  // ✅ 最後再關掉 Dialog
            }
            show()  // ✅ 最後呼叫 show()
        }
    }

    private fun setLoading(isLoading: Boolean, message: String = "") {
        btnAnalyze.isEnabled = !isLoading
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (message.isNotBlank()) resultView.text = message
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun Set<String>.filterByDisease(): Set<String> {
        return when (selectedDiseaseFilter) {
            "血壓" -> filter { it == "systolic" || it == "diastolic" }.toSet()
            "脈搏" -> filter { it == "pulse" }.toSet()
            else -> this // 不篩選：保留全部
        }
    }
}
