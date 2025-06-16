package com.example.jenmix.jen1

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.util.Log
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.example.jenmix.R

class MyMarkerView(
    context: Context,
    layoutResource: Int,
    private val chartRef: LineChart,
    private val gender: String,
    private val heightM: Float
) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)
    private val tvLink: TextView = findViewById(R.id.tvLink)
    private val TAG = "MyMarkerView"

    private val normalValues = mapOf(
        // 🩺 血壓（異常 → 對應正常值範圍）
        "高血壓" to "收縮壓90–120 / 舒張壓60–80",
        "高血壓前期" to "收縮壓90–120 / 舒張壓60–80",
        "血壓偏高" to "收縮壓90–120 / 舒張壓60–80",
        "低血壓" to "收縮壓90–120 / 舒張壓60–80",

        // ❤️‍🔥 脈搏
        "脈搏太高" to "脈搏60–100",
        "高脈搏" to "脈搏60–100",
        "脈搏太低" to "脈搏60–100",
        "低脈搏" to "脈搏60–100",

    )

    private var currentDisease: String = "未知"

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null || highlight == null) return

        val record = e.data as? HealthRecord ?: return
        val label = chartRef.data?.getDataSetByIndex(highlight.dataSetIndex)?.label ?: "未知"

        val disease = when (label) {
            "收縮壓", "舒張壓" -> getBloodPressureAnalysis(record)
            "脈搏" -> getPulseAnalysis(record)
            else -> label
        } ?: "正常值"

        val normalText = normalValues[disease] ?: ""
        tvContent.text = buildString {
            append("📌 病症：$disease\n")
            append("📏 測量值：${"%.1f".format(e.y)}\n")
            if (normalText.isNotBlank()) append("✅ 正常值：$normalText\n")

            if (disease == "高血壓") {
                append("\n📖 高血壓定義：\n")
                append("．兩週內出現 3 次以上\n")
                append("．收縮壓 ≥140 或 舒張壓 ≥90 mmHg")
            }
        }

        currentDisease = disease

        if (disease !in listOf("正常值", "未知")) {
            tvLink.visibility = View.VISIBLE
            tvLink.paintFlags = tvLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG

            super.refreshContent(e, highlight)
        }
    }

    override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -height.toFloat())

    private fun getBloodPressureAnalysis(r: HealthRecord): String? =
        when {
            r.systolic_mmHg >= 140 || r.diastolic_mmHg >= 90 -> "高血壓"
            r.systolic_mmHg in 130..139 || r.diastolic_mmHg in 80..89 -> "高血壓前期"
            r.systolic_mmHg in 120..129 || r.diastolic_mmHg == 80 -> "血壓偏高"
            r.systolic_mmHg < 90 || r.diastolic_mmHg < 60 -> "低血壓"
            else -> null
        }

    private fun getPulseAnalysis(r: HealthRecord): String? =
        when {
            r.pulse_bpm > 120 -> "脈搏太高"
            r.pulse_bpm in 101..120 -> "高脈搏"
            r.pulse_bpm < 50 -> "脈搏太低"
            r.pulse_bpm in 50..59 -> "低脈搏"
            else -> null
        }
    }

