package com.soundamplifier.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.soundamplifier.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class MicSourceManager(
    context: Context,
    private val engine: NativeAudioEngine
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _options = MutableStateFlow<List<MicSourceOption>>(emptyList())
    val options: StateFlow<List<MicSourceOption>> = _options

    private val _activeType = MutableStateFlow(MicSourceType.DEFAULT)
    val activeType: StateFlow<MicSourceType> = _activeType

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toasts: SharedFlow<String> = _toasts

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refreshDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val before = _options.value
            refreshDevices()

            // If active mic disappeared, fallback and notify
            val after = _options.value
            val activeStillAvailable = after.any { it.type == _activeType.value && it.isAvailable }
            if (!activeStillAvailable) {
                val next = if (after.any { it.type == MicSourceType.PHONE && it.isAvailable }) MicSourceType.PHONE else MicSourceType.DEFAULT
                setActiveTypeInternal(next, notifyFallback = true)
            } else if (before != after) {
                // Keep current selection but list changed
            }
        }
    }

    fun start() {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        refreshDevices()
        // Always use phone mic when available, else system default
        val chosen = if (_options.value.any { it.type == MicSourceType.PHONE && it.isAvailable }) {
            MicSourceType.PHONE
        } else {
            MicSourceType.DEFAULT
        }
        setActiveTypeInternal(chosen, notifyFallback = false)
    }

    fun stop() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }

    /** Call after locale changes so labels and fallback toasts match current language. */
    fun refreshDeviceList() {
        refreshDevices()
    }

    private fun refreshDevices() {
        val inputs = try {
            audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).toList()
        } catch (_: SecurityException) {
            // Missing BLUETOOTH_CONNECT on some devices; still allow defaults.
            emptyList()
        }

        val bt = inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || (Build.VERSION.SDK_INT >= 31 && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET) }
        val wired = inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
        val phone = inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }

        val list = listOf(
            MicSourceOption(MicSourceType.BLUETOOTH, appContext.getString(R.string.mic_label_bluetooth), bt?.id, bt != null),
            MicSourceOption(MicSourceType.WIRED, appContext.getString(R.string.mic_label_wired), wired?.id, wired != null),
            MicSourceOption(MicSourceType.PHONE, appContext.getString(R.string.mic_label_phone), phone?.id, phone != null),
        )

        val defaultLabel = appContext.getString(R.string.mic_label_system_default)
        _options.value = if (inputs.isEmpty()) {
            listOf(MicSourceOption(MicSourceType.DEFAULT, defaultLabel, null, true))
        } else {
            list + MicSourceOption(MicSourceType.DEFAULT, defaultLabel, null, true)
        }
    }

    private fun setActiveTypeInternal(type: MicSourceType, notifyFallback: Boolean) {
        val option = _options.value.firstOrNull { it.type == type && it.isAvailable }
            ?: _options.value.firstOrNull { it.type == MicSourceType.DEFAULT }
        val targetId = option?.deviceId ?: 0

        _activeType.value = option?.type ?: MicSourceType.DEFAULT

        // Apply to native engine: preferred input device ID (0 = default)
        engine.setPreferredInputDeviceId(targetId)
        if (notifyFallback) {
            val label = option?.label ?: appContext.getString(R.string.mic_label_system_default)
            _toasts.tryEmit(appContext.getString(R.string.toast_mic_switched, label))
        }
    }
}

