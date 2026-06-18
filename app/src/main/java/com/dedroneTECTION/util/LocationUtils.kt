package com.dedroneTECTION.util

import android.location.Location
import kotlin.math.*

object LocationUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0

    fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun bearingBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) -
                sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        var bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }

    fun isValidCoordinate(lat: Double?, lon: Double?): Boolean {
        if (lat == null || lon == null) return false
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }

    fun formatDistance(meters: Double): String = when {
        meters < 1000 -> String.format("%.0fm", meters)
        meters < 10000 -> String.format("%.2fkm", meters / 1000)
        else -> String.format("%.0fkm", meters / 1000)
    }
}
