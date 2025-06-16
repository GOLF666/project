package com.example.jenmix.jen8

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jenmix.R

class HealthCardAdapter(private val items: List<HealthItem>) :
    RecyclerView.Adapter<HealthCardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvCardTitle)
        val tvValue: TextView = view.findViewById(R.id.tvCardValue)
        val tvStatus: TextView = view.findViewById(R.id.tvCardStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_analysis_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvValue.text = item.value
        holder.tvStatus.text = item.status
    }

    override fun getItemCount(): Int = items.size
}
