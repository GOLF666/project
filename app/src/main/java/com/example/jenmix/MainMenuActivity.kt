package com.example.jenmix

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.jenmix.hu.SecondActivity
import com.example.jenmix.jen1.MainActivity1
import com.example.jenmix.jen2.MainActivity2
import com.example.jenmix.jen3.MainActivity3
import com.example.jenmix.jen4.MainActivity4
import com.example.jenmix.jen5.MainActivity5
import com.example.jenmix.jen6.MainActivity6
import com.example.jenmix.jen7.MainActivity7
import com.example.jenmix.jen8.MainActivity8
import com.example.jenmix.jen9.MainActivity9
import okhttp3.OkHttpClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import android.widget.Toast
import com.example.jenmix.hu.FourthActivity

class MainMenuActivity : AppCompatActivity() {

    private var keroPlayer: MediaPlayer? = null
    private val client = OkHttpClient()
    private val nodeApiBaseUrl = "http://192.168.0.10:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // ✅ 語錄元件綁定 + 載入
        val quoteText = findViewById<TextView>(R.id.quoteText)
        getDailyQuote(quoteText)

        findViewById<ImageButton>(R.id.btnWeight).setOnClickListener {
            startActivity(Intent(this, MainActivity2::class.java))
        }
        findViewById<ImageButton>(R.id.btnAnxiety).setOnClickListener {
            startActivity(Intent(this, MainActivity3::class.java))
        }
        findViewById<ImageButton>(R.id.btnMedications).setOnClickListener {
            startActivity(Intent(this, MainActivity4::class.java))
        }
        // 这一行改成 btnSecondActivity —— 不要再用 btnBp 了
        findViewById<ImageButton>(R.id.btnSecondActivity).setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnGoToAnalysis).setOnClickListener {
            startActivity(Intent(this, MainActivity1::class.java))
        }

        // 前往「運動建議」頁面
        findViewById<ImageButton>(R.id.btnGoToExercise).setOnClickListener {
            startActivity(Intent(this, MainActivity6::class.java))
        }

        findViewById<ImageButton>(R.id.btnHelpNoticeActivity).setOnClickListener {
            startActivity(Intent(this, MainActivity7::class.java))
        }

        findViewById<ImageButton>(R.id.btnhosActivity).setOnClickListener {
            startActivity(Intent(this, MainActivity5::class.java))
        }

        findViewById<ImageButton>(R.id.btnbmiActivity).setOnClickListener {
            startActivity(Intent(this, MainActivity8::class.java))
        }

        findViewById<ImageButton>(R.id.btnAppointmentActivity).setOnClickListener {
            startActivity(Intent(this, MainActivity9::class.java))
        }
        findViewById<ImageButton>(R.id.btnanxietyindex).setOnClickListener {
            startActivity(Intent(this, FourthActivity::class.java))
        }

        keroPlayer = MediaPlayer.create(this, R.raw.kero_kero)

        // 🐸 青蛙吉祥物按鈕互動功能
        findViewById<ImageButton>(R.id.frogButton).setOnClickListener {

            try {
                if (keroPlayer == null) {
                    keroPlayer = MediaPlayer.create(this, R.raw.kero_kero)
                }

                // ✅ 若正在播放就不再觸發
                if (keroPlayer?.isPlaying == true) {
                    return@setOnClickListener
                }

                keroPlayer?.seekTo(0)
                keroPlayer?.start()

                keroPlayer?.setOnCompletionListener {
                    it.reset()
                    it.release()
                    keroPlayer = null
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // 保底重建
                keroPlayer = MediaPlayer.create(this, R.raw.kero_kero)
                keroPlayer?.start()
            }
            // ✅ 執行輕微震動
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        80,  // 震動時間：80ms
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                // for pre-Oreo
                vibrator.vibrate(80)
            }

            it.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120).withEndAction {
                it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()
            AlertDialog.Builder(this)
                .setTitle("嗨\uD83D\uDC4B，我是您的AI健康助手Kero🐸")
                .setMessage("您想問我什麼健康問題呢？")
                .setPositiveButton("進入詢問") { _, _ ->
                    startActivity(Intent(this, ChatActivity::class.java))
                }
                .setNegativeButton("先不用", null)
                .show()
        }
    }

    // ✅ 每日語錄載入 API
    private fun getDailyQuote(quoteText: TextView) {
        val url = "$nodeApiBaseUrl/api/daily-quote"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainMenuActivity, "❌ 無法取得每日語錄", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val quote = json.optString("quote", "🌿 今天沒有語錄喔～")
                    runOnUiThread {
                        quoteText.text = quote
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            keroPlayer?.release()
            keroPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


