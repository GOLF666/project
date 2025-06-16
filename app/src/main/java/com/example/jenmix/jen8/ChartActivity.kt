package com.example.jenmix.jen8

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChartActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        lineChart = findViewById(R.id.lineChart)
        pieChart = findViewById(R.id.pieChart)
        btnBack = findViewById(R.id.btnBackToMain)

        btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity8::class.java))
            finish()
        }

        // 取得歷史紀錄並顯示圖表
        loadWeightDataAndShowCharts()
    }

    private fun loadWeightDataAndShowCharts() {
        val api = RetrofitClient.getInstance().create(HistoryApi::class.java)

        // ✅ 改成固定查詢「使用者名稱」
        val call = api.getChartData("使用者名稱") // ← 如果你後續有登入功能可改為變數

        call.enqueue(object : Callback<List<WeightRecord>> {
            override fun onResponse(
                call: Call<List<WeightRecord>>,
                response: Response<List<WeightRecord>>
            ) {
                if (response.isSuccessful) {
                    val records = response.body() ?: emptyList()
                    if (records.isEmpty()) {
                        Log.w("ChartActivity", "⚠️ 無任何體重紀錄資料")
                    }
                    WeightLineChartHelper.setupLineChart(lineChart, records)
                    WeightPieChartHelper.setupPieChart(pieChart, records)
                } else {
                    Log.e("ChartActivity", "❌ 資料讀取失敗 code=${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<WeightRecord>>, t: Throwable) {
                Log.e("ChartActivity", "❌ 連線失敗: ${t.message}")
            }
        })
    }
}
