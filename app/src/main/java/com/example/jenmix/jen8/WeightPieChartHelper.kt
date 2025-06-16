package com.example.jenmix.jen8

import android.graphics.Color
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

object WeightPieChartHelper {

    fun setupPieChart(chart: PieChart, records: List<WeightRecord>) {
        val zoneCount = mutableMapOf(
            "< 60kg" to 0,
            "60-70kg" to 0,
            "70-80kg" to 0,
            "80kg+" to 0
        )

        for (record in records) {
            when {
                record.weight < 60 -> zoneCount["< 60kg"] = zoneCount["< 60kg"]!! + 1
                record.weight < 70 -> zoneCount["60-70kg"] = zoneCount["60-70kg"]!! + 1
                record.weight < 80 -> zoneCount["70-80kg"] = zoneCount["70-80kg"]!! + 1
                else -> zoneCount["80kg+"] = zoneCount["80kg+"]!! + 1
            }
        }

        val entries = zoneCount.map { PieEntry(it.value.toFloat(), it.key) }

        val dataSet = PieDataSet(entries, "體重分布").apply {
            colors = listOf(
                Color.parseColor("#FFCDD2"),
                Color.parseColor("#90CAF9"),
                Color.parseColor("#A5D6A7"),
                Color.parseColor("#FFF59D")
            )
            valueTextSize = 14f
            valueTextColor = Color.BLACK
        }

        chart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            setEntryLabelColor(Color.BLACK)
            setUsePercentValues(true)
            isDrawHoleEnabled = true
            holeRadius = 40f
            transparentCircleRadius = 45f
            animateY(1000)
            invalidate()
        }
    }
}
