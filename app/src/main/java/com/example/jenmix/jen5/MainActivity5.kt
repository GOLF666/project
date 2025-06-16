package com.example.jenmix.jen5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.jenmix.R

class MainActivity5 : AppCompatActivity() {
    private lateinit var spinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var hospitalAdapter: HospitalAdapter

    private val regions = listOf("台北", "新北", "桃園", "台中", "台南", "高雄")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main5)

        spinner = findViewById(R.id.spinner_region)
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        hospitalAdapter = HospitalAdapter()
        recyclerView.adapter = hospitalAdapter

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, regions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedRegion = regions[position]
                loadHospitals(selectedRegion)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadHospitals(region: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.11.246.191:3000/") // ⬅ 替換為你的伺服器位置
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(HospitalApi::class.java)
        api.getHospitals(region).enqueue(object : Callback<List<Hospital>> {
            override fun onResponse(call: Call<List<Hospital>>, response: Response<List<Hospital>>) {
                if (response.isSuccessful) {
                    hospitalAdapter.submitList(response.body() ?: emptyList())
                } else {
                    Toast.makeText(this@MainActivity5, "無法載入資料", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Hospital>>, t: Throwable) {
                Toast.makeText(this@MainActivity5, "連線失敗: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
