package com.example.jenmix.jen9

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.jenmix.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity9 : AppCompatActivity() {

    private var selectedTimeInMillis: Long = 0L
    private var editingReminderTime: Long = 0L
    lateinit var reminderCard: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main9)

        val db = AppDatabase.getDatabase(this)
        val reminderDao = db.reminderDao()
        val username = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("username", "") ?: ""

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        val etDepartment = findViewById<EditText>(R.id.etDepartment)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnPickDate = findViewById<Button>(R.id.btnPickDate)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val tvSelectedTime = findViewById<TextView>(R.id.tvSelectedTime)
        val reminderListLayout = findViewById<LinearLayout>(R.id.reminderListLayout)
        val tvReminderListTitle = findViewById<TextView>(R.id.tvReminderListTitle)

        lifecycleScope.launch {
            val reminders = reminderDao.getRemindersByUsername(username)
            for (reminder in reminders) {
                addReminderCard(
                    username,
                    reminder.title,
                    reminder.location,
                    reminder.department,
                    reminder.note,
                    reminder.timeInMillis,
                    reminderDao,
                    reminderListLayout,
                    tvReminderListTitle
                )
            }
        }

        btnPickDate.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                TimePickerDialog(this, { _, h, min ->
                    val cal = Calendar.getInstance()
                    cal.set(y, m, d, h, min)
                    selectedTimeInMillis = cal.timeInMillis

                    val selectedText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(cal.time)
                    Toast.makeText(this, "已選擇 $selectedText", Toast.LENGTH_SHORT).show()

                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val department = etDepartment.text.toString().trim()
            val note = etNote.text.toString().trim()

            if (title.isEmpty() || location.isEmpty() || selectedTimeInMillis == 0L) {
                Toast.makeText(this, "請輸入標題、地點並選擇時間", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val appointmentTime = sdf.format(Date(selectedTimeInMillis))

            val calDayBefore = Calendar.getInstance().apply {
                timeInMillis = selectedTimeInMillis
                add(Calendar.DAY_OF_MONTH, -1)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val oneDayBeforeTime = calDayBefore.timeInMillis
            if (oneDayBeforeTime > System.currentTimeMillis()) {
                scheduleReminder(applicationContext, oneDayBeforeTime, "$title（前一天提醒）", location)
            }

            val calMorning = Calendar.getInstance().apply {
                timeInMillis = selectedTimeInMillis
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val sameDayMorningTime = calMorning.timeInMillis
            if (sameDayMorningTime > System.currentTimeMillis()) {
                scheduleReminder(applicationContext, sameDayMorningTime, "$title（當日提醒）", location)
            }

            tvSelectedTime.text = "✅ 行程已設定提醒\n🕒 時間：$appointmentTime\n📍 地點：$location\n📌 科別：$department\n📝 備註：$note"
            tvSelectedTime.visibility = View.VISIBLE

            etTitle.text.clear()
            etLocation.text.clear()
            etDepartment.text.clear()
            etNote.text.clear()

            tvReminderListTitle.visibility = View.VISIBLE
            reminderListLayout.visibility = View.VISIBLE

            val thisReminderTime = selectedTimeInMillis
            selectedTimeInMillis = 0L

            lifecycleScope.launch {
                if (editingReminderTime != 0L) {
                    reminderDao.deleteByTime(editingReminderTime)
                    editingReminderTime = 0L
                }
                reminderDao.insert(
                    ReminderEntity(
                        username = username, // ✅ 加這行
                        title = title,
                        location = location,
                        department = department,
                        note = note,
                        timeInMillis = thisReminderTime
                    )
                )
                reminderListLayout.removeAllViews()
                val newList = reminderDao.getRemindersByUsername(username)
                for (r in newList) {
                    addReminderCard(
                        username,
                        r.title, r.location, r.department, r.note, r.timeInMillis,
                        reminderDao, reminderListLayout, tvReminderListTitle
                    )
                }
            }

            Toast.makeText(this, "行程已設定提醒！", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addReminderCard(
        username: String, // ✅ 加上這個參數
        title: String,
        location: String,
        department: String,
        note: String,
        timeInMillis: Long,
        dao: ReminderDao,
        layout: LinearLayout,
        tvTitle: TextView
    ) {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.card_reminder, layout, false) as CardView

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val timeText = sdf.format(Date(timeInMillis))

        cardView.findViewById<TextView>(R.id.tvTime).text = "🕒 $timeText"
        cardView.findViewById<TextView>(R.id.tvTitle).text = "📌 行程：$title"
        cardView.findViewById<TextView>(R.id.tvLocation).text = "📍 地點：$location"
        cardView.findViewById<TextView>(R.id.tvDepartment).text = "🏥 科別：$department"
        cardView.findViewById<TextView>(R.id.tvNote).text = "📝 備註：$note"

        val btnDelete = cardView.findViewById<Button>(R.id.btnDelete)
        btnDelete.setOnClickListener {
            lifecycleScope.launch {
                dao.deleteByUsernameTitleTime(username, title, timeInMillis)
                cancelReminder(applicationContext, title, timeInMillis)
            }
            layout.removeView(cardView)
            if (layout.childCount == 0) {
                tvTitle.visibility = View.GONE
                layout.visibility = View.GONE
            }
            Toast.makeText(this, "✅ 已成功刪除提醒", Toast.LENGTH_SHORT).show()
        }

        val btnEdit = cardView.findViewById<Button>(R.id.btnEdit)
        btnEdit.setOnClickListener {
            val etTitle = findViewById<EditText>(R.id.etTitle)
            val etLocation = findViewById<EditText>(R.id.etLocation)
            val etDepartment = findViewById<EditText>(R.id.etDepartment)
            val etNote = findViewById<EditText>(R.id.etNote)

            etTitle.setText(title)
            etLocation.setText(location)
            etDepartment.setText(department)
            etNote.setText(note)
            selectedTimeInMillis = timeInMillis
            editingReminderTime = timeInMillis
            Toast.makeText(this, "請修改完後重新儲存", Toast.LENGTH_SHORT).show()
        }

        layout.addView(cardView)
        tvTitle.visibility = View.VISIBLE
        layout.visibility = View.VISIBLE
    }

    private fun scheduleReminder(context: Context, timeInMillis: Long, title: String, location: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", "你要去 $location 看診喔～")
        }
        val requestCode = (title + timeInMillis.toString()).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "⚠️ 無法設定提醒，請手動開啟精準鬧鐘權限", Toast.LENGTH_LONG).show()
                val settingsIntent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(settingsIntent)
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }

    private fun cancelReminder(context: Context, title: String, timeInMillis: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = (title + timeInMillis.toString()).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
