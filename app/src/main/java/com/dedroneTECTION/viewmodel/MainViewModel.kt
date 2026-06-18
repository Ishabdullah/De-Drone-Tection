package com.dedroneTECTION.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dedroneTECTION.DeDroneApplication
import com.dedroneTECTION.model.*
import com.dedroneTECTION.service.AcousticCaptureService
import com.dedroneTECTION.service.BLEScanService
import com.dedroneTECTION.service.USBDeviceReceiver
import com.dedroneTECTION.service.WiFiScanReceiver
import com.dedroneTECTION.util.DeviceUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicInteger

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DeDroneApplication
    private val detectionRepo = app.detectionRepository
    private val firebaseRepo = app.firebaseRepository

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val detections = detectionRepo.detections
    val acousticAlerts = detectionRepo.acousticAlerts
    val sdrStatus = detectionRepo.sdrStatus

    private var bleScanService: BLEScanService? = null
    private var acousticService: AcousticCaptureService? = null
    private val wifiReceiver = WiFiScanReceiver()
    private val usbReceiver = USBDeviceReceiver()

    private var simulationJob: Job? = null
    private var staleCleanupJob: Job? = null
    private val simulationCounter = AtomicInteger(0)

    private var wifiScanJob: Job? = null

    private val usbStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connected = intent.getBooleanExtra("connected", false)
            val deviceName = intent.getStringExtra("device_name") ?: ""
            val vendorId = intent.getIntExtra("vendor_id", 0)
            val productId = intent.getIntExtra("product_id", 0)
            detectionRepo.updateSDRStatus(
                SDRStatus(connected, deviceName, vendorId, productId)
            )
        }
    }

    init {
        registerReceivers()
        startWiFiScanning()
        startStaleCleanup()
    }

    private fun registerReceivers() {
        val context = getApplication<Application>()

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(wifiReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(wifiReceiver, filter)
        }

        val usbFilter = IntentFilter().apply {
            addAction("com.dedroneTECTION.SDR_STATUS_CHANGED")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(usbStatusReceiver, usbFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbStatusReceiver, usbFilter)
        }
    }

    private fun startWiFiScanning() {
        wifiScanJob = viewModelScope.launch {
            while (isActive) {
                val wifiManager = getApplication<Application>()
                    .getApplicationContext().getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                wifiManager?.startScan()
                delay(_uiState.value.scanInterval)
            }
        }
    }

    private fun startStaleCleanup() {
        staleCleanupJob = viewModelScope.launch {
            while (isActive) {
                detectionRepo.removeStale(30000)
                updateThreatSummary()
                delay(5000)
            }
        }
    }

    fun toggleBLEScan() {
        val newState = !_uiState.value.isBLEEnabled
        _uiState.update { it.copy(isBLEEnabled = newState) }

        if (newState) {
            viewModelScope.launch {
                detectionRepo.addDetection(
                    createSimulationDetection("BLE Scanner Active", DetectionMethod.BLE)
                )
            }
        }
    }

    fun toggleWiFiScan() {
        val newState = !_uiState.value.isWiFiEnabled
        _uiState.update { it.copy(isWiFiEnabled = newState) }

        if (newState) {
            viewModelScope.launch {
                detectionRepo.addDetection(
                    createSimulationDetection("WiFi Scanner Active", DetectionMethod.WIFI)
                )
            }
        }
    }

    fun toggleAcousticDetection() {
        val newState = !_uiState.value.isAcousticEnabled
        _uiState.update { it.copy(isAcousticEnabled = newState) }
    }

    fun toggleNetworkMode() {
        _uiState.update { it.copy(isNetworkEnabled = !it.isNetworkEnabled) }
    }

    fun toggleSimulationMode() {
        val newState = !_uiState.value.isSimulationEnabled
        _uiState.update { it.copy(isSimulationEnabled = newState) }

        if (newState) {
            startSimulation()
        } else {
            stopSimulation()
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setScanInterval(interval: Long) {
        _uiState.update { it.copy(scanInterval = interval) }
    }

    fun setAcousticSensitivity(sensitivity: Float) {
        _uiState.update { it.copy(acousticSensitivity = sensitivity) }
        acousticService?.setSensitivity(sensitivity)
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (isActive) {
                delay(10000)

                val id = simulationCounter.incrementAndGet()
                val lat = 37.7749 + (Math.random() - 0.5) * 0.01
                val lon = -122.4194 + (Math.random() - 0.5) * 0.01
                val rssi = -30 - (Math.random() * 50).toInt()
                val distance = DroneDetection.estimateDistance(rssi)

                val detection = DroneDetection(
                    id = "sim-$id",
                    name = "Simulated DJI Mavic ${id % 10}",
                    droneId = "SIM-DRONE-${String.format("%04d", id)}",
                    rssi = rssi,
                    latitude = lat,
                    longitude = lon,
                    altitude = 50.0 + Math.random() * 100,
                    estimatedDistance = distance,
                    detectionMethod = DetectionMethod.SIMULATION,
                    timestamp = System.currentTimeMillis(),
                    threatLevel = DroneDetection.calculateThreatLevel(distance),
                    scanName = "Simulation"
                )

                detectionRepo.addDetection(detection)
                updateThreatSummary()

                if (_uiState.value.isNetworkEnabled) {
                    firebaseRepo.pushDetection(detection, app.deviceHash)
                }
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    private fun updateThreatSummary() {
        val (high, medium, low) = detectionRepo.getThreatSummary()
        _uiState.update { it.copy(
            highThreatCount = high,
            mediumThreatCount = medium,
            lowThreatCount = low
        ) }
    }

    private fun createSimulationDetection(name: String, method: DetectionMethod): DroneDetection {
        return DroneDetection(
            id = DeviceUtils.generateDetectionId(),
            name = name,
            droneId = "",
            rssi = -50,
            estimatedDistance = 100.0,
            detectionMethod = method,
            timestamp = System.currentTimeMillis(),
            threatLevel = ThreatLevel.LOW,
            scanName = method.name
        )
    }

    fun getFilteredDetections(): StateFlow<List<DroneDetection>> {
        return _uiState.map { state ->
            val allDetections = detectionRepo.detections.value
            if (state.searchQuery.isEmpty()) {
                allDetections
            } else {
                allDetections.filter {
                    it.name.contains(state.searchQuery, ignoreCase = true) ||
                            it.droneId.contains(state.searchQuery, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, detectionRepo.detections.value)
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        staleCleanupJob?.cancel()
        wifiScanJob?.cancel()

        try {
            val context = getApplication<Application>()
            context.unregisterReceiver(wifiReceiver)
            context.unregisterReceiver(usbStatusReceiver)
        } catch (_: Exception) {}
    }
}

data class MainUiState(
    val isBLEEnabled: Boolean = false,
    val isWiFiEnabled: Boolean = false,
    val isAcousticEnabled: Boolean = false,
    val isNetworkEnabled: Boolean = false,
    val isSimulationEnabled: Boolean = false,
    val searchQuery: String = "",
    val scanInterval: Long = 5000,
    val acousticSensitivity: Float = 0.5f,
    val highThreatCount: Int = 0,
    val mediumThreatCount: Int = 0,
    val lowThreatCount: Int = 0
)
