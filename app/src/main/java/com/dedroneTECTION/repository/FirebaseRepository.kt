package com.dedroneTECTION.repository

import com.dedroneTECTION.model.DroneDetection
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val database: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val detectionsRef get() = database?.getReference("detections")

    suspend fun pushDetection(detection: DroneDetection, deviceHash: String): Boolean {
        return try {
            val ref = detectionsRef ?: return false
            val data = detection.toJSON().toMutableMap()
            data["device_hash"] = deviceHash
            data["server_timestamp"] = System.currentTimeMillis()

            ref.push().setValue(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRecentDetections(limit: Int = 50): List<Map<String, Any?>> {
        return try {
            val ref = detectionsRef ?: return emptyList()
            val snapshot = ref
                .orderByChild("timestamp")
                .limitToLast(limit)
                .get()
                .await()

            snapshot.children.mapNotNull { it.value as? Map<String, Any?> }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isConnected(): Boolean {
        return try {
            database?.getReference(".info/connected") != null
        } catch (e: Exception) {
            false
        }
    }
}
