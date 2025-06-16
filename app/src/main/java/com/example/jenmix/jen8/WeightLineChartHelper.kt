package com.example.jenmix.jen8

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

object WeightLineChartHelper {

    fun setupLineChart(chart: LineChart, records: List<WeightRecord>) {
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        // 時間格式化
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        records.sortedBy { it.measuredAt }.forEachIndexed { index, record ->
            entries.add(Entry(index.toFloat(), record.weight))
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            labels.add(sdf.format(inputFormat.parse(record.measuredAt)!!))
        }

        val dataSet = LineDataSet(entries, "體重 (kg)").apply {
            color = Color.parseColor("#3F51B5")
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            setDrawFilled(true)
            fillColor = Color.parseColor("#BBDEFB")
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.textColor = Color.BLACK
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.granularity = 1f
            xAxis.labelRotationAngle = -45f
            xAxis.textColor = Color.BLACK
            animateX(1000)
            invalidate()
        }
    }
}
