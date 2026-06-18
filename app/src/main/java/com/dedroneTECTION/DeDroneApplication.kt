package com.dedroneTECTION

import android.app.Application
import android.provider.Settings
import com.dedroneTECTION.repository.DetectionRepository
import com.dedroneTECTION.repository.FirebaseRepository
import com.dedroneTECTION.util.DeviceUtils

class DeDroneApplication : Application() {

    val detectionRepository = DetectionRepository()
    val firebaseRepository = FirebaseRepository()

    val deviceHash: String by lazy {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        DeviceUtils.generateDeviceHash(androidId)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: DeDroneApplication
            private set
    }
}
