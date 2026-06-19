package com.dedroneTECTION.model



enum class DetectionMethod {
    BLE,
    WIFI,
    ACOUSTIC,
    SIMULATION
}

enum class ThreatLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class RemoteIDMessageType(val typeId: Int) {
    BASIC_ID(0x00),
    LOCATION(0x01),
    SYSTEM(0x02);

    companion object {
        fun fromTypeId(typeId: Int): RemoteIDMessageType? =
            entries.find { it.typeId == typeId }
    }
}

enum class IDType(val value: Int) {
    NONE(0),
    SESSION_KNOWN(1),
    SESSION_RANDOM(2),
    UAS_ID_RELATIVE(3);

    companion object {
        fun fromValue(value: Int): IDType =
            entries.find { it.value == value } ?: NONE
    }
}

enum class UAStype(val value: Int) {
    NONE(0),
    AEROPLANE(1),
    HELICOPTER(2),
    MULTICOPTER(3),
    GLIDER(11),
    OTHER(15);

    companion object {
        fun fromValue(value: Int): UAStype =
            entries.find { it.value == value } ?: NONE
    }
}

data class BasicID(
    val idType: IDType = IDType.NONE,
    val uaType: UAStype = UAStype.NONE,
    val idData: String = ""
)

data class LocationData(
    val status: Int = 0,
    val direction: Double = 0.0,
    val speedHorizontal: Double = 0.0,
    val speedVertical: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitudePressure: Double = 0.0,
    val altitudeGeodetic: Double = 0.0,
    val height: Double = 0.0,
    val horizontalAccuracy: Double = 0.0,
    val verticalAccuracy: Double = 0.0,
    val barometerAccuracy: Double = 0.0,
    val speedAccuracy: Double = 0.0,
    val timestamp: Double = 0.0
)

data class SystemData(
    val operatorLatitude: Double = 0.0,
    val operatorLongitude: Double = 0.0,
    val areaCount: Int = 0,
    val areaRadius: Int = 0,
    val areaCeiling: Double = 0.0,
    val areaFloor: Double = 0.0,
    val classificationType: Int = 0,
    val operatorAltitude: Double = 0.0,
    val systemTimestamp: Double = 0.0
)

data class DroneDetection(
    val id: String = "",
    val name: String = "Unknown Drone",
    val droneId: String = "",
    val rssi: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val estimatedDistance: Double = 0.0,
    val detectionMethod: DetectionMethod = DetectionMethod.BLE,
    val timestamp: Long = System.currentTimeMillis(),
    val threatLevel: ThreatLevel = ThreatLevel.LOW,
    val basicID: BasicID = BasicID(),
    val locationData: LocationData? = null,
    val systemData: SystemData? = null,
    val rawPayload: ByteArray = byteArrayOf(),
    val scanName: String = ""
) {
    fun toJSON(): Map<String, Any?> = mapOf(
        "id" to id,
        "drone_id" to droneId,
        "name" to name,
        "rssi" to rssi,
        "latitude" to latitude,
        "longitude" to longitude,
        "altitude" to altitude,
        "estimated_distance" to estimatedDistance,
        "detection_method" to detectionMethod.name,
        "timestamp" to timestamp,
        "threat_level" to threatLevel.name
    )

    companion object {
        fun calculateThreatLevel(distance: Double): ThreatLevel = when {
            distance < 50.0 -> ThreatLevel.HIGH
            distance < 200.0 -> ThreatLevel.MEDIUM
            else -> ThreatLevel.LOW
        }

        fun estimateDistance(rssi: Int, txPower: Int = -59, n: Double = 2.5): Double {
            if (rssi == 0) return 0.0
            val ratio = (txPower - rssi).toDouble() / (10.0 * n)
            return Math.pow(10.0, ratio)
        }
    }
}

data class AcousticAlert(
    val timestamp: Long = System.currentTimeMillis(),
    val dominantFrequency: Double = 0.0,
    val confidence: Double = 0.0,
    val spectrogramData: FloatArray = floatArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AcousticAlert) return false
        return timestamp == other.timestamp && dominantFrequency == other.dominantFrequency
    }

    override fun hashCode(): Int = 31 * timestamp.hashCode() + dominantFrequency.hashCode()
}

data class SDRStatus(
    val connected: Boolean = false,
    val deviceName: String = "",
    val vendorId: Int = 0,
    val productId: Int = 0
)
