package com.example.jenmix.jen8

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jenmix.R
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val records: List<WeightRecord>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWeight: TextView = view.findViewById(R.id.tvWeight)
        val tvDiff: TextView = view.findViewById(R.id.tvDiff)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_record, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = records.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]

        // 原始字串格式：2025-05-26 15:26:20
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val formattedTime = try {
            outputFormat.format(inputFormat.parse(record.measuredAt)!!)
        } catch (e: Exception) {
            "解析錯誤"
        }

        holder.tvWeight.text = String.format("%.1f kg", record.weight)

        if (position < records.size - 1) {
            val prevWeight = records[position + 1].weight
            val diff = record.weight - prevWeight
            val diffText = if (diff > 0) {
                "+%.1f kg".format(diff)
            } else if (diff < 0) {
                "%.1f kg".format(diff)
            } else {
                "0.0 kg"
            }
            holder.tvDiff.text = diffText
            holder.tvDiff.setTextColor(
                when {
                    diff > 0 -> Color.parseColor("#F44336")
                    diff < 0 -> Color.parseColor("#4CAF50")
                    else -> Color.DKGRAY
                }
            )
        } else {
            holder.tvDiff.text = "-"
            holder.tvDiff.setTextColor(Color.DKGRAY)
        }

        holder.tvTime.text = formattedTime
    }
}