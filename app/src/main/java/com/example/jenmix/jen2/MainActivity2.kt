package com.example.jenmix.jen2
import com.example.jenmix.R
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class MainActivity2 : AppCompatActivity() {

    private lateinit var btnFetch: Button
    private lateinit var tvSelectedTime: TextView
    private lateinit var spinnerMedication: Spinner
    private lateinit var btnSetReminder: Button
    private lateinit var lvItems: ListView
    private lateinit var swToggleMedication: Switch
    private lateinit var swTextZoom: Switch
    private lateinit var reminderDao: MedicationReminderDao
    private lateinit var username: String

    // 從後端抓到的全部藥物
    private var allMedications: List<Medication> = emptyList()

    // 顯示在 ListView 的資料：
    // medicationItems 用於顯示藥物清單（需按下「藥物讀取」後填入）
    // customReminderItems 為使用者設定的提醒資料
    private val medicationItems = mutableListOf<ReminderItem>()
    private val customReminderItems = mutableListOf<ReminderItem>()

    private lateinit var reminderAdapter: ReminderAdapter

    private var selectedHour = -1
    private var selectedMinute = -1

    // 權限請求代碼
    private val VIBRATE_PERMISSION_REQUEST_CODE = 1001
    private val NOTIFICATIONS_PERMISSION_REQUEST_CODE = 1002

    // 定義與資料庫對應的「藥物類型」順序
    private val typeOrder = listOf(
        "高血壓藥",
        "低血壓藥",
        "高脈搏藥",
        "低脈搏藥",
        "體重過高藥",
        "體重過低藥",
        "高肌力藥",
        "低肌力藥"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        reminderDao = AppDatabase2.getDatabase(this).reminderDao()
        username = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("username", "") ?: ""

        btnFetch = findViewById(R.id.btnFetch)
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        spinnerMedication = findViewById(R.id.spinnerMedication)
        btnSetReminder = findViewById(R.id.btnSetReminder)
        lvItems = findViewById(R.id.lvReminders)
        swToggleMedication = findViewById(R.id.swToggleMedication)

        swTextZoom = findViewById(R.id.swTextZoom)
        swTextZoom.setOnCheckedChangeListener { _, isChecked ->
            setAllTextSizes(findViewById(android.R.id.content), isChecked)
        }

        // 初始化 ListView Adapter
        reminderAdapter = ReminderAdapter(this, customReminderItems.toMutableList()) { item ->
            cancelCustomReminder(item)
        }
        lvItems.adapter = reminderAdapter

        // Switch 切換 → 根據狀態更新 ListView
        swToggleMedication.setOnCheckedChangeListener { _, _ ->
            updateDisplayedItems()
        }

        checkPermissions()
        checkExactAlarmPermission()
        ReminderUtils.createNotificationChannel(this)

        // (A) 一開始就抓取藥物 (只用於 Spinner，medicationItems 不填入)
        fetchAllMedications()

        // 點擊 TextView → 選擇提醒時間
        tvSelectedTime.setOnClickListener {
            showMaterialTimePicker()
        }

        // 設定提醒：使用者可立即選藥物並設定提醒
        btnSetReminder.setOnClickListener {
            if (selectedHour == -1 || selectedMinute == -1) {
                Toast.makeText(this, "請先選擇提醒時間", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedText = spinnerMedication.selectedItem?.toString() ?: ""
            if (selectedText.isNotEmpty() && selectedText.startsWith("【")) {
                Toast.makeText(this, "請選擇有效的藥物", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val med = allMedications.find { it.name == selectedText }
            if (med == null) {
                Toast.makeText(this, "找不到此藥物", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setCustomReminder(med)
        }

        // (B) 按下「藥物讀取」後才把 allMedications 填入 medicationItems，更新 ListView（依 Switch 狀態）
        btnFetch.setOnClickListener {
            loadMedicationItems()
        }
    }

    // ✅ 每次進入都重新讀取該帳號資料
    override fun onStart() {
        super.onStart()
        val currentUsername = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("username", "") ?: ""

        if (currentUsername != username) {
            username = currentUsername
        }
        lifecycleScope.launch {
            val saved = reminderDao.getAllByUsername(username)
            customReminderItems.clear()
            for (r in saved) {
                customReminderItems.add(
                    ReminderItem(
                        med = Medication(
                            id = r.requestCode / 10000,
                            name = r.name,
                            type = r.type,
                            dosage = r.dosage,
                            ingredients = r.ingredients,
                            contraindications = r.contraindications,
                            side_effects = r.sideEffects,
                            source_url = r.sourceUrl
                        ),
                        reminderTime = r.reminderTime,
                        requestCode = r.requestCode
                    )
                )
            }
            updateDisplayedItems()
            reminderAdapter.notifyDataSetChanged() // ✅ 強制刷新
        }
    }

    /**
     * 一開始就抓取藥物，讓 Spinner 有資料可選
     * (medicationItems 暫時不填入)
     */
    private fun fetchAllMedications() {
        RetrofitClient.apiService.getMedications().enqueue(object : Callback<List<Medication>> {
            override fun onResponse(call: Call<List<Medication>>, response: Response<List<Medication>>) {
                if (response.isSuccessful) {
                    val meds = response.body() ?: emptyList()
                    allMedications = meds
                    // 依 typeOrder 排序後，更新 Spinner
                    setupSpinner(meds)
                } else {
                    Toast.makeText(this@MainActivity2, "獲取失敗: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Medication>>, t: Throwable) {
                Toast.makeText(this@MainActivity2, "網路錯誤: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * 按下「藥物讀取」後，將 allMedications 填入 medicationItems
     * 並根據 Switch 狀態決定是否更新 ListView
     */
    private fun loadMedicationItems() {
        medicationItems.clear()
        val groupedMap = allMedications.groupBy { it.type }
        typeOrder.forEach { typeName ->
            groupedMap[typeName]?.forEach { med ->
                medicationItems.add(
                    ReminderItem(
                        med = med,
                        reminderTime = "", // 此為藥物資料，不顯示提醒時間
                        requestCode = -1
                    )
                )
            }
        }
        Toast.makeText(this, "藥物清單已載入", Toast.LENGTH_SHORT).show()
        updateDisplayedItems()
    }

    /**
     * 為 Spinner 建立分組清單（不加 Emoji，維持原樣）
     */
    private fun setupSpinner(meds: List<Medication>) {
        val groupedMap = meds.groupBy { it.type }
        val groupedItems = mutableListOf<String>()
        typeOrder.forEach { typeName ->
            if (groupedMap.containsKey(typeName)) {
                groupedItems.add("【$typeName】")
                groupedItems.addAll(groupedMap[typeName]!!.map { it.name })
            }
        }
        val adapter = GroupSpinnerAdapter(this, groupedItems)
        spinnerMedication.adapter = adapter
    }

    private fun resizeHourMinuteLabels(view: View) {
        if (view is TextView) {
            val text = view.text?.toString()?.trim()
            if (text == "Hour" || text == "Minute") {
                view.textSize = 13f
                view.setTypeface(null, Typeface.BOLD)
                view.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                resizeHourMinuteLabels(view.getChildAt(i))
            }
        }
    }

    /**
     * 顯示時間選擇器
     */
    private fun showMaterialTimePicker() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
            .setTitleText("選擇提醒時間")
            .build()

        picker.addOnPositiveButtonClickListener {
            selectedHour = picker.hour
            selectedMinute = picker.minute
            tvSelectedTime.text = String.format("提醒時間：%02d:%02d", selectedHour, selectedMinute)
        }

        // 先 show 出來
        picker.show(supportFragmentManager, "tag_material_time_picker")

        // 使用 Handler 延遲放大 Dialog（這樣 dialog 就不會是 null）
        // ✨ 延遲讓 Dialog 建立後，遞迴搜尋 TextView 並放大第一個包含 "選擇提醒時間" 的標題
        Handler(Looper.getMainLooper()).postDelayed({
            val dialog = picker.dialog ?: return@postDelayed
            val window = dialog.window ?: return@postDelayed

            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.55).toInt()
            )

            val rootView = window.decorView

            rootView?.let { view ->
                val titleView = findTextViewWithText(view, "選擇提醒時間")
                titleView?.apply {
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                }

                resizeHourMinuteLabels(view)
            }
        }, 150)
    }

    private fun setAllTextSizes(view: View, enlarge: Boolean) {
        if (view is TextView) {
            val tagKey = R.id.text_size_tag  // ❗請先在 res/values/ids.xml 定義這個 ID

            // 儲存初始大小（只存一次）
            if (view.getTag(tagKey) == null) {
                val originalSizeSp = view.textSize / resources.displayMetrics.scaledDensity
                view.setTag(tagKey, originalSizeSp)
            }

            val baseSize = view.getTag(tagKey) as Float
            val scale = if (enlarge) 1.3f else 1.0f
            view.textSize = baseSize * scale
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setAllTextSizes(view.getChildAt(i), enlarge)
            }
        }
    }

    // 🔍 遞迴找出包含特定文字的 TextView
    private fun findTextViewWithText(view: View, text: String): TextView? {
        if (view is TextView && view.text.toString().contains(text)) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findTextViewWithText(view.getChildAt(i), text)
                if (result != null) return result
            }
        }
        return null
    }

    /**
     * 設定提醒
     * 提醒觸發後 ReminderReceiver 會重新排程下一次提醒（24 小時後）
     */
    private fun setCustomReminder(med: Medication) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val uniqueRequestCode = med.id * 10000 + selectedHour * 100 + selectedMinute
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("med_id", med.id)
            putExtra("med_name", med.name)
            putExtra("med_type", med.type)
            putExtra("med_dosage", med.dosage)
            putExtra("med_ingredients", med.ingredients)
            putExtra("med_contraindications", med.contraindications)
            putExtra("med_side_effects", med.side_effects)
            putExtra("med_source_url", med.source_url)
            putExtra("reminder_time", String.format("%02d:%02d", selectedHour, selectedMinute))
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            uniqueRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            // 使用 setExactAndAllowWhileIdle 排程精準鬧鐘
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Toast.makeText(this, "提醒已設定: ${med.name} @ ${calendar.time}", Toast.LENGTH_SHORT).show()
            customReminderItems.add(
                ReminderItem(
                    med = med,
                    reminderTime = String.format("%02d:%02d", selectedHour, selectedMinute),
                    requestCode = uniqueRequestCode
                )
            )
            updateDisplayedItems()
        } catch (se: SecurityException) {
            Log.e("MainActivity", "無法設定精準鬧鐘: ${se.message}")
            Toast.makeText(this, "需要開啟精準鬧鐘權限才能設定提醒", Toast.LENGTH_SHORT).show()
        }
        // 在 setCustomReminder() 結尾加上：
        lifecycleScope.launch {
            reminderDao.insert(
                MedicationReminderEntity(
                    requestCode = uniqueRequestCode,
                    name = med.name,
                    type = med.type,
                    dosage = med.dosage,
                    ingredients = med.ingredients,
                    contraindications = med.contraindications,
                    sideEffects = med.side_effects,
                    sourceUrl = med.source_url,
                    reminderTime = String.format("%02d:%02d", selectedHour, selectedMinute),
                    username = username  // ✅ 傳入帳號
                )
            )
        }

    }

    /**
     * 取消提醒
     */
    private fun cancelCustomReminder(item: ReminderItem) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            item.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "提醒已刪除: ${item.med.name} @ ${item.reminderTime}", Toast.LENGTH_SHORT).show()
        customReminderItems.remove(item)
        updateDisplayedItems()
        // 在 cancelCustomReminder(item) 裡加上：
        lifecycleScope.launch {
            reminderDao.deleteByRequestCode(item.requestCode, username)
        }
    }

    /**
     * 根據 Switch 狀態：
     *  開啟 → 顯示 medicationItems (藥物清單)
     *  關閉 → 顯示 customReminderItems (提醒)
     */
    private fun updateDisplayedItems() {
        if (swToggleMedication.isChecked) {
            reminderAdapter.setItems(medicationItems)
        } else {
            reminderAdapter.setItems(customReminderItems)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.VIBRATE), VIBRATE_PERMISSION_REQUEST_CODE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATIONS_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            VIBRATE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "震動權限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "震動權限被拒絕", Toast.LENGTH_SHORT).show()
                }
            }
            NOTIFICATIONS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "通知權限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "通知權限被拒絕", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
