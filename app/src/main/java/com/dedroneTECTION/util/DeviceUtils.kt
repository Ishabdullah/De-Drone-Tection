package com.dedroneTECTION.util

import java.security.MessageDigest

object DeviceUtils {

    fun generateDeviceHash(androidId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest("de-drone-tection-$androidId".toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun generateDetectionId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return "det-" + (1..12).map { chars.random() }.joinToString("")
    }
}
