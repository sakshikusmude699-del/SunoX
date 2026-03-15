package com.soundamplifier.audio

import kotlin.math.*

/**
 * Real-time audio processor applying:
 * - Per-frequency gain (from audiogram profile)
 * - Noise gate (noise reduction)
 * - Dynamic range compression (boost quiet sounds)
 *
 * Processes PCM float buffers in-place.
 */
class AudioProcessor {

    // Per-band gains in linear scale (index = frequency band)
    var bandGains: FloatArray = FloatArray(6) { 1f }

    // Noise gate threshold (0.0 - 1.0 linear amplitude)
    var noiseGateThreshold: Float = 0.01f

    // Compression: ratio and threshold
    var compressionThreshold: Float = 0.3f  // above this, compress
    var compressionRatio: Float = 4f         // 4:1 ratio

    // Master gain multiplier
    var masterGain: Float = 1.5f

    // Simple IIR band-pass filter state per band
    private val filterStates = Array(6) { FloatArray(2) }

    // Band center frequencies and their approximate filter coefficients
    private val bandFreqs = intArrayOf(250, 500, 1000, 2000, 4000, 8000)
    private val sampleRate = 16000f

    /**
     * Process a buffer of PCM float samples in-place.
     * Input is mono, values in [-1.0, 1.0].
     */
    fun process(buffer: ShortArray, size: Int): ShortArray {
        val floatBuf = ShortArray(size)

        for (i in 0 until size) {
            var sample = buffer[i] / 32768f

            // 1. Noise gate
            if (abs(sample) < noiseGateThreshold) {
                floatBuf[i] = 0
                continue
            }

            // 2. Apply per-band gain via simple shelving approach
            sample = applyBandGains(sample, i)

            // 3. Dynamic range compression
            sample = compress(sample)

            // 4. Master gain
            sample *= masterGain

            // 5. Clip to valid range
            sample = sample.coerceIn(-1f, 1f)

            floatBuf[i] = (sample * 32767f).toInt().toShort()
        }

        return floatBuf
    }

    /**
     * Simplified band gain: applies low-shelf, mid gains, and high-shelf
     * based on the 6-band audiogram profile.
     */
    private fun applyBandGains(sample: Float, sampleIndex: Int): Float {
        // Low shelf (250-500 Hz) — bands 0,1
        val lowGain = (bandGains[0] + bandGains[1]) / 2f
        // Mid (1k-2k Hz) — bands 2,3
        val midGain = (bandGains[2] + bandGains[3]) / 2f
        // High shelf (4k-8k Hz) — bands 4,5
        val highGain = (bandGains[4] + bandGains[5]) / 2f

        // Blend gains — simplified weighted average
        // A proper implementation would use FFT or IIR filters per band
        return sample * ((lowGain + midGain + highGain) / 3f)
    }

    private fun compress(sample: Float): Float {
        val absVal = abs(sample)
        if (absVal <= compressionThreshold) return sample

        val excess = absVal - compressionThreshold
        val compressed = compressionThreshold + (excess / compressionRatio)
        return if (sample >= 0) compressed else -compressed
    }

    /** Set gains from dB values (converts dB → linear) */
    fun setGainsFromDb(gainsDb: List<Float>) {
        bandGains = FloatArray(gainsDb.size) { i ->
            dbToLinear(gainsDb[i])
        }
    }

    /** Set noise reduction level (0-100) → gate threshold */
    fun setNoiseReduction(level: Int) {
        // Map 0-100 to threshold 0.0-0.1
        noiseGateThreshold = (level / 100f) * 0.1f
    }

    private fun dbToLinear(db: Float): Float =
        10f.pow(db / 20f)
}
