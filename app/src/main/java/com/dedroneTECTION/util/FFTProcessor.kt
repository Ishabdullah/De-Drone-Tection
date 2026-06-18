package com.dedroneTECTION.util

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FFTProcessor(private val bufferSize: Int = 1024) {

    private var realInput = FloatArray(bufferSize)
    private var imagInput = FloatArray(bufferSize)

    fun process(input: ShortArray): FloatArray {
        val windowed = applyHannWindow(input)

        realInput = FloatArray(bufferSize) { windowed[it].toFloat() / Short.MAX_VALUE }
        imagInput = FloatArray(bufferSize) { 0f }

        fft(realInput, imagInput)

        val magnitudes = FloatArray(bufferSize / 2) { i ->
            val re = realInput[i]
            val im = imagInput[i]
            10f * kotlin.math.log10((re * re + im * im).coerceAtLeast(1e-12f))
        }

        return magnitudes
    }

    fun processToFloat(input: FloatArray): FloatArray {
        realInput = FloatArray(bufferSize) { input[it] }
        imagInput = FloatArray(bufferSize) { 0f }

        fft(realInput, imagInput)

        return FloatArray(bufferSize / 2) { i ->
            val re = realInput[i]
            val im = imagInput[i]
            sqrt(re * re + im * im)
        }
    }

    private fun applyHannWindow(input: ShortArray): ShortArray {
        val windowed = ShortArray(bufferSize)
        for (i in 0 until bufferSize.coerceAtMost(input.size)) {
            val window = 0.5f * (1.0f - cos(2.0 * PI * i / (bufferSize - 1))).toFloat()
            windowed[i] = (input[i] * window).toInt().toShort()
        }
        return windowed
    }

    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n == 0) return

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit

            if (i < j) {
                var temp = real[i]; real[i] = real[j]; real[j] = temp
                temp = imag[i]; imag[i] = imag[j]; imag[j] = temp
            }
        }

        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * PI / len
            val wR = cos(angle).toFloat()
            val wI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var curR = 1f
                var curI = 0f

                for (k in 0 until halfLen) {
                    val tR = curR * real[i + k + halfLen] - curI * imag[i + k + halfLen]
                    val tI = curR * imag[i + k + halfLen] + curI * real[i + k + halfLen]

                    real[i + k + halfLen] = real[i + k] - tR
                    imag[i + k + halfLen] = imag[i + k] - tI
                    real[i + k] = real[i + k] + tR
                    imag[i + k] = imag[i + k] + tI

                    val newCurR = curR * wR - curI * wI
                    curI = curR * wI + curI * wR
                    curR = newCurR
                }
                i += len
            }
            len = len shl 1
        }
    }

    fun findDominantFrequency(magnitudes: FloatArray, sampleRate: Int = 44100): Pair<Float, Float> {
        var maxIndex = 0
        var maxValue = 0f

        val startBin = (100.0 * bufferSize / sampleRate).toInt().coerceAtLeast(0)
        val endBin = (800.0 * bufferSize / sampleRate).toInt().coerceAtMost(magnitudes.size - 1)

        for (i in startBin..endBin) {
            if (magnitudes[i] > maxValue) {
                maxValue = magnitudes[i]
                maxIndex = i
            }
        }

        val frequency = maxIndex.toFloat() * sampleRate / bufferSize
        return Pair(frequency, maxValue)
    }

    fun detectRotorSignature(magnitudes: FloatArray, sampleRate: Int = 44100): Float {
        val (domFreq, domMag) = findDominantFrequency(magnitudes, sampleRate)

        val rotorFreqLow = 50f
        val rotorFreqHigh = 400f
        val harmonicLow = rotorFreqLow * 2
        val harmonicHigh = rotorFreqHigh * 2

        if (domFreq < rotorFreqLow || domFreq > rotorFreqHigh) {
            return 0f
        }

        val freqScore = 1.0f - kotlin.math.abs(domFreq - 200f) / 200f
        val magScore = (domMag / 40f).coerceIn(0f, 1f)

        val binWidth = sampleRate.toFloat() / bufferSize
        var harmonicEnergy = 0f
        var harmonicCount = 0

        for (harmonic in 2..5) {
            val hFreq = domFreq * harmonic
            val bin = (hFreq / binWidth).toInt()
            if (bin < magnitudes.size) {
                harmonicEnergy += magnitudes[bin]
                harmonicCount++
            }
        }

        val harmonicScore = if (harmonicCount > 0) {
            (harmonicEnergy / harmonicCount / 30f).coerceIn(0f, 1f)
        } else 0f

        val confidence = (freqScore * 0.4f + magScore * 0.3f + harmonicScore * 0.3f).coerceIn(0f, 1f)

        return confidence
    }
}
