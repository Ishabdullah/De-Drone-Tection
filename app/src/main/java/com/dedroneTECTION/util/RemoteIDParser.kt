package com.dedroneTECTION.util

import com.dedroneTECTION.model.*

class RemoteIDParser {

    companion object {
        private const val SERVICE_UUID_LOW = 0xFEAA.toInt()
        private const val REMOTE_ID_SERVICE_DATA_TYPE = 0x16

        private const val BASIC_ID_MSG = 0x00
        private const val LOCATION_MSG = 0x01
        private const val SYSTEM_MSG = 0x02
    }

    fun parseServiceData(serviceData: ByteArray?): Pair<BasicID, LocationData?> {
        if (serviceData == null || serviceData.size < 3) {
            return Pair(BasicID(), null)
        }

        var basicID = BasicID()
        var locationData: LocationData? = null

        try {
            var offset = 0
            while (offset + 1 < serviceData.size) {
                val msgType = serviceData[offset].toInt() and 0xFF
                val msgLength = serviceData[offset + 1].toInt() and 0xFF

                if (offset + 2 + msgLength > serviceData.size) break

                val msgData = serviceData.copyOfRange(offset + 2, offset + 2 + msgLength)

                when (msgType) {
                    BASIC_ID_MSG -> basicID = parseBasicID(msgData)
                    LOCATION_MSG -> locationData = parseLocation(msgData)
                    SYSTEM_MSG -> { /* parsed but not returned in this method */ }
                }

                offset += 2 + msgLength
            }
        } catch (e: Exception) {
            return Pair(BasicID(), null)
        }

        return Pair(basicID, locationData)
    }

    fun parseServiceDataFull(serviceData: ByteArray?): Triple<BasicID, LocationData?, SystemData?> {
        if (serviceData == null || serviceData.size < 3) {
            return Triple(BasicID(), null, null)
        }

        var basicID = BasicID()
        var locationData: LocationData? = null
        var systemData: SystemData? = null

        try {
            var offset = 0
            while (offset + 1 < serviceData.size) {
                val msgType = serviceData[offset].toInt() and 0xFF
                val msgLength = serviceData[offset + 1].toInt() and 0xFF

                if (offset + 2 + msgLength > serviceData.size) break

                val msgData = serviceData.copyOfRange(offset + 2, offset + 2 + msgLength)

                when (msgType) {
                    BASIC_ID_MSG -> basicID = parseBasicID(msgData)
                    LOCATION_MSG -> locationData = parseLocation(msgData)
                    SYSTEM_MSG -> systemData = parseSystem(msgData)
                }

                offset += 2 + msgLength
            }
        } catch (e: Exception) {
            return Triple(BasicID(), null, null)
        }

        return Triple(basicID, locationData, systemData)
    }

    private fun parseBasicID(data: ByteArray): BasicID {
        if (data.size < 19) return BasicID()

        val idType = IDType.fromValue((data[0].toInt() and 0x0F))
        val uaType = UAStype.fromValue((data[0].toInt() shr 4) and 0x0F)

        val idBytes = data.copyOfRange(1, 19)
        val idData = idBytes
            .filter { it != 0x00.toByte() }
            .map { (it.toInt() and 0xFF).toChar() }
            .joinToString("")
            .trim()

        return BasicID(idType = idType, uaType = uaType, idData = idData)
    }

    private fun parseLocation(data: ByteArray): LocationData {
        if (data.size < 19) return LocationData()

        val status = data[0].toInt() and 0x0F
        val directionRaw = ((data[1].toInt() and 0xFF) shl 4) or ((data[2].toInt() and 0xFF) shr 4)
        val direction = directionRaw * 0.00549324

        val eastRaw = ((data[2].toInt() and 0x0F) shl 15) or
                ((data[3].toInt() and 0xFF) shl 7) or
                ((data[4].toInt() and 0xFF) shr 1)
        val speedHorizontal = eastRaw * 0.25

        val speedVerticalRaw = ((data[4].toInt() and 0x01) shl 15) or ((data[5].toInt() and 0xFF) shl 7) or ((data[6].toInt() and 0xFF) shr 1)
        val speedVertical = (speedVerticalRaw - 10000) * 0.5

        val latRaw = ((data[6].toInt() and 0x01) shl 23) or
                ((data[7].toInt() and 0xFF) shl 15) or
                ((data[8].toInt() and 0xFF) shl 7) or
                ((data[9].toInt() and 0xFF) shr 1)
        val latitude = (latRaw - 90000000) * 1e-7

        val lonRaw = ((data[9].toInt() and 0x01) << 23) or
                ((data[10].toInt() and 0xFF) shl 15) or
                ((data[11].toInt() and 0xFF) shl 7) or
                ((data[12].toInt() and 0xFF) shr 1)
        val longitude = (lonRaw - 180000000) * 1e-7

        val altitudeRaw = ((data[12].toInt() and 0x01) shl 7) or (data[13].toInt() and 0xFF)
        val altitudePressure = (altitudeRaw - 1000) * 0.5

        val altGeoRaw = ((data[14].toInt() and 0xFF) shl 4) or ((data[15].toInt() and 0xFF) shr 4)
        val altitudeGeodetic = (altGeoRaw - 1000) * 0.5

        val heightRaw = ((data[15].toInt() and 0x0F) shl 7) or ((data[16].toInt() and 0xFF) shr 1)
        val height = heightRaw * 0.5

        val timestamp = (data[17].toInt() and 0xFF).toDouble() * 0.5

        return LocationData(
            status = status,
            direction = direction,
            speedHorizontal = speedHorizontal,
            speedVertical = speedVertical,
            latitude = latitude,
            longitude = longitude,
            altitudePressure = altitudePressure,
            altitudeGeodetic = altitudeGeodetic,
            height = height,
            timestamp = timestamp
        )
    }

    private fun parseSystem(data: ByteArray): SystemData {
        if (data.size < 15) return SystemData()

        val opLatRaw = ((data[0].toInt() and 0xFF) shl 23) or
                ((data[1].toInt() and 0xFF) shl 15) or
                ((data[2].toInt() and 0xFF) shl 7) or
                ((data[3].toInt() and 0xFF) shr 1)
        val operatorLatitude = (opLatRaw - 90000000) * 1e-7

        val opLonRaw = ((data[3].toInt() and 0x01) shl 23) or
                ((data[4].toInt() and 0xFF) shl 15) or
                ((data[5].toInt() and 0xFF) shl 7) or
                ((data[6].toInt() and 0xFF) shr 1)
        val operatorLongitude = (opLonRaw - 180000000) * 1e-7

        val areaCount = data[7].toInt() and 0xFF
        val areaRadius = data[8].toInt() and 0xFF

        val areaCeilingRaw = ((data[9].toInt() and 0xFF) shl 4) or ((data[10].toInt() and 0xFF) shr 4)
        val areaCeiling = (areaCeilingRaw - 1000) * 0.1

        val areaFloorRaw = ((data[10].toInt() and 0x0F) shl 7) or ((data[11].toInt() and 0xFF) shr 1)
        val areaFloor = (areaFloorRaw - 1000) * 0.1

        val classificationType = ((data[11].toInt() and 0x01) shl 3) or ((data[12].toInt() and 0xFF) shr 5)

        val opAltRaw = ((data[12].toInt() and 0x1F) shl 10) or ((data[13].toInt() and 0xFF) shl 2) or ((data[14].toInt() and 0xFF) shr 6)
        val operatorAltitude = (opAltRaw - 1000) * 0.1

        val systemTimestamp = ((data[14].toInt() and 0x3F).toLong() shl 32)

        return SystemData(
            operatorLatitude = operatorLatitude,
            operatorLongitude = operatorLongitude,
            areaCount = areaCount,
            areaRadius = areaRadius,
            areaCeiling = areaCeiling,
            areaFloor = areaFloor,
            classificationType = classificationType,
            operatorAltitude = operatorAltitude,
            systemTimestamp = systemTimestamp.toDouble()
        )
    }

    fun extractRemoteIDFromScanResult(scanRecord: ByteArray?): String? {
        if (scanRecord == null) return null

        var offset = 0
        while (offset < scanRecord.size) {
            val length = scanRecord[offset].toInt() and 0xFF
            if (length == 0) break

            if (offset + 1 < scanRecord.size) {
                val adType = scanRecord[offset + 1].toInt() and 0xFF
                if (adType == 0xFF && offset + 3 < scanRecord.size) {
                    val companyByte1 = scanRecord[offset + 2].toInt() and 0xFF
                    val companyByte2 = scanRecord[offset + 3].toInt() and 0xFF
                    if (companyByte1 == 0x06 && companyByte2 == 0x00) {
                        return "Apple Nearby"
                    }
                    if (companyByte1 == 0x00 && companyByte2 == 0x06) {
                        return "RemoteID"
                    }
                }
            }

            if (length < 1) break
            offset += 1 + length
        }

        return null
    }
}
