package com.example.jenmix.jen8

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface HistoryApi {

    // 查詢特定時間區間資料（如歷史紀錄畫面用）
    @GET("weight-history")
    fun getWeightHistory(
        @Query("start") startDate: String,
        @Query("end") endDate: String
    ): Call<List<WeightRecord>>

    // 查詢全資料（未指定條件）
    @GET("weight-history")
    fun getAllRecords(): Call<List<WeightRecord>>

    // ✅ 新增這個方法：用於圖表查詢特定使用者資料
    @GET("weight-history")
    fun getChartData(
        @Query("username") username: String
    ): Call<List<WeightRecord>>
}
