package com.dedroneTECTION.repository

import com.dedroneTECTION.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class DetectionRepository {

    private val _detections = MutableStateFlow<List<DroneDetection>>(emptyList())
    val detections: StateFlow<List<DroneDetection>> = _detections.asStateFlow()

    private val _acousticAlerts = MutableStateFlow<List<AcousticAlert>>(emptyList())
    val acousticAlerts: StateFlow<List<AcousticAlert>> = _acousticAlerts.asStateFlow()

    private val _sdrStatus = MutableStateFlow(SDRStatus())
    val sdrStatus: StateFlow<SDRStatus> = _sdrStatus.asStateFlow()

    private val knownDrones = ConcurrentHashMap<String, DroneDetection>()

    fun addDetection(detection: DroneDetection) {
        val key = detection.droneId.ifEmpty { detection.id }
        knownDrones[key] = detection
        _detections.value = knownDrones.values
            .sortedByDescending { it.timestamp }
            .toList()
    }

    fun addDetections(detections: List<DroneDetection>) {
        detections.forEach { detection ->
            val key = detection.droneId.ifEmpty { detection.id }
            knownDrones[key] = detection
        }
        _detections.value = knownDrones.values
            .sortedByDescending { it.timestamp }
            .toList()
    }

    fun removeStale(maxAgeMs: Long = 60000) {
        val now = System.currentTimeMillis()
        val iterator = knownDrones.entries.iterator()
        while (iterator.hasNext()) {
            if (now - iterator.next().value.timestamp > maxAgeMs) {
                iterator.remove()
            }
        }
        _detections.value = knownDrones.values
            .sortedByDescending { it.timestamp }
            .toList()
    }

    fun addAcousticAlert(alert: AcousticAlert) {
        val current = _acousticAlerts.value.toMutableList()
        current.add(0, alert)
        if (current.size > 50) current.removeAt(current.lastIndex)
        _acousticAlerts.value = current
    }

    fun updateSDRStatus(status: SDRStatus) {
        _sdrStatus.value = status
    }

    fun getActiveDetections(): List<DroneDetection> {
        val cutoff = System.currentTimeMillis() - 30000
        return knownDrones.values.filter { it.timestamp > cutoff }.toList()
    }

    fun clearAll() {
        knownDrones.clear()
        _detections.value = emptyList()
        _acousticAlerts.value = emptyList()
    }

    fun getDetectionCount(): Int = knownDrones.size

    fun getThreatSummary(): Triple<Int, Int, Int> {
        val now = System.currentTimeMillis()
        val active = knownDrones.values.filter { now - it.timestamp < 30000 }
        return Triple(
            active.count { it.threatLevel == ThreatLevel.HIGH },
            active.count { it.threatLevel == ThreatLevel.MEDIUM },
            active.count { it.threatLevel == ThreatLevel.LOW }
        )
    }
}
