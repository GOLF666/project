package com.example.jenmix.jen8

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jenmix.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.compareTo


@SuppressLint("MissingPermission")
class MainActivity8 : AppCompatActivity() {

    private lateinit var tvUserInfo: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvUploadStatus: TextView
    private lateinit var recyclerCards: RecyclerView
    private lateinit var btnChart: Button
    private lateinit var btnHistory: Button

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    private val targetMac = "60:E8:5B:D6:90:77"
    private val serviceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val charUuid    = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb")

    private val handler      = Handler(Looper.getMainLooper())
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var hasUploaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!UserPrefs.isSetupCompleted(this)) {
            startActivity(Intent(this, FirstTimeSetupActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main8)

        tvUserInfo     = findViewById(R.id.tvUserInfo)
        tvStatus       = findViewById(R.id.tvStatus)
        tvWeight       = findViewById(R.id.tvWeight)
        tvUploadStatus = findViewById(R.id.tvUploadStatus)
        recyclerCards  = findViewById(R.id.recyclerCards)
        btnChart       = findViewById(R.id.btnChart)
        btnHistory     = findViewById(R.id.btnHistory)

        recyclerCards.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        showUserInfo()

        // ✅ 加上歷史紀錄跳轉
        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // ✅ 加上圖表統計跳轉
        btnChart.setOnClickListener {
            startActivity(Intent(this, ChartActivity::class.java))
        }

        val btMgr = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btMgr.adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner

        if (!bluetoothAdapter.isEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        2002
                    )
                }
            } else {
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            handler.postDelayed(scanLoop, 1000)
        }
    }

    private fun showUserInfo() {
        val gender   = UserPrefs.getGender(this)
        val age      = UserPrefs.getAge(this)
        val height   = UserPrefs.getHeight(this)
        val username = "使用者名稱"
        tvUserInfo.text = getString(R.string.label_user_info, username, gender, age, height)
    }

    private fun hasPermissions(): Boolean {
        val perms = listOfNotNull(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else null,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null
        )
        return perms.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestPermissions() {
        val perms = listOfNotNull(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else null,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null
        )
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 2001)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001 && hasPermissions()) {
            handler.postDelayed(scanLoop, 1000)
        } else {
            Toast.makeText(this, getString(R.string.permission_denied_msg), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(scanLoop)
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        super.onDestroy()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.address == targetMac) {
                runOnUiThread { tvStatus.text = getString(R.string.msg_connecting) }
                stopBleScan()
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()

                if (ActivityCompat.checkSelfPermission(this@MainActivity8, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt = device.connectGatt(this@MainActivity8, false, gattCallback)
                } else {
                    runOnUiThread { tvStatus.text = getString(R.string.msg_no_ble_permission) }
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread { tvStatus.text = getString(R.string.msg_connected) }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    tvStatus.text = getString(R.string.msg_disconnected)
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val svc  = gatt.getService(serviceUuid) ?: return
            val char = svc.getCharacteristic(charUuid) ?: return
            gatt.setCharacteristicNotification(char, true)
            val desc = char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) ?: return
            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(desc)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            runOnUiThread { tvStatus.text = getString(R.string.msg_data_receiving) }

            if (data.size == 20 && data[0] == 0x0D.toByte() && data[1] == 0x1F.toByte() && data[2] == 0x14.toByte()) {
                val parsed = BLEDataParser.parse(data)
                runOnUiThread {
                    tvStatus.text = getString(R.string.msg_measuring_done)
                    tvWeight.text = getString(R.string.label_weight, parsed.weight)
                    if (!hasUploaded) {
                        hasUploaded = true
                        uploadWeightToServer("使用者名稱", parsed.weight)
                        showHealthCards(parsed.weight, parsed.impedance, parsed.bmr)
                        tvUploadStatus.postDelayed({
                            hasUploaded = false
                            tvUploadStatus.text = ""
                        }, 3000)
                    }
                }
            }
        }
    }

    private val scanLoop = object : Runnable {
        override fun run() {
            if (bluetoothGatt == null) {
                runOnUiThread { tvStatus.text = getString(R.string.msg_scan) }
                startBleScan()
                handler.postDelayed({
                    stopBleScan()
                    handler.postDelayed(this, 5000)
                }, 5000)
            }
        }
    }

    private fun startBleScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothScanner.startScan(null, scanSettings, scanCallback)
        } else {
            tvStatus.text = getString(R.string.msg_no_ble_permission)
        }
    }

    private fun stopBleScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothScanner.stopScan(scanCallback)
        }
    }

    private fun uploadWeightToServer(username: String, weight: Float) {
        val gender = UserPrefs.getGender(this)
        val height = UserPrefs.getHeight(this)
        val age = UserPrefs.getAge(this)

        val roundedWeight = String.format("%.1f", weight).toFloat()

        Log.d("UPLOAD", "gender=$gender, height=$height, age=$age")

        val request = WeightUploadRequest(
            username = username,
            weight = roundedWeight,
            gender = gender,
            height = height,
            age = age
        )

        val api = RetrofitClient.getInstance().create(UploadApi::class.java)
        api.uploadWeight(request)
            .enqueue(object : Callback<WeightUploadResponse> {
                override fun onResponse(call: Call<WeightUploadResponse>, response: Response<WeightUploadResponse>) {
                    tvUploadStatus.text = if (response.isSuccessful) {
                        getString(R.string.msg_upload_success)
                    } else {
                        getString(R.string.msg_upload_fail, response.message())
                    }
                }

                override fun onFailure(call: Call<WeightUploadResponse>, t: Throwable) {
                    tvUploadStatus.text = getString(R.string.msg_upload_error, t.message ?: "未知錯誤")
                }
            })
    }

    private fun showHealthCards(weight: Float, impedance: Int, bmr: Int) {
        val gender = UserPrefs.getGender(this)
        val age    = UserPrefs.getAge(this)
        val height = UserPrefs.getHeight(this)
        if (gender.isNotBlank() && age > 0 && height > 0f) {
            val cards = HealthCardGenerator.generateCards(gender, age, height, weight, impedance, bmr)
            recyclerCards.adapter = HealthCardAdapter(cards)
        }
    }
}

