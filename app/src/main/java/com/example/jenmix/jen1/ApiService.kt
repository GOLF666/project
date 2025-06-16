package com.example.jenmix.jen1

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("analyzeSingle")
    fun getSingleAnalysis(
        @Query("user") user: String,
        @Query("date") date: String
    ): Call<AnalysisResult>

    /**
     * 取得總體健康分析（平均所有紀錄）
     * @param user 使用者名稱
     */
    @GET("analyzeAllAggregate")
    fun getAllAggregate(
        @Query("user") user: String
    ): Call<AnalysisResult>

    /**
     * 取得區間分析資料（指定起始日期至今）
     * @param user 使用者名稱
     * @param startDate 起始日期 yyyy-MM-dd（例如：7 天前）
     */
    @GET("analyzeRange")
    fun getRangeAnalysis(
        @Query("user") user: String,
        @Query("start_date") startDate: String
    ): Call<AnalysisResult>

    @GET("get_records")
    fun getRecords(
        @Query("user") user: String
    ): Call<List<HealthRecord>>

    /**
     * 取得指定使用者所有可用分析日期
     * @param userName 使用者名稱
     */
    @GET("dates/{user_name}")
    fun getAvailableDates(
        @Path("user_name") userName: String
    ): Call<List<String>>

    @GET("/get_source_url")
    fun getSourceUrl(@Query("disease") disease: String): Call<UrlResponse>

    @GET("get_users")
    fun getUsers(): Call<List<String>>

    @GET("analyze/custom_range")
    fun getCustomRangeAnalysis(
        @Query("user") user: String,
        @Query("start") start: String,
        @Query("end") end: String
    ): Call<AnalysisResult>
   }
