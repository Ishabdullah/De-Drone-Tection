package com.dedroneTECTION.repository

import com.dedroneTECTION.model.DroneDetection
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val database = FirebaseDatabase.getInstance()
    private val detectionsRef = database.getReference("detections")

    suspend fun pushDetection(detection: DroneDetection, deviceHash: String): Boolean {
        return try {
            val data = detection.toJSON().toMutableMap()
            data["device_hash"] = deviceHash
            data["server_timestamp"] = System.currentTimeMillis()

            detectionsRef.push().setValue(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRecentDetections(limit: Long = 50): List<Map<String, Any?>> {
        return try {
            val snapshot = detectionsRef
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
            val info = database.getReference(".info/connected")
            true
        } catch (e: Exception) {
            false
        }
    }
}
