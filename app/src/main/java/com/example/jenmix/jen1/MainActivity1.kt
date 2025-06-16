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
        // 初始化元件及設定邏輯
        bindViews()
        setupRetrofit()
        setupSpinners()
        setupButtons()

        loadUsersFromServer()

        val expandSwitch = findViewById<Switch>(R.id.switch_expand_chart)
        expandSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "🔍 放大中...", Toast.LENGTH_SHORT).show()
                showChartInDialog()
                expandSwitch.isChecked = false
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
            .baseUrl("http://10.11.246.191:3000/") // 請根據實際狀況調整
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
        // 使用者選單設定
        userSpinner.apply {
            setAdapter(
                ArrayAdapter(
                    this@MainActivity1,
                    android.R.layout.simple_list_item_1,
                    users
                )
            )
            setOnClickListener { showDropDown() }
            setOnItemClickListener { _, _, _, _ ->
                // 清空舊資料
                dateSpinner.text = null
                rangeSpinner.text = null
                diseaseSpinner.text = null
                selectedRange = null
                selectedDiseaseFilter = null
                resultView.text = ""
                // 載入該使用者可分析的日期
                val selectedUser = text.toString().trim()
                loadDatesForUser(selectedUser)
                // 顯示日期與範圍選單
                dateSpinner.visibility = View.VISIBLE
                rangeSpinner.visibility = View.VISIBLE
                diseaseSpinner.visibility = View.VISIBLE

            }
        }

            val diseaseOptions = listOf("不篩選", "高血壓", "低血壓", "脈搏異常",)
            diseaseSpinner.apply {
                setAdapter(ArrayAdapter(this@MainActivity1, android.R.layout.simple_list_item_1, diseaseOptions))
                setOnClickListener { showDropDown() }
                setOnItemClickListener { _, _, position, _ ->
                    // ✅ 選疾病時清空日期與範圍
                    dateSpinner.text = null
                    rangeSpinner.text = null
                    selectedRange = null
                    selectedDiseaseFilter = when (diseaseOptions[position]) {
                        "不篩選" -> null
                        "高血壓", "低血壓" -> "血壓"
                        "脈搏異常" -> "脈搏"
                        else -> null
                    }
                    val user = getSelectedUserOrWarn() ?: return@setOnItemClickListener
                    val days = when (selectedRange) {
                        "一週" -> 7
                        "一個月" -> 30
                        "半年" -> 180
                        "一年" -> 365
                        else -> null
                    }
                    drawChartForRecords(user, days)
                }
            }

        // 日期選擇
        dateSpinner.apply {
            setOnClickListener { showDateRangeDialog()  }
            setOnItemClickListener { _, _, _, _ ->
                // ✅ 選日期時清空範圍與疾病篩選
                rangeSpinner.text = null
                diseaseSpinner.text = null
                selectedRange = null
                selectedDiseaseFilter = null
            }
        }

        // 範圍選單設定
        rangeSpinner.apply {
            setAdapter(ArrayAdapter(this@MainActivity1, android.R.layout.simple_list_item_1, ranges))
            setOnClickListener { showDropDown() }
            setOnItemClickListener { _, _, position, _ ->
                // 清空日期並記錄選擇範圍
                dateSpinner.text = null
                diseaseSpinner.text = null
                selectedRange = ranges[position]
                selectedDiseaseFilter = null
                lastSelectionType = SelectionType.ANALYSIS_RANGE // ✅ 加在這裡
            }
        }
    }

    private fun setupButtons() {
        btnAnalyze.setOnClickListener {
            val user = getSelectedUserOrWarn() ?: return@setOnClickListener
            val dateText = dateSpinner.text.toString().trim()

            // ✅ 1. 若已選擇「自訂區間」
            if (!customStartDate.isNullOrBlank() && !customEndDate.isNullOrBlank()) {
                if (customStartDate == customEndDate) {
                    // ✅ 起始日與結束日相同：分析單日資料
                    setLoading(true, "🔄 分析 ${customStartDate} 當日資料...")
                    api.getSingleAnalysis(user, customStartDate!!)
                        .enqueue(createAnalysisCallback(user))
                } else {
                    // ✅ 起始日與結束日不同：分析區間資料
                    setLoading(true, "🔄 分析 ${customStartDate} 到 ${customEndDate} 的資料...")
                    api.getCustomRangeAnalysis(user, customStartDate!!, customEndDate!!)
                        .enqueue(createAnalysisCallback(user))
                }

                // ✅ 2. 若是選單日（舊邏輯）
            } else if (dateText.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
                selectedRange = null
                rangeSpinner.text = null
                setLoading(true, "🔄 分析單日資料...")
                api.getSingleAnalysis(user, dateText).enqueue(createAnalysisCallback(user))

                // ✅ 3. 若選擇固定範圍
            } else if (!selectedRange.isNullOrBlank()) {
                when (selectedRange) {
                    "總分析" -> {
                        setLoading(true, "🔄 正在分析所有資料...")
                        api.getAllAggregate(user).enqueue(createAnalysisCallback(user))
                    }
                    "一週" -> analyzeRange(user, 7)
                    "一個月" -> analyzeRange(user, 30)
                    "半年" -> analyzeRange(user, 180)
                    "一年" -> analyzeRange(user, 365)
                }

                // ✅ 4. 都沒選
            } else {
                showToast("請先選擇日期或範圍")
            }
        }

        switchShowDetails.setOnCheckedChangeListener { _, isChecked ->
            if (chart.visibility == View.VISIBLE) {
                val user = getSelectedUserOrWarn() ?: return@setOnCheckedChangeListener
                val days = when (selectedRange) {
                    "一週" -> 7
                    "一個月" -> 30
                    "半年" -> 180
                    "一年" -> 365
                    else -> null
                }
                drawChartForRecords(user, days)
            }
        }

        switchToggleView.setOnCheckedChangeListener { _, isChecked ->
            resultView.visibility = if (isChecked) View.GONE else View.VISIBLE
            chart.visibility = if (isChecked) View.VISIBLE else View.GONE

            if (isChecked) {
                // 👉 顯示圖表才顯示單位
                val user = getSelectedUserOrWarn() ?: return@setOnCheckedChangeListener
                if (!customStartDate.isNullOrBlank() && !customEndDate.isNullOrBlank()) {
                    drawChartForRecords(user)
                } else {
                    val days = when (selectedRange) {
                        "一週" -> 7
                        "一個月" -> 30
                        "半年" -> 180
                        "一年" -> 365
                        else -> null
                    }
                    drawChartForRecords(user, days)
                }
            } else {
                // 👉 切回分析時隱藏單位文字
            }

            if (isChecked) {
                val user = getSelectedUserOrWarn() ?: return@setOnCheckedChangeListener

                // 若是自訂日期區間，顯示自訂資料
                if (!customStartDate.isNullOrBlank() && !customEndDate.isNullOrBlank()) {
                    drawChartForRecords(user) // 預設顯示全部，過濾在 drawChartForRecords() 處理
                } else {
                    val days = when (selectedRange) {
                        "一週" -> 7
                        "一個月" -> 30
                        "半年" -> 180
                        "一年" -> 365
                        else -> null
                    }
                    drawChartForRecords(user, days)
                }
            }
        }
    }

    private fun showDateRangeDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_date_range)

        val btnStart = dialog.findViewById<Button>(R.id.btnStartDate)
        val btnEnd = dialog.findViewById<Button>(R.id.btnEndDate)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        btnStart.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val date = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                customStartDate = sdf.format(date.time)
                btnStart.text = "開始：$customStartDate"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnEnd.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val date = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                customEndDate = sdf.format(date.time)
                btnEnd.text = "結束：$customEndDate"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnConfirm.setOnClickListener {
            if (customStartDate != null && customEndDate != null) {
                dateSpinner.setText("$customStartDate 到 $customEndDate", false)
                rangeSpinner.text = null
                diseaseSpinner.text = null
                selectedRange = null
                selectedDiseaseFilter = null

                lastSelectionType = SelectionType.DATE_RANGE
            } else {
                showToast("請選擇完整區間")
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createAnalysisCallback(user: String): Callback<AnalysisResult> =
        object : Callback<AnalysisResult> {
            override fun onResponse(call: Call<AnalysisResult>, response: Response<AnalysisResult>) {
                setLoading(false)
                if (response.isSuccessful) {
                    response.body()?.let {
                        showResult(it)
                    } ?: run {
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

    private fun openSuggestionUrl(disease: String) {
        Toast.makeText(this@MainActivity1, "📖 正在開啟：$disease 建議...", Toast.LENGTH_SHORT).show()

        api.getSourceUrl(disease).enqueue(object : Callback<UrlResponse> {
            override fun onResponse(call: Call<UrlResponse>, response: Response<UrlResponse>) {
                val url = response.body()?.url
                if (!url.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity1, "✅ 已跳轉至建議網站", Toast.LENGTH_SHORT).show()
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

    private fun drawChartForRecords(user: String, days: Int? = null) {
        chart.clear()
        api.getRecords(user).enqueue(object : Callback<List<HealthRecord>> {
            override fun onResponse(call: Call<List<HealthRecord>>, response: Response<List<HealthRecord>>) {
                val records = response.body()
                if (records.isNullOrEmpty()) {
                    showToast("無記錄資料")
                    Log.d("HealthAnalysis", "記錄資料為空")
                    return
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                // 解析所有記錄中的有效日期
                val recordDates = records.mapNotNull {
                    runCatching { dateFormat.parse(it.measure_at) }.getOrNull()
                }

                val latestDate = recordDates.maxOrNull()

                // 改成這樣：先看 days，再看 custom，再 fallback
                val filteredRecords = when {
                    // 1️⃣ 如果傳進來有 days，先用「最近 N 天」過濾
                    days != null && latestDate != null -> {
                        val cutoff = Calendar.getInstance().apply {
                            time = latestDate
                            add(Calendar.DAY_OF_YEAR, -days)
                        }.time
                        records.filter {
                            dateFormat.parse(it.measure_at)?.after(cutoff) == true
                        }
                    }
                    // 2️⃣ 再看是不是自訂區間
                    !customStartDate.isNullOrBlank() && !customEndDate.isNullOrBlank() -> {
                        val start = dateFormat.parse(customStartDate!!)!!
                        val end   = dateFormat.parse(customEndDate!!)!!
                        records.filter { rec ->
                            dateFormat.parse(rec.measure_at)?.let { it in start..end } ?: false
                        }
                    }
                    // 3️⃣ 都沒有，就全部資料
                    else -> records
                }


                // 🔽 以下這段保留你原本的
                val inputFormats = listOf(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                    SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
                    SimpleDateFormat("yyyy/M/d", Locale.getDefault()),
                    SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
                )

                val outputFormat = SimpleDateFormat("M/d", Locale.getDefault())

                // 解析 + 排序（注意！使用 Pair 確保資料跟日期配對）
                val sortedWithDates = filteredRecords.mapNotNull { record ->
                    val parsedDate = inputFormats.firstNotNullOfOrNull { fmt ->
                        try { fmt.parse(record.measure_at) } catch (_: Exception) { null }
                    }
                    parsedDate?.let { Pair(record, it) }
                }.sortedBy { it.second }  // 以 Date 排序

                // 分離記錄與日期
                val sortedRecords = sortedWithDates.map { it.first }
                formattedDateLabels = sortedWithDates.map { outputFormat.format(it.second) }

                latestXLabels = sortedRecords.map { it.measure_at }

                // 更新使用者性別與身高（以第一筆記錄為準）
                sortedRecords.firstOrNull()?.let {
                    currentGender = it.gender
                    currentHeightM = it.height.toFloat() / 100
                }

                // 取得各記錄異常指標
                val abnormalIndicators = sortedRecords
                    .flatMap { analyzeRecord(it) }
                    .flatMap { diseaseSeriesMapping[it] ?: emptyList() }
                    .toSet()

                // 建立各量測指標資料集
                val systolicEntries = mutableListOf<Entry>()
                val diastolicEntries = mutableListOf<Entry>()
                val pulseEntries = mutableListOf<Entry>()
                sortedRecords.forEachIndexed { index, record ->
                    val fixedRecord = record.copy(user = user)

                    systolicEntries.add(
                        Entry(
                            index.toFloat(),
                            record.systolic_mmHg.toFloat()
                        ).apply {data = fixedRecord })
                    diastolicEntries.add(
                        Entry(
                            index.toFloat(),
                            record.diastolic_mmHg.toFloat()
                        ).apply { data = fixedRecord})
                    pulseEntries.add(
                        Entry(
                            index.toFloat(),
                            record.pulse_bpm.toFloat()
                        ).apply { data = fixedRecord })
                }

                // 使用 MutableList<ILineDataSet>，避免後續轉型問題
                val dataSets = mutableListOf<ILineDataSet>()
                if ("systolic" in abnormalIndicators && (selectedDiseaseFilter == null || selectedDiseaseFilter == "血壓")) {
                    val systolicDataSet =
                        createDataSet(systolicEntries, "收縮壓", Color.RED, Color.RED, unitLabel = "mmHg")
                    Log.d("HealthAnalysis", "systolicDataSet label: ${systolicDataSet.label}")
                    dataSets.add(systolicDataSet)
                }

                if ("diastolic" in abnormalIndicators && (selectedDiseaseFilter == null || selectedDiseaseFilter == "血壓")) {
                    val diastolicDataSet =
                        createDataSet(diastolicEntries, "舒張壓", Color.BLUE, Color.BLUE, unitLabel = "mmHg")
                    dataSets.add(diastolicDataSet)
                }

                if ("pulse" in abnormalIndicators && (selectedDiseaseFilter == null || selectedDiseaseFilter == "脈搏")) {
                    val pulseDataSet =
                        createDataSet(pulseEntries, "脈搏", Color.MAGENTA, Color.MAGENTA, unitLabel = "bpm")
                    Log.d("HealthAnalysis", "pulseDataSet label: ${pulseDataSet.label}")
                    dataSets.add(pulseDataSet)
                }

                // 建立疾病標記資料集（僅顯示資料點，不畫連線）
                val diseaseMarkerMap = mutableMapOf<String, MutableList<Entry>>().apply {
                    diseaseMapping.keys.forEach { put(it, mutableListOf()) }
                }
                sortedRecords.forEachIndexed { index, record ->
                    analyzeRecord(record).forEach { disease ->
                        diseaseSeriesMapping[disease]?.forEach { key ->
                            val yValue = when (key) {
                                "systolic" -> record.systolic_mmHg.toFloat()
                                "diastolic" -> record.diastolic_mmHg.toFloat()
                                "pulse" -> record.pulse_bpm.toFloat()
                                else -> return@forEach
                            }
                            diseaseMarkerMap[disease]?.add(
                                Entry(
                                    index.toFloat(),
                                    yValue
                                ).apply { data = record })
                        }
                    }
                }
                diseaseMarkerMap.forEach { (disease, entries) ->
                    if (entries.isNotEmpty()) {
                        Log.d(
                            "HealthAnalysis",
                            "Adding disease marker dataset, disease: $disease, entry count: ${entries.size}"
                        )
                        val relatedKeys = diseaseSeriesMapping[disease] ?: emptyList()
                        val isMatch = selectedDiseaseFilter == null || selectedDiseaseFilter == "全部" || when (selectedDiseaseFilter) {
                            "血壓" -> relatedKeys.any { it == "systolic" || it == "diastolic" }
                            "脈搏" -> relatedKeys.contains("pulse")
                            else -> false
                        }

                        if (isMatch) {
                            val (markerColor, _) = diseaseMapping[disease]!!
                            dataSets.add(
                                createDataSet(
                                    entries,
                                    label = disease,
                                    lineColor = markerColor,
                                    circleColor = markerColor,
                                    lineWidth = 0f,
                                    circleRadius = 6f,
                                    valueTextSize = 12f,
                                    drawValues = false
                                )
                            )
                        }
                    }
                }

                if (dataSets.isEmpty()) {
                    showToast("目前沒有異常指標，圖表無法顯示")
                    chart.clear()
                    return
                }
                chart.data = LineData(dataSets)
                chart.setVisibleXRangeMaximum(5f)
                chart.moveViewToX(chart.data.entryCount.toFloat())
                chart.data.dataSets.forEachIndexed { index, dataSet ->
                    Log.d(
                        "HealthAnalysis",
                        "After setting chart data: 資料集 $index 的 label: ${dataSet.label}"
                    )
                }
                // 設定圖表資料
                chart.data = LineData(dataSets)

                // 👇 單位顯示修正版 👇
                val hasRealData = dataSets.any { it.entryCount > 0 }
                if (hasRealData) {
                    val unitSet = mutableSetOf<String>()
                    dataSets.filter { it.entryCount > 0 }.forEach { dataSet ->
                        when {
                            dataSet.label.contains("收縮壓") || dataSet.label.contains("舒張壓") -> unitSet.add("mmHg")
                            dataSet.label.contains("脈搏") -> unitSet.add("bpm")
                        }
                    }
                }

                // 🔽 👇 加在這裡 👇
                chart.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        chart.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        chart.legend.isWordWrapEnabled = true
                        chart.legend.maxSizePercent = 0.7f
                        chart.invalidate()
                        chart.requestLayout()
                    }
                })

                val entryCount = chart.data?.dataSets?.firstOrNull()?.entryCount ?: 0

                chart.xAxis.apply {
                    setDrawLabels(true)
                    valueFormatter = IndexAxisValueFormatter(formattedDateLabels)
                    labelCount = formattedDateLabels.size
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textSize = 14.5f
                    labelRotationAngle = 0f
                    textColor = Color.DKGRAY
                }
                chart.axisLeft.textSize = 14.5f
                chart.axisRight.textSize = 14.5f
                chart.legend.apply {
                    textSize = 14.5f
                    formSize = 14.5f
                    xEntrySpace = 20f
                    yEntrySpace = 20f
                }
                configureChart(chart)
                chart.marker = MyMarkerView(
                    this@MainActivity1,
                    R.layout.custom_marker_view,
                    chart,
                    currentGender,
                    currentHeightM
                )


                if (switchShowDetails.isChecked) {
                    addLimitLinesToChart(chart, abnormalIndicators.filterByDisease())
                } else {
                    chart.axisLeft.removeAllLimitLines()  // 🔥 若未開啟「顯示細節」，清空 limit lines
                }

                // 計算單位字串
                val unitSet = mutableSetOf<String>()
                chart.data?.dataSets?.forEach { dataSet ->
                    when {
                        dataSet.label.contains("收縮壓") || dataSet.label.contains("舒張壓") -> unitSet.add("mmHg")
                        dataSet.label.contains("脈搏") -> unitSet.add("bpm")
                    }
                }

                if (unitSet.isNotEmpty()) {
                    val unitText = "單位：${unitSet.joinToString(" / ")}"

                    chart.description.apply {
                        text = unitText
                        textSize = 14f
                        textColor = Color.DKGRAY
                        // 移進來並往下
                        setPosition(400f, chart.height * 0.16f)  // 調整 Y 值來控制「往下」
                        isEnabled = true
                    }
                } else {
                    chart.description.isEnabled = false
                }



                chart.invalidate()
                // 延後操作，確保圖表已繪製完成
                chart.post {
                    // 此時圖表已經繪製完成，MarkerView 也應該已附加到圖表上
                    Log.d("HealthAnalysis", "Chart fully drawn, ready for interaction")
                    chart.legend.isWordWrapEnabled = true
                    chart.invalidate()
                    chart.requestLayout()
                }

                // 設定圖表手勢與點擊監聽（採用空實作）
                chart.setOnChartGestureListener(object : OnChartGestureListener {
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

                chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {}
                    override fun onNothingSelected() {
                        chart.highlightValue(null)
                    }
                })
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
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                yOffset = 12f
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
                        Color.YELLOW
                    )
                )
            if ("脈搏太低" in diseaseMapping)
                targetChart.axisLeft.addLimitLine(createLimitLine(50f, "脈搏太低：<50", Color.CYAN))
            if ("低脈搏" in diseaseMapping)
                targetChart.axisLeft.addLimitLine(
                    createLimitLine(
                        59f,
                        "低脈搏：50–59",
                        Color.parseColor("#ADD8E6")
                    )
                )
        }
    }

    private fun configureChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setHighlightPerTapEnabled(true)
            setHighlightPerDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDoubleTapToZoomEnabled(false)
            setDragEnabled(true)
            setDrawMarkers(true)
            chart.setScaleXEnabled(true)
            chart.setScaleYEnabled(false)

            // 🔧 左側 Y 軸設定
            axisLeft.apply {
                textSize = 14.5f
                textColor = Color.DKGRAY
                axisLineColor = Color.DKGRAY
                gridColor = Color.LTGRAY
            }

            // 🔧 右側 Y 軸設定（僅作輔助用途）
            axisRight.apply {
                isEnabled = true
                textSize = 14.5f
                textColor = Color.DKGRAY
                axisLineColor = Color.DKGRAY
                gridColor = Color.TRANSPARENT // 不顯示右側背景線
            }

            // 🔧 圖例 Legend 設定（自動換行 + 間距 + 對齊 + 美化）
            legend.apply {
                verticalAlignment = LegendVerticalAlignment.TOP
                horizontalAlignment = LegendHorizontalAlignment.CENTER
                orientation = LegendOrientation.HORIZONTAL
                setDrawInside(false)
                isWordWrapEnabled = true     // ✨ 超重要，支援自動換行
                maxSizePercent =  0.7f      // ✅ 限制最大寬度觸發換行
                xEntrySpace = 16f            // 左右間距
                yEntrySpace = 12f            // 上下間距
                textSize = 12f
                formSize = 12f               // 圖例前面的 icon 大小
            }

            setExtraOffsets(10f, 10f, 10f, 20f) // 四邊額外留白，避免重疊
        }
    }

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

            if (unitLabel.isNotBlank()) {
                valueFormatter = object : ValueFormatter() {
                    override fun getPointLabel(entry: Entry?): String {
                        return if (entry != null) "${entry.y.toInt()}" else ""
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

            // ✅ 複製主圖資料
            fullChart.data = chart.data

            // 🔍 解析目前有哪些單位
            val unitSet = mutableSetOf<String>()
            fullChart.data?.dataSets?.forEach { dataSet ->
                when {
                    dataSet.label.contains("收縮壓") || dataSet.label.contains("舒張壓") -> unitSet.add("mmHg")
                    dataSet.label.contains("脈搏") -> unitSet.add("bpm")
                }
            }

            fullChart.post {
                val unitLabel = "單位：" + unitSet.joinToString(" / ")
                fullChart.description.isEnabled = true
                fullChart.description.text = unitLabel
                fullChart.description.textSize = 16f
                fullChart.description.textColor = Color.DKGRAY
                fullChart.description.setPosition(
                    400f, fullChart.viewPortHandler.contentTop() - 40f)
                fullChart.invalidate()
            }

            // ✅ 補上單位格式
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

            // ✅ 偵測要加哪些異常線
            val dataLabels = chart.data.dataSets.mapNotNull { it.label }
            val abnormalIndicators = mutableSetOf<String>().apply {
                if (dataLabels.any { it.contains("收縮壓") }) add("systolic")
                if (dataLabels.any { it.contains("舒張壓") }) add("diastolic")
                if (dataLabels.any { it.contains("脈搏") }) add("pulse")
            }.filterByDisease().toSet()

            if (switchShowDetails.isChecked) {
                addLimitLinesToChart(fullChart, abnormalIndicators)
            } else {
                fullChart.axisLeft.removeAllLimitLines()
            }

            // ✅ 標準樣式
            configureChart(fullChart)

            // ✅ MarkerView
            fullChart.marker = MyMarkerView(
                this@MainActivity1,
                R.layout.custom_marker_view,
                fullChart,
                currentGender,
                currentHeightM
            )

            val infoText = findViewById<TextView>(R.id.user_info_text)
            val record = chart.data?.dataSets?.firstOrNull()?.getEntryForIndex(0)?.data as? HealthRecord
            val genderText = when (record?.gender?.lowercase()) {
                "male", "男" -> "男"
                "female", "女" -> "女"
                else -> "⚠️未知"
            }
            val ageText = record?.age?.let { "$it 歲" } ?: "未知"
            infoText.text = "👤 ${record?.user}｜⚧️ 性別：$genderText｜🎂 年齡：$ageText｜📏 身高：${record?.height?.toInt()}cm｜⚖️ 體重：${record?.weight}kg｜"


            val inputFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                SimpleDateFormat("yyyy/M/d", Locale.getDefault()),
                SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
            )
            val outputFormat = SimpleDateFormat("M/d", Locale.getDefault())

            fullChart.xAxis.valueFormatter = IndexAxisValueFormatter(formattedDateLabels)
            fullChart.xAxis.labelCount = formattedDateLabels.size


            // ✅ 設定 X 軸
            fullChart.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(formattedDateLabels)
                setDrawLabels(true)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelRotationAngle = 0f
                textSize = 16.5f
                textColor = Color.DKGRAY
                setLabelCount(formattedDateLabels.size, true)
                gridColor = Color.LTGRAY       // 使用淡灰色，柔和清楚
                gridLineWidth = 1.2f           // 線條比預設略粗但不過頭
                axisLineColor = Color.DKGRAY   // X 軸底線變明顯一點
                axisLineWidth = 1.5f
            }

            // ✅ Y 軸 / 圖例
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

            // ✅ 點資料後開啟建議網址
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
                        Toast.makeText(this@MainActivity1, "📖 正在開啟：$disease 建議...", Toast.LENGTH_SHORT).show()
                        openSuggestionUrl(disease)
                    }
                }

                override fun onNothingSelected() {
                    fullChart.highlightValue(null)
                }
            })

            // ✅ 支援手勢操作（可選）
            fullChart.setOnChartGestureListener(object : OnChartGestureListener {
                override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartSingleTapped(me: MotionEvent?) {}
                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
            })

            // ✅ 關閉按鈕
            closeBtn.setOnClickListener { dismiss() }

            show()
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
