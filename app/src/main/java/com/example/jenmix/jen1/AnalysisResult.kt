package com.example.jenmix.jen1

data class AnalysisResult(
    val user: String,
    val age: Int?,
    val gender:String?,
    val record_date: String,
    val 風險等級: String,
    val 分析結果: List<String>,
    val 交叉狀況: List<String>,
    val 可能病症: List<String>,
    val 建議: List<String>
)

