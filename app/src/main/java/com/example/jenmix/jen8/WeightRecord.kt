package com.example.jenmix.jen8

import com.google.gson.annotations.SerializedName

data class WeightRecord(
    val id: Int,
    val username: String,
    val gender: String,
    val height: Float,
    val age: Int,
    val weight: Float,

    @SerializedName("measured_at")
    val measuredAt: String
)
