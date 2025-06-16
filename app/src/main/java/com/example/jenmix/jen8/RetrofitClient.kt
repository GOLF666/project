package com.example.jenmix.jen8

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://10.11.246.191:3000/"  // ← 根據你的實際 IP
    
    // ✅ 統一使用一個 Retrofit instance，並使用自訂日期格式
    private val retrofit: Retrofit by lazy {
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun getInstance(): Retrofit = retrofit
}
