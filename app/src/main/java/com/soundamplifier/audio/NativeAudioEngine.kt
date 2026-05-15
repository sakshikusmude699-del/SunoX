package com.soundamplifier.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Kotlin wrapper for the native Oboe audio engine.
 * Exposes the same interface as the previous AudioEngine for ViewModel compatibility.
 */
class NativeAudioEngine {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var pollJob: Job? = null

    private val _inputLevel = MutableStateFlow(0f)
    val inputLevel: StateFlow<Float> = _inputLevel

    val isRunning: Boolean
        get() = nativeIsRunning()

    init {
        nativeInit()
    }

    fun start(): Boolean {
        if (isRunning) return true
        val ok = nativeStart()
        if (ok) {
            pollJob = scope.launch {
                while (isActive && nativeIsRunning()) {
                    _inputLevel.value = nativeGetInputLevel()
                    delay(50)  // 20 Hz update for UI
                }
                _inputLevel.value = 0f
            }
        }
        return ok
    }

    fun stop() {
        nativeStop()
        pollJob?.cancel()
        pollJob = null
        _inputLevel.value = 0f
    }

    fun setMasterGain(gain: Float) = nativeSetMasterGain(gain)
    fun setBandGainsFromDb(gains: FloatArray) = nativeSetBandGains(gains)
    fun setLowBoost(db: Float) = nativeSetLowBoost(db)
    fun setHighBoost(db: Float) = nativeSetHighBoost(db)
    fun setPreferredInputDeviceId(deviceId: Int) = nativeSetPreferredInputDeviceId(deviceId)

    fun setCompressionParams(attackMs: Float, releaseMs: Float, ratio: Float, thresholdDb: Float, kneeDb: Float) =
        nativeSetCompressionParams(attackMs, releaseMs, ratio, thresholdDb, kneeDb)
    fun setBandCompressionThresholds(thresholds: FloatArray) = nativeSetBandCompressionThresholds(thresholds)
    fun setPrescribedGains(gains: FloatArray) = nativeSetPrescribedGains(gains)
    fun setExpansionParams(thresholdDb: Float, ratio: Float) = nativeSetExpansionParams(thresholdDb, ratio)
    fun setBoostQuietSounds(percent: Float) = nativeSetBoostQuietSounds(percent)
    fun getRoutedInputDeviceId(): Int = nativeGetRoutedInputDeviceId()

    fun getVisualizationData(): ShortArray? = if (isRunning) nativeGetVisualizationData() else null

    fun release() {
        stop()
        nativeRelease()
    }

    private external fun nativeInit()
    private external fun nativeRelease()
    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeIsRunning(): Boolean
    private external fun nativeGetInputLevel(): Float
    private external fun nativeSetMasterGain(gain: Float)
    private external fun nativeSetBandGains(gains: FloatArray)
    private external fun nativeSetLowBoost(db: Float)
    private external fun nativeSetHighBoost(db: Float)
    private external fun nativeSetPreferredInputDeviceId(deviceId: Int)
    private external fun nativeGetRoutedInputDeviceId(): Int
    private external fun nativeSetCompressionParams(attackMs: Float, releaseMs: Float, ratio: Float, thresholdDb: Float, kneeDb: Float)
    private external fun nativeSetBandCompressionThresholds(thresholds: FloatArray)
    private external fun nativeSetPrescribedGains(gains: FloatArray)
    private external fun nativeSetExpansionParams(thresholdDb: Float, ratio: Float)
    private external fun nativeSetBoostQuietSounds(percent: Float)
    private external fun nativeGetVisualizationData(): ShortArray?

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}
