package com.dedroneTECTION.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import androidx.core.app.NotificationCompat
import com.dedroneTECTION.MainActivity
import com.dedroneTECTION.R
import com.dedroneTECTION.model.*
import com.dedroneTECTION.util.DeviceUtils
import com.dedroneTECTION.util.RemoteIDParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BLEScanService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): BLEScanService = this@BLEScanService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val remoteIDParser = RemoteIDParser()

    private var bluetoothLeScanner: android.bluetooth.le.BluetoothLeScanner? = null
    private val scanResults = MutableStateFlow<List<DroneDetection>>(emptyList())
    val detections: StateFlow<List<DroneDetection>> = scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val foundDevices = mutableMapOf<String, DroneDetection>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { processScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        stopScanning()
        scope.cancel()
        super.onDestroy()
    }

    fun startScanning() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        bluetoothLeScanner?.let { scanner ->
            val settings = android.bluetooth.le.ScanSettings.Builder()
                .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()

            scanner.startScan(null, settings, scanCallback)
            _isScanning.value = true
        }

        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun stopScanning() {
        bluetoothLeScanner?.stopScan(scanCallback)
        _isScanning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun processScanResult(result: ScanResult) {
        val device = result.device
        val scanRecord = result.scanRecord?.bytes
        val rssi = result.rssi

        var name = device.name ?: "Unknown BLE Device"
        var droneId = device.address ?: ""

        val remoteIDLabel = remoteIDParser.extractRemoteIDFromScanResult(scanRecord)
        if (remoteIDLabel != null) {
            name = "$remoteIDLabel - $name"
        }

        var basicID = BasicID()
        var locationData: LocationData? = null

        if (scanRecord != null) {
            val parsed = remoteIDParser.parseServiceData(scanRecord)
            basicID = parsed.first
            locationData = parsed.second
        }

        if (basicID.idData.isNotEmpty()) {
            droneId = basicID.idData
            name = "${basicID.uaType.name} - $droneId"
        }

        val estimatedDistance = DroneDetection.estimateDistance(rssi)
        val threatLevel = DroneDetection.calculateThreatLevel(estimatedDistance)

        val detection = DroneDetection(
            id = DeviceUtils.generateDetectionId(),
            name = name,
            droneId = droneId,
            rssi = rssi,
            latitude = locationData?.latitude,
            longitude = locationData?.longitude,
            altitude = locationData?.altitudeGeodetic,
            estimatedDistance = estimatedDistance,
            detectionMethod = DetectionMethod.BLE,
            timestamp = System.currentTimeMillis(),
            threatLevel = threatLevel,
            basicID = basicID,
            locationData = locationData,
            rawPayload = scanRecord ?: byteArrayOf(),
            scanName = "BLE:${device.address}"
        )

        foundDevices[droneId.ifEmpty { result.device.address }] = detection
        scanResults.value = foundDevices.values.toList()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BLE Drone Scan",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active BLE scanning for drone signals"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("De-Drone-Tection")
            .setContentText("BLE scanning active...")
            .setSmallIcon(R.drawable.ic_scanner)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "ble_scan_channel"
    }
}
