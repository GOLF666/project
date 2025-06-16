package com.example.jenmix.jen4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import com.example.jenmix.R
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.launch

class MainActivity4 : AppCompatActivity() {

    private val REQUEST_CODE_NOTIFICATION_PERMISSION = 1
    private val reminderList = mutableListOf<Reminder>()
    private val filteredList = mutableListOf<Reminder>()
    private lateinit var reminderAdapter: ReminderAdapter
    private lateinit var spinnerCategory: Spinner
    private var selectedCategory: String = Category.ALL.value
    private lateinit var reminderDao: GeneralReminderDao
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main4)

        // 請求通知權限（Android 13 以上版本）
        requestNotificationPermission()
        reminderDao = AppDatabase4.getDatabase(this).generalReminderDao()

        username = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("username", "") ?: ""

        // 初始化介面元件
        val btnAddReminder: Button = findViewById(R.id.btnAddReminder)
        val rvReminders: RecyclerView = findViewById(R.id.rvReminders)
        spinnerCategory = findViewById(R.id.spinnerCategory)

        lifecycleScope.launch {
            val savedReminders = reminderDao.getAllByUsername(username)
            reminderList.addAll(savedReminders.map {
                Reminder(
                    id = it.id,
                    hour = it.hour,
                    minute = it.minute,
                    category = it.category,
                    dayOfWeek = it.dayOfWeek,
                    title = it.title,
                    content = it.content,
                    isRepeat = it.isRepeat
                )
            })
            filterReminders()
        }

        // 設定 RecyclerView 與 Adapter
        reminderAdapter = ReminderAdapter(
            filteredList,
            onReminderDelete = { reminder -> deleteReminder(reminder) },
            onReminderTimeChanged = { reminder -> updateReminderTime(reminder) },
            onReminderEdit = { reminder -> showAddReminderDialog(reminder) }
        )
        rvReminders.layoutManager = LinearLayoutManager(this)
        rvReminders.adapter = reminderAdapter

        // 設定上方類別篩選 Spinner（使用 enum 統一管理）
        val categories = Category.values().map { it.value }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = adapter
        spinnerCategory.setSelection(0)
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
                filterReminders()
            }
            override fun onNothingSelected(parentView: AdapterView<*>?) {
                selectedCategory = Category.ALL.value
                filterReminders()
            }
        }

        // 新增提醒按鈕點擊事件
        btnAddReminder.setOnClickListener { showAddReminderDialog() }
    }

    // 請求通知權限（Android 13+）
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    /**
     * 顯示新增／修改提醒對話框
     */
    private fun showAddReminderDialog(reminder: Reminder? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder4, null)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val spinnerCategoryDialog = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerCategory)
        val spinnerDayOfWeek = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerDayOfWeek)

        // 提醒類別（不包含吃藥）
        val categories = arrayOf(Category.BLOOD_PRESSURE.value, Category.WEIGHT.value, Category.WATER.value, Category.OTHER.value)
        spinnerCategoryDialog.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, categories))

        // 星期選擇 Spinner（第一項為每天）
        val days = arrayOf("每天", "周日", "周一", "周二", "周三", "周四", "周五", "周六")
        spinnerDayOfWeek.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, days))

        // 若為修改提醒則預填原有資料
        reminder?.let {
            timePicker.hour = it.hour
            timePicker.minute = it.minute
            spinnerCategoryDialog.setText(it.category, false)

            // dayOfWeek 為 null 或 -1 表示「每天」
            val daySelection = when (it.dayOfWeek) {
                null, -1 -> 0
                0 -> 1
                1 -> 2
                2 -> 3
                3 -> 4
                4 -> 5
                5 -> 6
                6 -> 7
                else -> 0
            }
            spinnerDayOfWeek.setText(days[daySelection], false)
        }

        AlertDialog.Builder(this)
            .setTitle(if (reminder == null) "新增提醒" else "修改提醒")
            .setView(dialogView)
            .setPositiveButton("確定") { _, _ ->
                val hour = timePicker.hour
                val minute = timePicker.minute
                val category = spinnerCategoryDialog.text.toString()
                val dayOfWeek = when (spinnerDayOfWeek.text.toString()) {
                    "每天" -> null
                    "周日" -> 0
                    "周一" -> 1
                    "周二" -> 2
                    "周三" -> 3
                    "周四" -> 4
                    "周五" -> 5
                    "周六" -> 6
                    else -> null
                }

                // 這裡的提醒標題與內容固定
                val titleText = "提醒時間到了"
                val contentText = "請打開APP"

                if (reminder == null) {
                    Toast.makeText(this, "✅提醒已新增", Toast.LENGTH_SHORT).show()
                    // 新增提醒，利用當前時間產生唯一 id
                    val newReminder = Reminder(
                        id = System.currentTimeMillis().toInt(),
                        hour = hour,
                        minute = minute,
                        category = category,
                        dayOfWeek = dayOfWeek,
                        title = titleText,
                        content = contentText,
                        isRepeat = false
                    )
                    reminderList.add(newReminder)
                    scheduleReminder(newReminder)

                    // 🧠 同步儲存到 Room 資料庫
                    lifecycleScope.launch {
                        reminderDao.insert(
                            GeneralReminderEntity(
                                id = newReminder.id,
                                hour = newReminder.hour,
                                minute = newReminder.minute,
                                category = newReminder.category,
                                dayOfWeek = newReminder.dayOfWeek,
                                title = newReminder.title,
                                content = newReminder.content,
                                isRepeat = newReminder.isRepeat,
                                username = username
                            )
                        )
                    }
                } else {
                    // 修改提醒時保留原有 id
                    val updatedReminder = reminder.copy(
                        hour = hour,
                        minute = minute,
                        category = category,
                        dayOfWeek = dayOfWeek,
                        title = titleText,
                        content = contentText
                    )

                    lifecycleScope.launch {
                        reminderDao.insert(
                            GeneralReminderEntity(
                                id = updatedReminder.id,
                                hour = updatedReminder.hour,
                                minute = updatedReminder.minute,
                                category = updatedReminder.category,
                                dayOfWeek = updatedReminder.dayOfWeek,
                                title = updatedReminder.title,
                                content = updatedReminder.content,
                                isRepeat = updatedReminder.isRepeat,
                                username = username
                            )
                        )
                    }
                    val index = reminderList.indexOfFirst { it.id == reminder.id }
                    if (index != -1) {
                        reminderList[index] = updatedReminder
                    }
                    scheduleReminder(updatedReminder)
                    Toast.makeText(this, "✏\uFE0F提醒已更新", Toast.LENGTH_SHORT).show()
                }
                filterReminders()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 利用 WorkManager 安排提醒排程
     */
    private fun scheduleReminder(reminder: Reminder) {
        val nextTriggerTime = ReminderScheduler.calculateNextTriggerTime(reminder)
        val initialDelay = nextTriggerTime - System.currentTimeMillis()
        if (initialDelay <= 0) {
            // 若初始延遲時間非正數，則直接跳過
            return
        }

        val workData = workDataOf(
            "id" to reminder.id,
            "hour" to reminder.hour,
            "minute" to reminder.minute,
            "category" to reminder.category,
            "isRepeat" to reminder.isRepeat,
            "dayOfWeek" to (reminder.dayOfWeek ?: -1)
        )

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workData)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        // 使用 reminder.id 產生獨一無二的工作名稱
        val uniqueWorkName = "reminder_${reminder.id}"
        WorkManager.getInstance(this).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun deleteReminder(reminder: Reminder) {
        reminderList.removeAll { it.id == reminder.id }
        filterReminders()
        Toast.makeText(this, "\uD83D\uDDD1\uFE0F提醒已刪除", Toast.LENGTH_SHORT).show()
        val uniqueWorkName = "reminder_${reminder.id}"
        WorkManager.getInstance(this).cancelUniqueWork(uniqueWorkName)

        lifecycleScope.launch {
            reminderDao.deleteByIdAndUsername(reminder.id, username)
        }
    }

    private fun updateReminderTime(reminder: Reminder) {
        scheduleReminder(reminder)
        filterReminders()
    }

    private fun filterReminders() {
        filteredList.clear()
        filteredList.addAll(
            if (selectedCategory == Category.ALL.value) {
                reminderList
            } else {
                reminderList.filter { it.category == selectedCategory }
            }
        )
        reminderAdapter.notifyDataSetChanged()
    }

    // 利用 enum 統一管理提醒類別（不含吃藥）
    enum class Category(val value: String) {
        ALL("全部"),
        BLOOD_PRESSURE("測量血壓"),
        WEIGHT("測量體重"),
        WATER("喝水"),
        MEDICATION("吃藥"),
        OTHER("其他")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通知權限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "通知權限被拒絕", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
