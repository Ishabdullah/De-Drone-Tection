package com.dedroneTECTION.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dedroneTECTION.MainActivity
import com.dedroneTECTION.R
import com.dedroneTECTION.util.FFTProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AcousticCaptureService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): AcousticCaptureService = this@AcousticCaptureService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val fftProcessor = FFTProcessor(BUFFER_SIZE)

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _spectrogramData = MutableStateFlow(FloatArray(0))
    val spectrogramData: StateFlow<FloatArray> = _spectrogramData.asStateFlow()

    private val _droneSoundDetected = MutableStateFlow(false)
    val droneSoundDetected: StateFlow<Boolean> = _droneSoundDetected.asStateFlow()

    private val _dominantFrequency = MutableStateFlow(0f)
    val dominantFrequency: StateFlow<Float> = _dominantFrequency.asStateFlow()

    private val _confidence = MutableStateFlow(0f)
    val confidence: StateFlow<Float> = _confidence.asStateFlow()

    private var sensitivity = 0.5f
    private var consecutiveDetections = 0

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        stopCapture()
        scope.cancel()
        super.onDestroy()
    }

    fun setSensitivity(value: Float) {
        sensitivity = value.coerceIn(0.1f, 1.0f)
    }

    fun startCapture() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = minBufferSize.coerceAtLeast(BUFFER_SIZE * 2)

        audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (e: SecurityException) {
            null
        }

        audioRecord?.let { record ->
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            record.startRecording()
            _isCapturing.value = true

            captureJob = scope.launch {
                val readBuffer = ShortArray(BUFFER_SIZE)

                while (isActive && _isCapturing.value) {
                    val read = record.read(readBuffer, 0, BUFFER_SIZE)
                    if (read > 0) {
                        val magnitudes = fftProcessor.process(readBuffer.copyOf(read))
                        _spectrogramData.value = magnitudes

                        val (freq, confidence) = fftProcessor.findDominantFrequency(magnitudes, SAMPLE_RATE)
                        _dominantFrequency.value = freq

                        val rotorConfidence = fftProcessor.detectRotorSignature(magnitudes, SAMPLE_RATE)
                        _confidence.value = rotorConfidence

                        if (rotorConfidence > sensitivity) {
                            consecutiveDetections++
                            if (consecutiveDetections >= 3) {
                                _droneSoundDetected.value = true
                            }
                        } else {
                            consecutiveDetections = 0
                            _droneSoundDetected.value = false
                        }
                    }
                    delay(50)
                }
            }
        }

        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun stopCapture() {
        _isCapturing.value = false
        _droneSoundDetected.value = false
        consecutiveDetections = 0
        captureJob?.cancel()
        captureJob = null
        audioRecord?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        audioRecord = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Acoustic Detection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Acoustic drone detection active"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("De-Drone-Tection")
            .setContentText("Acoustic monitoring active...")
            .setSmallIcon(R.drawable.ic_scanner)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 2048
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "acoustic_channel"
    }
}
