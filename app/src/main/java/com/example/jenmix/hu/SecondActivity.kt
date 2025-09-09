package com.example.jenmix.hu

import com.example.jenmix.R
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
// MPAndroidChart 控件
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart

// 数据模型
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet

// 格式化
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter

// 组件 & 动画
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.animation.Easing

import com.google.android.material.card.MaterialCardView

// 交互
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

import android.widget.ImageView
import android.widget.ImageButton



import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan


import android.text.style.ForegroundColorSpan
import org.json.JSONException




class SecondActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private var startDate: String = ""
    private var endDate: String = ""
    private var filterOption: String = "all"
















    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BloodPressureAdapter
    private val dataList = mutableListOf<BloodPressureData2>()
    private lateinit var pieChart: PieChart
















    private lateinit var headerLayout: LinearLayout
















    private lateinit var btnAnalyzeData: Button // 分析數據按鈕
















    private var selectedValueType: String = "systolic"  // 預設選擇收縮壓
















    private lateinit var btnGenerateChart: Button // 生成图表按钮
    private lateinit var barChart: BarChart // 图表
    private lateinit var lineChart2: LineChart












    private lateinit var yAxisLabel: TextView
    private lateinit var xAxisLabel: TextView















    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val cardViewExplanation = findViewById<MaterialCardView>(R.id.cardViewExplanation)
        cardViewExplanation.visibility = View.GONE
        val newchartContainer = findViewById<FrameLayout>(R.id.chartContainer)
        newchartContainer.visibility = View.GONE
        val cardViewExplanation2 = findViewById<MaterialCardView>(R.id.cardViewExplanation2)
        cardViewExplanation2.visibility = View.GONE
        val newchartContainer2 = findViewById<FrameLayout>(R.id.newchartContainer)
        newchartContainer2.visibility = View.GONE


        val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
        imageButton2.visibility = View.GONE
        val imageButton = findViewById<ImageButton>(R.id.headerImage)
        imageButton.visibility = View.VISIBLE
        val headerText: TextView = findViewById(R.id.headerText)
        headerText.visibility = View.VISIBLE
        val subHeaderText: TextView = findViewById(R.id.subHeaderText)
        subHeaderText.visibility = View.VISIBLE






        val headerTextView = findViewById<TextView>(R.id.headerText)
        val text = "🩺🫀 血壓搜尋:"
        val spannable = SpannableString(text)

// 放大整段（因為只有一行）
        spannable.setSpan(
            RelativeSizeSpan(1.0f),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// 設定顏色為深藍色
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#003366")),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        headerTextView.setText(spannable, TextView.BufferType.SPANNABLE)














        imageButton.setOnClickListener {
            val imageButton = findViewById<ImageButton>(R.id.headerImage)
            imageButton.visibility = View.GONE

            val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
            imageButton2.visibility = View.VISIBLE
        }






        // 初始化视图元素
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BloodPressureAdapter(dataList)




        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter




        headerLayout = findViewById(R.id.fixedHeader)







        barChart = findViewById<BarChart>(R.id.lineChart) // 在此初始化
        barChart.visibility = View.GONE // 初始化时折线图不可见




        lineChart2 = findViewById<LineChart>(R.id.newlineChart)
        lineChart2.visibility = View.GONE








        if (::yAxisLabel.isInitialized) yAxisLabel.visibility = View.GONE
        if (::xAxisLabel.isInitialized) xAxisLabel.visibility = View.GONE




        val chartTitle = findViewById<TextView>(R.id.chartTitle)
        chartTitle.visibility = View.GONE




        val newchartTitle = findViewById<TextView>(R.id.newchartTitle)
        newchartTitle.visibility = View.GONE







        pieChart = findViewById(R.id.pieChart) // 在此初始化
        pieChart.visibility = View.GONE // 初始化时折线图不可见
        val pieChartTitle = findViewById<TextView>(R.id.pieChartTitle)
        pieChartTitle.visibility = View.GONE
        // 设置圆饼图字体大小
        pieChart.setEntryLabelTextSize(16f)  // 设置标签字体大小
        pieChart.setCenterTextSize(16f)  // 设置中心文本字体大小
        pieChart.description.textSize = 16f  // 设置描述文本字体大小
        val legend = pieChart.legend
        legend.textSize = 16f  // 設定圖例字體大小







        val legends = barChart.legend
        legends.textSize = 16f  // 設定圖例字體大小




        // 设置文字颜色为黑色
        pieChart.setEntryLabelColor(Color.BLACK)   // 设置切片标签的颜色为黑色
        pieChart.setCenterTextColor(Color.BLACK)   // 设置圆心文本颜色为黑色



        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }




        btnAnalyzeData = findViewById(R.id.btnAnalyzeData)
        btnAnalyzeData.visibility = View.GONE // 初始隐藏



        btnAnalyzeData.setOnClickListener {
            analyzeData()
        }




        findViewById<Button>(R.id.select).setOnClickListener {
            showSearchMethodDialog()
        }
    }





    private fun showSearchMethodDialog() {
        val options = arrayOf(
            "以日期搜尋",
            "以數值搜尋",
            //"以日期搜尋體重"
        )


        val dialogView = layoutInflater.inflate(R.layout.dialog_search_method, null)
        val listView = dialogView.findViewById<ListView>(R.id.list_options)


        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        listView.adapter = adapter


        val dialog = AlertDialog.Builder(this)
            .setTitle("選擇搜尋方式")
            .setView(dialogView)
            .create()


        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> showDatePicker()
                1 -> showValueSearch()
                2 -> showWeightSearchDatePicker()
            }
            dialog.dismiss()
        }


        dialog.show()
    }





    // 起始與結束日期連續選取
    private fun showWeightSearchDatePicker() {
        val calendar = Calendar.getInstance()




        // 選擇起始日期
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                startDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)


                // 選擇結束日期
                DatePickerDialog(
                    this,
                    { _, endYear, endMonth, endDay ->
                        endDate = String.format("%04d-%02d-%02d", endYear, endMonth + 1, endDay)
                        fetchWeightDataByDateRange(startDate!!, endDate!!)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }



    // 發送請求並處理回應
    private fun fetchWeightDataByDateRange(startDate: String, endDate: String) {
        val userId = 1 // 實際使用時動態帶入
        val url = "https://test-9wne.onrender.com/getFilteredWeightData?startDate=$startDate&endDate=$endDate&user_id=$userId"
        val request = Request.Builder().url(url).build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SecondActivity, "請求失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }


            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    val jsonArray = JSONArray(json)
                    val resultCount = jsonArray.length()
                    val entries = mutableListOf<WeightEntry>()


                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val rawDate = item.getString("measure_at")
                        val parsedDate = inputFormat.parse(rawDate)
                        val formattedDate = outputFormat.format(parsedDate!!)
                        val weight = item.getDouble("weight_kg").toFloat()


                        entries.add(WeightEntry(formattedDate, weight))
                    }


                    runOnUiThread {
                        Toast.makeText(this@SecondActivity, "共取得 $resultCount 筆體重資料", Toast.LENGTH_LONG).show()
                        showWeightData(entries)
                    }
                }
            }
        })
    }






    private fun showWeightData(entries: List<WeightEntry>) {
        val recyclerViewWeight = findViewById<RecyclerView>(R.id.recyclerViewWeight)
        val weightHeader = findViewById<LinearLayout>(R.id.weightHeader)


        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
        findViewById<LinearLayout>(R.id.weightHeader).visibility = View.GONE
        barChart.visibility = View.GONE
        pieChart.visibility = View.GONE
        lineChart2 = findViewById<LineChart>(R.id.newlineChart)
        lineChart2.visibility = View.GONE
        findViewById<TextView>(R.id.chartTitle).visibility = View.GONE
        findViewById<TextView>(R.id.newchartTitle).visibility = View.GONE
        findViewById<TextView>(R.id.pieChartTitle).visibility = View.GONE
        findViewById<View>(R.id.textDateRange).visibility = View.GONE
        findViewById<View>(R.id.textDateRange2).visibility = View.GONE
        val imageButton = findViewById<ImageButton>(R.id.headerImage)
        imageButton.visibility = View.GONE
        val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
        imageButton2.visibility = View.GONE
        val headerText: TextView = findViewById(R.id.headerText)
        headerText.visibility = View.GONE
        val subHeaderText: TextView = findViewById(R.id.subHeaderText)
        subHeaderText.visibility = View.GONE



        // Y軸、X軸標籤
        if (::xAxisLabel.isInitialized) {
            xAxisLabel.visibility = View.GONE
        }
        if (::yAxisLabel.isInitialized) {
            yAxisLabel.visibility = View.GONE
        }


        // 按鈕（如果需要也可以隱藏）
        btnAnalyzeData.visibility = View.GONE
        headerLayout.visibility = View.GONE


        recyclerViewWeight.visibility = View.VISIBLE
        weightHeader.visibility = View.VISIBLE


        recyclerViewWeight.layoutManager = LinearLayoutManager(this)
        recyclerViewWeight.adapter = WeightAdapter(entries)
    }









    private fun showValueSearch() {
        val options = arrayOf("以舒張壓搜尋", "以收縮壓搜尋")


        AlertDialog.Builder(this)
            .setTitle("選擇搜尋方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showDiastolicPressureInput()  // 选择舒张压搜索
                    1 -> showSystolicPressureInput()   // 选择收缩压搜索
                }
            }
            .show()
    }





    // 选择以舒张压搜索时弹出两个输入框：舒张压下限和舒张压上限
    private fun showDiastolicPressureInput() {
        val diastolicLower = EditText(this)
        val diastolicUpper = EditText(this)






        diastolicLower.hint = "請輸入最小舒張壓"
        diastolicUpper.hint = "請輸入最大舒張壓"



        diastolicLower.inputType = InputType.TYPE_CLASS_NUMBER
        diastolicUpper.inputType = InputType.TYPE_CLASS_NUMBER




        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 20)  // 加一些間距
        layout.addView(diastolicLower)
        layout.addView(diastolicUpper)



        AlertDialog.Builder(this)
            .setTitle("輸入舒張壓範圍")
            .setView(layout)
            .setPositiveButton("確認") { _, _ ->
                val lower = diastolicLower.text.toString().trim().toIntOrNull()
                val upper = diastolicUpper.text.toString().trim().toIntOrNull()


                if (lower == null || upper == null) {
                    Toast.makeText(this, "請輸入有效的數字", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (lower > upper) {
                    Toast.makeText(this, "下限不能大於上限", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }


                fetchBloodPressureDataByRange(lower, upper, "diastolic") // 呼叫 API
            }
            .setNegativeButton("取消", null)
            .show()
    }





    private fun showSystolicPressureInput() {
        val systolicLower = EditText(this)
        val systolicUpper = EditText(this)

        systolicLower.hint = "請輸入最小收縮壓"
        systolicUpper.hint = "請輸入最大收縮壓"


        systolicLower.inputType = InputType.TYPE_CLASS_NUMBER
        systolicUpper.inputType = InputType.TYPE_CLASS_NUMBER

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 20)  // 加一些間距
        layout.addView(systolicLower)
        layout.addView(systolicUpper)

        AlertDialog.Builder(this)
            .setTitle("輸入收縮壓範圍")
            .setView(layout)
            .setPositiveButton("確認") { _, _ ->
                val lower = systolicLower.text.toString().trim().toIntOrNull()
                val upper = systolicUpper.text.toString().trim().toIntOrNull()


                if (lower == null || upper == null) {
                    Toast.makeText(this, "請輸入有效的數字", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }


                if (lower > upper) {
                    Toast.makeText(this, "下限不能大於上限", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }


                fetchBloodPressureDataByRange(lower, upper, "systolic") // 呼叫 API
            }
            .setNegativeButton("取消", null)
            .show()
    }





    private fun showDatePicker() {
        val calendar = Calendar.getInstance()


        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                startDate = "$year-${month + 1}-$dayOfMonth"


                DatePickerDialog(
                    this,
                    { _, endYear, endMonth, endDay ->
                        endDate = "$endYear-${endMonth + 1}-$endDay"
                        showFilterOptions()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()




            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }





    private fun showFilterOptions() {
        val options = arrayOf("所有數據", "正常", "偏高", "偏低", "危險")
        var selectedIndex = -1

        AlertDialog.Builder(this)
            .setTitle("選擇篩選條件")
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                filterOption = when (which) {
                    1 -> "normal"
                    2 -> "elevated"
                    3 -> "low"
                    4 -> "danger"
                    else -> "all"
                }
            }
            .setPositiveButton("確定") { _, _ -> fetchBloodPressureData() }
            .setNegativeButton("取消", null)
            .show()
    }



    private fun fetchBloodPressureData() {
        // 從 SharedPreferences 取出 username
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", null)

        if (username == null) {
            Toast.makeText(this, "找不到使用者帳號，請重新登入", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://test-9wne.onrender.com/getFilteredBloodPressureData" +
                "?startDate=$startDate&endDate=$endDate&filter=$filterOption&username=$username"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "請求失敗", Toast.LENGTH_SHORT).show()
                }
                Log.e("API_ERROR", e.message ?: "發生錯誤")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseData != null) {
                        try {
                            // 解析 JSON 陣列
                            parseAndSortBloodPressureData(responseData)
                        } catch (e: JSONException) {
                            Toast.makeText(applicationContext, "資料格式錯誤", Toast.LENGTH_SHORT).show()
                            Log.e("JSON_ERROR", e.message ?: "JSON解析錯誤")
                        }
                    } else {
                        Toast.makeText(applicationContext, "伺服器錯誤或無資料", Toast.LENGTH_SHORT).show()
                        Log.e("API_ERROR", "Response code: ${response.code}")
                    }

                    // 隱藏其他圖表
                    findViewById<BarChart>(R.id.barChart).visibility = View.GONE
                    lineChart2 = findViewById<LineChart>(R.id.newlineChart)
                    lineChart2.visibility = View.GONE
                    findViewById<TextView>(R.id.newchartTitle).visibility = View.GONE
                    pieChart.visibility = View.GONE
                    findViewById<RecyclerView>(R.id.recyclerViewWeight).visibility = View.GONE
                    findViewById<View>(R.id.weightHeader).visibility = View.GONE
                    findViewById<TextView>(R.id.pieChartTitle).visibility = View.GONE
                    findViewById<View>(R.id.textDateRange).visibility = View.GONE
                    findViewById<View>(R.id.textDateRange2).visibility = View.GONE
                    findViewById<ImageButton>(R.id.headerImage).visibility = View.GONE
                    findViewById<ImageView>(R.id.headerImage2).visibility = View.GONE
                    findViewById<TextView>(R.id.headerText).visibility = View.GONE
                    findViewById<TextView>(R.id.subHeaderText).visibility = View.GONE
                }
            }
        })
    }





    private fun fetchBloodPressureDataByRange(lower: Int, upper: Int, valueType: String) {
        // 隱藏圖表，顯示表格
        barChart.visibility = View.GONE
        lineChart2 = findViewById(R.id.newlineChart)
        lineChart2.visibility = View.GONE
        findViewById<TextView>(R.id.newchartTitle).visibility = View.GONE
        val chartTitle = findViewById<TextView>(R.id.chartTitle)
        chartTitle.visibility = View.GONE
        pieChart.visibility = View.GONE
        findViewById<RecyclerView>(R.id.recyclerViewWeight).visibility = View.GONE
        findViewById<View>(R.id.weightHeader).visibility = View.GONE
        val pieChartTitle = findViewById<TextView>(R.id.pieChartTitle)
        pieChartTitle.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        headerLayout.visibility = View.VISIBLE
        findViewById<View>(R.id.textDateRange).visibility = View.GONE
        findViewById<View>(R.id.textDateRange2).visibility = View.GONE
        val imageButton = findViewById<ImageButton>(R.id.headerImage)
        imageButton.visibility = View.GONE
        val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
        imageButton2.visibility = View.GONE
        val headerText: TextView = findViewById(R.id.headerText)
        headerText.visibility = View.GONE
        val subHeaderText: TextView = findViewById(R.id.subHeaderText)
        subHeaderText.visibility = View.GONE

        // 從 SharedPreferences 取出 username
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", null)

        if (username == null) {
            Toast.makeText(this, "找不到使用者帳號，請重新登入", Toast.LENGTH_SHORT).show()
            return
        }

        val url =
            "https://test-9wne.onrender.com/getBloodPressureByValue" +
                    "?type=$valueType&min=$lower&max=$upper&username=$username"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "請求失敗", Toast.LENGTH_SHORT).show()
                }
                Log.e("API_ERROR", e.message ?: "發生錯誤")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        responseData?.let {
                            parseAndSortBloodPressureData(it)
                        }
                    } else {
                        Toast.makeText(applicationContext, "伺服器錯誤", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }





    private fun parseAndSortBloodPressureData(json: String) {
        val jsonArray = JSONArray(json)
        dataList.clear()


        // 解析數據並加入 dataList
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val date = obj.getString("measure_at")
            val sys = obj.getInt("systolic_mmHg")
            val dia = obj.getInt("diastolic_mmHg")
            dataList.add(BloodPressureData2(date, sys, dia))
        }




        // 使用 SimpleDateFormat 來對日期進行排序
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())




        // 根據日期進行排序
        dataList.sortBy { dateFormat.parse(it.date) }




        // 更新 RecyclerView 顯示排序後的數據
        runOnUiThread {
            adapter.notifyDataSetChanged()


            if (dataList.isNotEmpty()) {
                recyclerView.visibility = View.VISIBLE
                headerLayout.visibility = View.VISIBLE
                btnAnalyzeData.visibility = View.VISIBLE  // 顯示分析按鈕
                barChart.visibility = View.GONE
                lineChart2 = findViewById<LineChart>(R.id.newlineChart)
                lineChart2.visibility = View.GONE
                findViewById<TextView>(R.id.newchartTitle).visibility = View.GONE
                if (::yAxisLabel.isInitialized) yAxisLabel.visibility = View.GONE
                if (::xAxisLabel.isInitialized) xAxisLabel.visibility = View.GONE
                val chartTitle = findViewById<TextView>(R.id.chartTitle)
                chartTitle.visibility = View.GONE
                pieChart.visibility = View.GONE
                findViewById<RecyclerView>(R.id.recyclerViewWeight).visibility = View.GONE
                findViewById<View>(R.id.weightHeader).visibility = View.GONE
                val pieChartTitle = findViewById<TextView>(R.id.pieChartTitle)
                pieChartTitle.visibility = View.GONE
                val imageButton = findViewById<ImageButton>(R.id.headerImage)
                imageButton.visibility = View.GONE
                val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
                imageButton2.visibility = View.GONE
                val headerText: TextView = findViewById(R.id.headerText)
                headerText.visibility = View.GONE
                val subHeaderText: TextView = findViewById(R.id.subHeaderText)
                subHeaderText.visibility = View.GONE

            } else {
                recyclerView.visibility = View.GONE
                headerLayout.visibility = View.GONE
                barChart.visibility = View.GONE
                lineChart2 = findViewById<LineChart>(R.id.newlineChart)
                lineChart2.visibility = View.GONE
                findViewById<TextView>(R.id.newchartTitle).visibility = View.GONE
                if (::yAxisLabel.isInitialized) yAxisLabel.visibility = View.GONE
                if (::xAxisLabel.isInitialized) xAxisLabel.visibility = View.GONE
                val chartTitle = findViewById<TextView>(R.id.chartTitle)
                chartTitle.visibility = View.GONE
                btnAnalyzeData.visibility = View.GONE  // 無數據時隱藏分析按鈕
                val pieChartTitle = findViewById<TextView>(R.id.pieChartTitle)
                pieChartTitle.visibility = View.GONE
                pieChart.visibility = View.GONE
                val imageButton = findViewById<ImageButton>(R.id.headerImage)
                imageButton.visibility = View.GONE
                val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
                imageButton2.visibility = View.GONE
                val headerText: TextView = findViewById(R.id.headerText)
                headerText.visibility = View.GONE
                val subHeaderText: TextView = findViewById(R.id.subHeaderText)
                subHeaderText.visibility = View.GONE

                findViewById<RecyclerView>(R.id.recyclerViewWeight).visibility = View.GONE
                findViewById<View>(R.id.weightHeader).visibility = View.GONE
                Toast.makeText(applicationContext, "無數據", Toast.LENGTH_SHORT).show()
            }
        }
    }






    private fun analyzeData() {
        if (dataList.isEmpty()) {
            Toast.makeText(this, "沒有數據可分析", Toast.LENGTH_SHORT).show()
            return
        }




        val options = arrayOf("基礎分析", "次數分布分析", "總百分比分析", "百分底分布分析")






        AlertDialog.Builder(this)
            .setTitle("選擇分析類型")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showBasicAnalysis()  // 執行基礎分析
                    1 -> showDistributionOptions()  // 讓使用者選擇月份或星期分析
                    2 -> showPieChart()  // 顯示圓餅圖
                    3 -> showDistributionOptionsline()
                }
            }
            .show()
    }







    private fun showPieChart() {
        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val barChart = findViewById<BarChart>(R.id.barChart)
        val lineChart = findViewById<BarChart>(R.id.lineChart)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val textDateRange2 = findViewById<TextView>(R.id.textDateRange2)
        val textDateRange = findViewById<TextView>(R.id.textDateRange)
        val pieChartTitle = findViewById<TextView>(R.id.pieChartTitle)
        val chartTitle = findViewById<TextView>(R.id.chartTitle)
        val cardViewExplanation2 = findViewById<MaterialCardView>(R.id.cardViewExplanation2)
        cardViewExplanation2.visibility = View.VISIBLE
        val imageButton = findViewById<ImageButton>(R.id.headerImage)
        imageButton.visibility = View.GONE
        val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
        imageButton2.visibility = View.GONE
        val headerText: TextView = findViewById(R.id.headerText)
        headerText.visibility = View.GONE
        val subHeaderText: TextView = findViewById(R.id.subHeaderText)
        subHeaderText.visibility = View.GONE


        pieChartTitle.visibility = View.VISIBLE


        val categoryLabelMap = mapOf(
            "normal" to "正常",
            "elevated" to "偏高",
            "low" to "偏低",
            "danger" to "危險"
        )


        val categoryCounts = mutableMapOf(
            "正常" to 0,
            "偏高" to 0,
            "偏低" to 0,
            "危險" to 0
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateList = dataList.mapNotNull { data ->
            try {
                dateFormat.parse(data.date)
            } catch (e: Exception) {
                null
            }
        }
        val earliestDate = dateList.minOrNull()
        val latestDate = dateList.maxOrNull()

        for (data in dataList) {
            val category = getBloodPressureCategory(data.sys, data.dia)
            val label = categoryLabelMap[category]
            if (label != null) {
                categoryCounts[label] = categoryCounts[label]!! + 1
            }
        }


        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        categoryCounts.forEach { (label, count) ->
            if (count > 0) {
                entries.add(PieEntry(count.toFloat(), label))
                val color = when (label) {
                    "正常" -> Color.parseColor("#006400")  // 深綠色
                    "偏高" -> Color.parseColor("#FFA500")  // 橙色
                    "偏低" -> Color.parseColor("#1E90FF")  // 藍色
                    "危險" -> Color.parseColor("#8B0000")  // 深紅色
                    else -> Color.GRAY
                }
                colors.add(color)
            }
        }

        val pieDataSet = PieDataSet(entries, "")
        pieDataSet.colors = colors
        pieDataSet.valueTextSize = 20f
        pieDataSet.valueTextColor = Color.WHITE
        pieDataSet.setValueFormatter(PercentFormatter(pieChart))

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData


        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(20f)
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(true)
        pieChart.animateY(1000, Easing.EaseInOutCubic)

        val legend = pieChart.legend
        legend.textSize = 18f
        legend.formSize = 15f
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setWordWrapEnabled(true)
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.form = Legend.LegendForm.CIRCLE
        legend.xEntrySpace = 20f

        pieChart.invalidate()

        val rangeFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val dateRange = if (earliestDate != null && latestDate != null) {
            "此圖包括 ${rangeFormatter.format(earliestDate)} - ${rangeFormatter.format(latestDate)} 的數據"
        } else {
            "沒有可用數據"
        }

        val standardText = """
註: sys→收縮壓, dia→舒張壓
▪ 正常：sys 90-120 且 dia 60-80
▪ 偏高：sys 120-140 或 dia ≥ 80-90
▪ 偏低：sys < 90 或 dia < 60
▪ 危險：sys > 140 或 dia > 90
""".trimIndent()

        textDateRange2.text = "$dateRange\n\n$standardText"
        textDateRange2.visibility = View.VISIBLE
        textDateRange.visibility = View.GONE

        findViewById<RecyclerView>(R.id.recyclerViewWeight).visibility = View.GONE
        findViewById<View>(R.id.weightHeader).visibility = View.GONE

        lineChart.axisLeft.setDrawLabels(false)
        lineChart.axisRight.setDrawLabels(false)
        lineChart.axisLeft.setDrawGridLines(false)
        lineChart.axisRight.setDrawGridLines(false)
        lineChart.axisLeft.setDrawAxisLine(false)
        lineChart.axisRight.setDrawAxisLine(false)

        pieChart.visibility = View.VISIBLE
        lineChart.visibility = View.GONE
        if (::yAxisLabel.isInitialized) yAxisLabel.visibility = View.GONE
        if (::xAxisLabel.isInitialized) xAxisLabel.visibility = View.GONE
        recyclerView.visibility = View.GONE
        headerLayout.visibility = View.GONE
        barChart.visibility = View.GONE
        lineChart2 = findViewById<LineChart>(R.id.newlineChart)
        lineChart2.visibility = View.GONE
        findViewById<TextView>(R.id.newchartTitle).visibility = View.GONE
        chartTitle.visibility = View.GONE


    }



    private fun showBasicAnalysis() {
        val count = dataList.size
        val avgSys = dataList.map { it.sys }.average()
        val avgDia = dataList.map { it.dia }.average()
        val maxSys = dataList.maxByOrNull { it.sys }?.sys ?: 0
        val maxDia = dataList.maxByOrNull { it.dia }?.dia ?: 0

        val message = """
  數據總數: $count
  平均收縮壓: ${avgSys.toInt()} mmHg
  平均舒張壓: ${avgDia.toInt()} mmHg
  最大收縮壓: $maxSys mmHg
  最大舒張壓: $maxDia mmHg
""".trimIndent()



        AlertDialog.Builder(this)
            .setTitle("基礎分析結果")
            .setMessage(message)
            .setPositiveButton("確定", null)
            .show()
    }






    private fun showDistributionOptions() {
        val options = arrayOf(
            "以月份分析\n(顯示搜尋出的資料裡有的月分的各個血壓標準各有幾次)",
            "\n以星期分析\n(顯示搜尋的資料裡有的星期(一~日)的各個血壓標準各有幾次)"
        )



        AlertDialog.Builder(this)
            .setTitle("選擇分析方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> analyzeByMonth()  // 進行月份分析
                    1 -> analyzeByWeek()   // 進行星期分析
                }
            }
            .show()
    }






    // 自定义的 ValueFormatter
    class CustomValueFormatter2 : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "${value.toInt()}%"
        }
    }



    private fun showLineChart(dataMap: Map<String, Map<String, Int>>) {
        val newchartTitle = findViewById<TextView>(R.id.newchartTitle)
        val newlineChart = findViewById<LineChart>(R.id.newlineChart)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val textDateRange = findViewById<TextView>(R.id.textDateRange)
        val textDateRange2 = findViewById<TextView>(R.id.textDateRange2)
        val barChart = findViewById<BarChart>(R.id.barChart)
        val chartTitle = findViewById<TextView>(R.id.chartTitle)
        val lineChart = findViewById<BarChart>(R.id.lineChart)
        val cardViewExplanation = findViewById<MaterialCardView>(R.id.cardViewExplanation)
        cardViewExplanation.visibility = View.VISIBLE
        val newchartContainer = findViewById<FrameLayout>(R.id.newchartContainer)
        newchartContainer.visibility = View.VISIBLE
        val chartContainer = findViewById<FrameLayout>(R.id.chartContainer)
        chartContainer.visibility = View.VISIBLE
        val imageButton = findViewById<ImageButton>(R.id.headerImage)
        imageButton.visibility = View.GONE
        val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
        imageButton2.visibility = View.GONE
        val headerText: TextView = findViewById(R.id.headerText)
        headerText.visibility = View.GONE
        val subHeaderText: TextView = findViewById(R.id.subHeaderText)
        subHeaderText.visibility = View.GONE





        barChart.visibility = View.GONE
        chartTitle.visibility = View.GONE








        val labels = mutableListOf<String>()
        val normalEntries = mutableListOf<Entry>()
        val elevatedEntries = mutableListOf<Entry>()
        val lowEntries = mutableListOf<Entry>()
        val dangerEntries = mutableListOf<Entry>()




        var xIndex = 0f
        val sortedData = dataMap.toSortedMap()




        val isWeekData = sortedData.keys.all { it.toIntOrNull() in 1..7 }
        val isMonthData = sortedData.keys.all { it.matches(Regex("\\d{4}-\\d{2}")) }
        val isFullDateData = sortedData.keys.all { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }




        newchartTitle.text = when {
            isWeekData -> "各星期幾血壓分類分佈圖"
            isMonthData -> "各月份血壓分類分佈圖"
            isFullDateData -> "每日血壓分類趨勢圖"
            else -> "血壓分類統計圖"
        }
        newchartTitle.visibility = View.VISIBLE




        val weekLabels = listOf("", "一", "二", "三", "四", "五", "六", "日")




        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var earliestDate: Date? = null
        var latestDate: Date? = null
        for (data in dataList) {
            try {
                val date = sdf.parse(data.date)
                if (earliestDate == null || date.before(earliestDate)) earliestDate = date
                if (latestDate == null || date.after(latestDate)) latestDate = date
            } catch (e: Exception) {
                Log.e("Date Parsing Error", "日期解析錯誤: ${data.date}")
            }
        }




        val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val dateRange = if (earliestDate != null && latestDate != null) {
            "此圖包括 ${dateFormatter.format(earliestDate)} - ${dateFormatter.format(latestDate)} 的數據"
        } else {
            "沒有可用數據"
        }




        val standardText = """
註: sys->收縮壓 , dia->舒張壓
▪ 危險：sys > 140 或 dia > 90
▪ 低血壓：sys < 90 或 dia < 60
▪ 正常：sys 90–120 且 dia 60–80
▪ 血壓偏高：sys 121–140 或 dia 81–90
""".trimIndent()
        textDateRange.text = "$dateRange\n\n$standardText"
        textDateRange.visibility = View.VISIBLE
        textDateRange2.visibility = View.GONE




        val inputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy/MM", Locale.getDefault())




        for ((key, categories) in sortedData) {
            val total = categories.values.sum().takeIf { it != 0 } ?: 1




            val label = when {
                isWeekData -> weekLabels[key.toInt()]
                isMonthData -> try { outputFormat.format(inputFormat.parse(key)!!) } catch (e: Exception) { key }
                isFullDateData -> try { outputFormat.format(sdf.parse(key)!!) } catch (e: Exception) { key }
                else -> key
            }
            labels.add(label)




            normalEntries.add(Entry(xIndex, (categories["normal"]?.toFloat() ?: 0f) / total * 100))
            elevatedEntries.add(Entry(xIndex, (categories["elevated"]?.toFloat() ?: 0f) / total * 100))
            lowEntries.add(Entry(xIndex, (categories["low"]?.toFloat() ?: 0f) / total * 100))
            dangerEntries.add(Entry(xIndex, (categories["danger"]?.toFloat() ?: 0f) / total * 100))




            xIndex += 1f
        }




        if (labels.isNotEmpty()) {
            val lastIndex = labels.lastIndex
            labels[lastIndex] = when {
                isWeekData -> "${labels[lastIndex]}"
                isMonthData -> "${labels[lastIndex]}"
                else -> labels[lastIndex]
            }
        }




        val customValueFormatter: ValueFormatter = CustomValueFormatter2()




        fun makeDataSet(entries: List<Entry>, label: String, colorCode: String): LineDataSet {
            return LineDataSet(entries, label).apply {
                color = Color.parseColor(colorCode)
                setCircleColor(Color.parseColor(colorCode))
                lineWidth = 5f
                circleRadius = 6f
                valueTextSize = 14f
                mode = LineDataSet.Mode.LINEAR
                valueFormatter = customValueFormatter
            }
        }




        val lineData = LineData(
            makeDataSet(normalEntries, "正常", "#006400"),
            makeDataSet(elevatedEntries, "血壓偏高", "#FF8C00"),
            makeDataSet(lowEntries, "低血壓", "#1E90FF"),
            makeDataSet(dangerEntries, "危險", "#8B0000")
        )




        val xAxis = newlineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.textSize = 14f
        xAxis.setLabelCount(labels.size, true)
        xAxis.setAvoidFirstLastClipping(false)




        val yAxis = newlineChart.axisLeft
        yAxis.setDrawLabels(true)
        yAxis.setDrawGridLines(true)
        yAxis.setDrawAxisLine(true)
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 110f  // 預留空間避免 label 擠在上方
        yAxis.granularity = 1f
        yAxis.textSize = 16f
        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "${value.toInt()}%"
        }




        newlineChart.axisRight.isEnabled = false




        val legend = newlineChart.legend
        legend.isWordWrapEnabled = true
        legend.setXEntrySpace(30f)
        legend.setFormSize(15f)
        legend.textSize = 20f
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)




        newlineChart.data = lineData
        newlineChart.setExtraOffsets(20f, 40f, 80f, 30f)
        newlineChart.notifyDataSetChanged()
        newlineChart.invalidate()




        val chartParent = newlineChart.parent as ViewGroup




        if (!::yAxisLabel.isInitialized) {
            yAxisLabel = TextView(this).apply {
                text = "(百分比%)"
                textSize = 16f
                setTextColor(Color.DKGRAY)
                rotation = -90f
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.START or Gravity.TOP
                    topMargin = 50   // 更靠近頂端
                    leftMargin = 40  // 靠近 Y 軸，但避免遮住 Y 軸刻度
                }
            }
            chartParent.addView(yAxisLabel)
        }
        yAxisLabel.visibility = View.VISIBLE




        if (!::xAxisLabel.isInitialized) {
            xAxisLabel = TextView(this).apply {
                text = ""
                textSize = 16f
                setTextColor(Color.DKGRAY)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                    bottomMargin = 40
                }
            }
            chartParent.addView(xAxisLabel)
        }
        updateAxisLabelsForLineChart()

        findViewById<TextView>(R.id.newchartTitle).visibility = View.VISIBLE
        barChart.visibility = View.GONE
        recyclerView.visibility = View.GONE
        headerLayout.visibility = View.GONE
        pieChart.visibility = View.GONE
        findViewById<TextView>(R.id.pieChartTitle).visibility = View.GONE
        textDateRange2.visibility = View.GONE
        textDateRange.visibility = View.VISIBLE

        findViewById<RecyclerView>(R.id.recyclerViewWeight).visibility = View.GONE
        findViewById<View>(R.id.weightHeader).visibility = View.GONE
        lineChart.visibility = View.GONE
        newlineChart.visibility = View.VISIBLE

    }








    private fun showDistributionOptionsline() {
        val options = arrayOf("以月份分析\n(顯示搜尋出的資料裡有的月分的各個血壓標準各佔幾%)", "\n以星期分析\n(顯示搜尋的資料裡有的星期(一~日)的各個血壓標準各佔幾%)")


        AlertDialog.Builder(this)
            .setTitle("選擇分析方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> analyzeByMonthline()  // 進行月份分析
                    1 -> analyzeByWeekline()   // 進行星期分析
                }
            }
            .show()
    }








    private fun analyzeByMonthline() {
        val monthData = mutableMapOf<String, MutableMap<String, Int>>()

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        for (data in dataList) {
            try {
                val date = inputFormat.parse(data.date)
                val yearMonth = outputFormat.format(date!!)
                val category = getBloodPressureCategory(data.sys, data.dia)

                // 初始化分類項目，如果還沒出現
                if (monthData[yearMonth] == null) {
                    monthData[yearMonth] = mutableMapOf(
                        "normal" to 0,
                        "elevated" to 0,
                        "low" to 0,
                        "danger" to 0
                    )
                }
                // 增加該分類次數
                monthData[yearMonth]?.set(category, monthData[yearMonth]?.get(category)!! + 1)
            } catch (e: Exception) {
                Log.e("Date Parse Error", "日期格式錯誤: ${data.date}")
            }
        }

        showLineChart(monthData)
    }








    private fun analyzeByWeekline() {
        val weekData = mutableMapOf<String, MutableMap<String, Int>>()

        for (data in dataList) {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(data.date)
                val calendar = Calendar.getInstance()
                calendar.time = date!!

                val weekDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SUNDAY -> "日"
                    Calendar.MONDAY -> "一"
                    Calendar.TUESDAY -> "二"
                    Calendar.WEDNESDAY -> "三"
                    Calendar.THURSDAY -> "四"
                    Calendar.FRIDAY -> "五"
                    Calendar.SATURDAY -> "六"
                    else -> "未知"
                }

                val category = getBloodPressureCategory(data.sys, data.dia)

                // 初始化分類項目
                if (weekData[weekDay] == null) {
                    weekData[weekDay] = mutableMapOf(
                        "normal" to 0,
                        "elevated" to 0,
                        "low" to 0,
                        "danger" to 0
                    )
                }

                // 增加該分類次數
                weekData[weekDay]?.set(category, weekData[weekDay]?.get(category)!! + 1)
            } catch (e: Exception) {
                Log.e("Date Parsing Error", "日期解析錯誤: ${data.date}")
            }
        }

        showLineChart(weekData)
    }












    private fun analyzeByMonth() {
        val monthData = mutableMapOf<String, MutableMap<String, Int>>()

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        for (data in dataList) {
            try {
                val date = inputFormat.parse(data.date)
                val yearMonth = outputFormat.format(date!!)
                val category = getBloodPressureCategory(data.sys, data.dia)

                // 初始化分類
                if (monthData[yearMonth] == null) {
                    monthData[yearMonth] =
                        mutableMapOf("normal" to 0, "elevated" to 0, "low" to 0, "danger" to 0)
                }
                monthData[yearMonth]?.set(category, monthData[yearMonth]?.get(category)!! + 1)
            } catch (e: Exception) {
                Log.e("Date Parse Error", "日期格式錯誤: ${data.date}")
            }
        }


        // 顯示月份長條圖
        showBarChart(monthData)
    }









    private fun analyzeByWeek() {
        val weekData = mutableMapOf<String, MutableMap<String, Int>>()

        for (data in dataList) {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(data.date)
                val calendar = Calendar.getInstance()
                calendar.time = date!!
                val weekDay =
                    calendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ..., 7=Saturday


                // 把日曜日 (1) 映射到 7，星期一 (2) 映射到 1，依此類推
                val adjustedWeekDay = if (weekDay == 1) 7 else weekDay - 1

                val category = getBloodPressureCategory(data.sys, data.dia)


                // 初始化分類
                if (weekData[adjustedWeekDay.toString()] == null) {
                    weekData[adjustedWeekDay.toString()] =
                        mutableMapOf("normal" to 0, "elevated" to 0, "low" to 0, "danger" to 0)
                }
                weekData[adjustedWeekDay.toString()]?.set(
                    category,
                    weekData[adjustedWeekDay.toString()]?.get(category)!! + 1
                )
            } catch (e: Exception) {
                Log.e("Date Parsing Error", "日期解析錯誤: ${data.date}")
            }
        }




        // 顯示星期長條圖
        showBarChart(weekData)
    }






    private fun getBloodPressureCategory(sys: Int, dia: Int): String {
        return when {
            sys > 140 || dia > 90 -> "danger"                 // 危險
            sys < 90 || dia < 60 -> "low"                     // 低血壓
            sys in 90..120 && dia in 60..80 -> "normal"       // 正常
            sys in 121..140 || dia in 81..90 -> "elevated"    // 血壓偏高
            else -> "normal"
        }
    }




    // 自定义的 ValueFormatter
    class CustomValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "${value.toInt()}次" // 转换为整数并加上“次”
        }
    }








    private fun showBarChart(dataMap: Map<String, Map<String, Int>>) {
        val chartTitle = findViewById<TextView>(R.id.chartTitle)
        val barChart = findViewById<BarChart>(R.id.lineChart)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val textDateRange = findViewById<TextView>(R.id.textDateRange)
        val textDateRange2 = findViewById<TextView>(R.id.textDateRange2)
        val cardViewExplanation = findViewById<MaterialCardView>(R.id.cardViewExplanation)
        cardViewExplanation.visibility = View.VISIBLE
        val chartContainer = findViewById<FrameLayout>(R.id.chartContainer)
        chartContainer.visibility = View.VISIBLE
        val imageButton = findViewById<ImageButton>(R.id.headerImage)
        imageButton.visibility = View.GONE
        val imageButton2 = findViewById<ImageView>(R.id.headerImage2)
        imageButton2.visibility = View.GONE
        val headerText: TextView = findViewById(R.id.headerText)
        headerText.visibility = View.GONE
        val subHeaderText: TextView = findViewById(R.id.subHeaderText)
        subHeaderText.visibility = View.GONE



        val labels = mutableListOf<String>()
        val stackedEntries = mutableListOf<BarEntry>()
        var xIndex = 0f
        val sortedData = dataMap.toSortedMap()




        val isWeekData = sortedData.keys.all { it.toIntOrNull() in 1..7 }
        val isMonthData = sortedData.keys.all { it.matches(Regex("\\d{4}-\\d{2}")) }
        val isFullDateData = sortedData.keys.all { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }




        chartTitle.text = when {
            isWeekData -> "各星期幾血壓分類分佈圖"
            isMonthData -> "各月份血壓分類分佈圖"
            isFullDateData -> "每日血壓分類趨勢圖"
            else -> "血壓分類統計圖"
        }
        chartTitle.visibility = View.VISIBLE




        val weekLabels = listOf("", "一", "二", "三", "四", "五", "六", "日")




        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var earliestDate: Date? = null
        var latestDate: Date? = null
        for (data in dataList) {
            try {
                val date = sdf.parse(data.date)
                if (earliestDate == null || date.before(earliestDate)) earliestDate = date
                if (latestDate == null || date.after(latestDate)) latestDate = date
            } catch (e: Exception) {
                Log.e("Date Parsing Error", "日期解析錯誤: ${data.date}")
            }
        }




        val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val dateRange = if (earliestDate != null && latestDate != null) {
            "此圖包括 ${dateFormatter.format(earliestDate)} - ${dateFormatter.format(latestDate)} 的數據"
        } else {
            "沒有可用數據"
        }




        val standardText = """
    註:sys->收縮壓 , dia->舒張壓        
    ▪ 正常：sys 90-120 且 dia 60-80
    ▪ 偏高：sys 120-140 或 dia ≥ 80-90
    ▪ 偏低：sys < 90 或 dia < 60
    ▪ 危險：sys > 140 或 dia > 90
 """.trimIndent()




        textDateRange.text = "$dateRange\n\n$standardText"
        textDateRange.visibility = View.VISIBLE
        textDateRange2.visibility = View.GONE




        val inputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy/MM", Locale.getDefault())




        for ((key, categories) in sortedData) {
            val label = when {
                isWeekData -> weekLabels[key.toInt()]
                isMonthData -> try { outputFormat.format(inputFormat.parse(key)!!) } catch (e: Exception) { key }
                isFullDateData -> try { outputFormat.format(sdf.parse(key)!!) } catch (e: Exception) { key }
                else -> key
            }




            if (label.isNullOrEmpty()) continue




            labels.add(label)




            val normal = categories["normal"]?.toFloat() ?: 0f
            val elevated = categories["elevated"]?.toFloat() ?: 0f
            val low = categories["low"]?.toFloat() ?: 0f
            val danger = categories["danger"]?.toFloat() ?: 0f




            val stackedValues = floatArrayOf(normal, elevated, low, danger)
            stackedEntries.add(BarEntry(xIndex, stackedValues))
            xIndex += 1f
        }




        val stackSet = BarDataSet(stackedEntries, "血壓分類").apply {
            setColors(
                Color.parseColor("#006400"),
                Color.parseColor("#FF8C00"),
                Color.parseColor("#1E90FF"),
                Color.parseColor("#8B0000")
            )
            stackLabels = arrayOf("正常", "偏高", "偏低", "危險") // "升高" 改為 "偏高"
            valueTextSize = 14f
            valueFormatter = object : ValueFormatter() {
                override fun getBarStackedLabel(value: Float, barEntry: BarEntry?): String {
                    if (barEntry == null || barEntry.yVals == null) return ""
                    val total = barEntry.yVals.sum()
                    if (total == 0f) return ""




                    // 使用 indexOfFirst 來查找 value 在 yVals 中的位置
                    val index = barEntry.yVals.indexOfFirst { it == value }
                    val percent = (value / total * 100).toInt()
                    val count = value.toInt()
                    return " ${count}次"
                }
            }
        }




        val barData = BarData(stackSet).apply {
            barWidth = 0.6f
        }




        barChart.data = barData
        barChart.setFitBars(true)
        barChart.description.isEnabled = false




        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.textSize = 14f
        xAxis.setLabelCount(labels.size, false)
        xAxis.setAvoidFirstLastClipping(false)
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = xIndex - 0.5f
        xAxis.setDrawGridLines(false)




        val yAxis = barChart.axisLeft
        yAxis.setDrawLabels(true)
        yAxis.setDrawGridLines(true)
        yAxis.setDrawAxisLine(true)
        yAxis.axisMinimum = 0f
        yAxis.granularity = 1f
        yAxis.textSize = 16f
        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }
        barChart.axisRight.isEnabled = false




        val legend = barChart.legend
        legend.isWordWrapEnabled = true
        legend.setXEntrySpace(30f)
        legend.setFormSize(15f)
        legend.textSize = 20f
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)




        barChart.setExtraOffsets(20f, 40f, 80f, 30f)
        barChart.invalidate()




        // 點擊區段顯示詳細說明，使用 AlertDialog 代替 Toast
        barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e !is BarEntry || e.yVals == null) return
                val stackIndex = h?.stackIndex ?: -1
                if (stackIndex == -1 || stackIndex >= e.yVals.size) return




                val value = e.yVals[stackIndex]
                val total = e.yVals.sum()
                val percent = if (total > 0) (value / total * 100).toInt() else 0
                val count = value.toInt()




                val labelName = when (stackIndex) {
                    0 -> "正常"
                    1 -> "偏高" // 改為 "偏高"
                    2 -> "偏低"
                    3 -> "危險"
                    else -> "未知"
                }




                val xIndex = e.x.toInt()
                val categoryLabel = if (xIndex in labels.indices) labels[xIndex] else "未知分類"




                // 使用 AlertDialog 顯示詳細資料
                showDetailsDialog(categoryLabel, labelName, count, percent)
            }




            override fun onNothingSelected() {}
        })




        val chartParent = barChart.parent as ViewGroup
        if (!::yAxisLabel.isInitialized) {
            yAxisLabel = TextView(this).apply {
                text = "(次數)"
                textSize = 16f
                setTextColor(Color.DKGRAY)
                rotation = -90f
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.START or Gravity.TOP
                    topMargin = 100
                    leftMargin = 20
                }
            }
            chartParent.addView(yAxisLabel)
        }
        yAxisLabel.visibility = View.VISIBLE




        if (!::xAxisLabel.isInitialized) {
            xAxisLabel = TextView(this).apply {
                text = ""
                textSize = 16f
                setTextColor(Color.DKGRAY)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                    bottomMargin = 40
                }
            }
            chartParent.addView(xAxisLabel)
        }




        updateAxisLabelsForBarChart()




        headerLayout.visibility = View.GONE
        recyclerView.visibility = View.GONE
        pieChart.visibility = View.GONE
        findViewById<TextView>(R.id.pieChartTitle).visibility = View.GONE
        barChart.visibility = View.VISIBLE
        lineChart2 = findViewById<LineChart>(R.id.newlineChart)
        lineChart2.visibility = View.GONE
        findViewById<TextView>(R.id.newchartTitle).visibility = View.GONE

    }


    /**
     * 顯示詳細資料的 AlertDialog
     */
    private fun showDetailsDialog(categoryLabel: String, labelName: String, count: Int, percent: Int) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("選擇的區段")
            .setMessage("分類：$labelName\n次數：$count 次\n占比：$percent%\n分類日期：$categoryLabel")
            .setPositiveButton("關閉") { dialogInterface, _ ->
                dialogInterface.dismiss() // 關閉對話框
            }
            .create()




        dialog.show() // 顯示 Dialog
    }





    private fun updateAxisLabelsForBarChart() {
        yAxisLabel.text = "(次數)"
        (yAxisLabel.layoutParams as FrameLayout.LayoutParams).apply {
            topMargin = 100
            leftMargin = 20
        }
        yAxisLabel.requestLayout()


        xAxisLabel.text = "" // 可以根據需要給 X 軸加單位
    }


    private fun updateAxisLabelsForLineChart() {
        yAxisLabel.text = "(百分比)"
        (yAxisLabel.layoutParams as FrameLayout.LayoutParams).apply {
            topMargin = 55
            leftMargin = 20
        }
        yAxisLabel.requestLayout()


        xAxisLabel.text = "" // 可以根據需要給 X 軸加單位
    }


}
