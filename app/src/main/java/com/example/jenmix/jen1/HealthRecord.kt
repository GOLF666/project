package com.example.jenmix.jen1

data class HealthRecord(
    val user: String,
    val age: Int,
    val gender: String,
    val measure_at: String,
    val height: Int,
    val weight: Float,
    val systolic_mmHg: Int,
    val diastolic_mmHg: Int,
    val pulse_bpm: Int
)
