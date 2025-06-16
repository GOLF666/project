package com.example.jenmix.jen8

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.jenmix.R

class FirstTimeSetupActivity : AppCompatActivity() {

    private lateinit var btnMale: ImageButton
    private lateinit var btnFemale: ImageButton
    private lateinit var editHeight: AutoCompleteTextView
    private lateinit var editAge: AutoCompleteTextView
    private lateinit var tvRequired: TextView
    private lateinit var btnComplete: Button

    private var selectedGender: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserPrefs.isSetupCompleted(this)) {
            startActivity(Intent(this, MainActivity8::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_first_time_setup)

        // 初始化元件
        btnMale = findViewById(R.id.btnMale)
        btnFemale = findViewById(R.id.btnFemale)
        editHeight = findViewById(R.id.editHeight)
        editAge = findViewById(R.id.editAge)
        tvRequired = findViewById(R.id.tvRequired)
        btnComplete = findViewById(R.id.btnComplete)

        // 性別按鈕點擊
        btnMale.setOnClickListener {
            selectedGender = "男"
            btnMale.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gender_male)
            btnFemale.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gray_light)
        }

        btnFemale.setOnClickListener {
            selectedGender = "女"
            btnFemale.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gender_female)
            btnMale.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gray_light)
        }

        // 建立身高與年齡下拉選單
        val heights = (100..250).map { it.toString() }
        editHeight.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, heights))

        val ages = (10..100).map { it.toString() }
        editAge.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ages))

        // 點擊顯示下拉
        editHeight.setOnClickListener { editHeight.showDropDown() }
        editAge.setOnClickListener { editAge.showDropDown() }

        // 點擊完成
        btnComplete.setOnClickListener {
            val height = editHeight.text.toString().toFloatOrNull()
            val age = editAge.text.toString().toIntOrNull()

            if (selectedGender.isEmpty() || height == null || age == null) {
                tvRequired.text = "請完整填寫所有欄位"
                return@setOnClickListener
            }

            tvRequired.text = ""
            showConfirmDialog(selectedGender, height, age)
        }
    }

    private fun showConfirmDialog(gender: String, height: Float, age: Int) {
        AlertDialog.Builder(this)
            .setTitle("確認資料")
            .setMessage("是否已確認您的基本資料填寫無誤？")
            .setPositiveButton("確認") { _, _ ->
                UserPrefs.saveUserInfo(this, gender, height, age)
                Toast.makeText(this, "資料已儲存", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity8::class.java))
                finish()
            }
            .setNegativeButton("再次檢查", null)
            .show()
    }
}
