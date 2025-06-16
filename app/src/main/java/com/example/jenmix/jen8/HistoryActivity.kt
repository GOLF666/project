package com.example.jenmix.jen8

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jenmix.databinding.ActivityHistoryBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentDate = Calendar.getInstance()
    private var currentMode = "week"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)

        binding.btnWeek.setOnClickListener { currentMode = "week"; loadData() }
        binding.btnMonth.setOnClickListener { currentMode = "month"; loadData() }
        binding.btnYear.setOnClickListener { currentMode = "year"; loadData() }

        binding.btnPrev.setOnClickListener {
            when (currentMode) {
                "week" -> currentDate.add(Calendar.WEEK_OF_YEAR, -1)
                "month" -> currentDate.add(Calendar.MONTH, -1)
                "year" -> currentDate.add(Calendar.YEAR, -1)
            }
            loadData()
        }

        binding.btnNext.setOnClickListener {
            when (currentMode) {
                "week" -> currentDate.add(Calendar.WEEK_OF_YEAR, 1)
                "month" -> currentDate.add(Calendar.MONTH, 1)
                "year" -> currentDate.add(Calendar.YEAR, 1)
            }
            loadData()
        }

        binding.btnPickDate.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setStart(Calendar.getInstance().apply { set(1900, Calendar.JANUARY, 1) }.timeInMillis)
                .setEnd(Calendar.getInstance().apply { set(2100, Calendar.DECEMBER, 31) }.timeInMillis)
                .build()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("選擇紀錄日期")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraints)
                .build()

            picker.show(supportFragmentManager, picker.toString())

            picker.addOnPositiveButtonClickListener { selection ->
                val selectedDate = Date(selection)
                val dateStr = sdf.format(selectedDate)
                fetchSingleDay(dateStr)
            }
        }

        loadData()
    }

    private fun loadData() {
        val (start, end) = getDateRange(currentDate, currentMode)
        binding.tvSelectedDate.text = if (start == end) start else "$start ~ $end"

        RetrofitClient.getInstance()
            .create(HistoryApi::class.java)
            .getWeightHistory(start, end)
            .enqueue(object : Callback<List<WeightRecord>> {
                override fun onResponse(call: Call<List<WeightRecord>>, response: Response<List<WeightRecord>>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@HistoryActivity, "資料載入失敗", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val records = response.body().orEmpty()
                    adapter = HistoryAdapter(records)
                    binding.recyclerHistory.adapter = adapter
                }

                override fun onFailure(call: Call<List<WeightRecord>>, t: Throwable) {
                    Toast.makeText(this@HistoryActivity, "伺服器錯誤", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchSingleDay(dateStr: String) {
        binding.tvSelectedDate.text = "$dateStr 的紀錄"

        RetrofitClient.getInstance()
            .create(HistoryApi::class.java)
            .getWeightHistory(dateStr, dateStr)
            .enqueue(object : Callback<List<WeightRecord>> {
                override fun onResponse(call: Call<List<WeightRecord>>, response: Response<List<WeightRecord>>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@HistoryActivity, "查詢失敗", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val records = response.body().orEmpty()
                    adapter = HistoryAdapter(records)
                    binding.recyclerHistory.adapter = adapter
                }

                override fun onFailure(call: Call<List<WeightRecord>>, t: Throwable) {
                    Toast.makeText(this@HistoryActivity, "伺服器錯誤", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getDateRange(base: Calendar, mode: String): Pair<String, String> {
        val start = base.clone() as Calendar
        val end = base.clone() as Calendar
        when (mode) {
            "week" -> {
                start.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                end.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            }
            "month" -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, start.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            "year" -> {
                start.set(Calendar.MONTH, Calendar.JANUARY)
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.MONTH, Calendar.DECEMBER)
                end.set(Calendar.DAY_OF_MONTH, 31)
            }
        }
        return sdf.format(start.time) to sdf.format(end.time)
    }
}
