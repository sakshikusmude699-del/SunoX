package com.soundamplifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Standard audiometric frequencies in Hz
val AUDIOGRAM_FREQUENCIES = intArrayOf(250, 500, 1000, 2000, 4000, 8000)

@Entity(tableName = "audiogram_profiles")
data class AudiogramProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String = "My Profile",
    val createdAt: Long = System.currentTimeMillis(),

    // Hearing threshold in dB HL per frequency for each ear
    // Index maps to AUDIOGRAM_FREQUENCIES
    val leftEarThresholds: String = "0,0,0,0,0,0",   // CSV
    val rightEarThresholds: String = "0,0,0,0,0,0",  // CSV

    // Derived boost gains in dB per frequency (auto-calculated from thresholds)
    val leftEarGains: String = "0,0,0,0,0,0",
    val rightEarGains: String = "0,0,0,0,0,0",

    // Noise reduction level 0-100
    val noiseReductionLevel: Int = 50
) {
    fun leftThresholdList(): List<Float> =
        leftEarThresholds.split(",").map { it.toFloat() }

    fun rightThresholdList(): List<Float> =
        rightEarThresholds.split(",").map { it.toFloat() }

    fun leftGainList(): List<Float> =
        leftEarGains.split(",").map { it.toFloat() }

    fun rightGainList(): List<Float> =
        rightEarGains.split(",").map { it.toFloat() }
}

/** Convert hearing thresholds to amplification gains using NAL-R inspired formula */
fun thresholdsToGains(thresholds: List<Float>): List<Float> {
    return thresholds.map { threshold ->
        when {
            threshold <= 20f -> 0f          // Normal hearing, no boost
            threshold <= 40f -> threshold * 0.4f
            threshold <= 60f -> threshold * 0.5f
            threshold <= 80f -> threshold * 0.6f
            else -> threshold * 0.7f        // Severe loss, higher gain
        }.coerceIn(0f, 40f)                 // Cap at 40dB gain for safety
    }
}
