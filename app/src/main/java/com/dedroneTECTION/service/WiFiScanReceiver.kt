package com.dedroneTECTION.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult as WifiScanResult
import android.net.wifi.WifiManager
import com.dedroneTECTION.model.*
import com.dedroneTECTION.util.DeviceUtils
import com.dedroneTECTION.util.RemoteIDParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WiFiScanReceiver : BroadcastReceiver() {

    private val remoteIDParser = RemoteIDParser()
    private val _scanResults = MutableStateFlow<List<DroneDetection>>(emptyList())
    val scanResults: StateFlow<List<DroneDetection>> = _scanResults.asStateFlow()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val results = wifiManager?.scanResults ?: return

        val droneDetections = mutableListOf<DroneDetection>()

        for (result in results) {
            val remoteIDLabel = remoteIDParser.extractRemoteIDFromScanResult(null)
            val hasRemoteID = result.SSID.contains("RemoteID", ignoreCase = true) ||
                    result.capabilities.contains("RemoteID", ignoreCase = true) ||
                    result.capabilities.contains("NAN", ignoreCase = true)

            if (hasRemoteID || result.level > -40) {
                val estimatedDistance = DroneDetection.estimateDistance(result.level, txPower = -30, n = 3.5)
                val threatLevel = DroneDetection.calculateThreatLevel(estimatedDistance)

                val detection = DroneDetection(
                    id = DeviceUtils.generateDetectionId(),
                    name = "WiFi:${result.SSID.ifEmpty { result.BSSID }}",
                    droneId = result.BSSID,
                    rssi = result.level,
                    estimatedDistance = estimatedDistance,
                    detectionMethod = DetectionMethod.WIFI,
                    timestamp = System.currentTimeMillis(),
                    threatLevel = threatLevel,
                    rawPayload = result.toString().toByteArray(),
                    scanName = "WiFi:${result.BSSID}"
                )

                droneDetections.add(detection)
            }
        }

        _scanResults.value = droneDetections
    }
}
