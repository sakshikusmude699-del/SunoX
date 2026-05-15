package com.soundamplifier.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Core real-time audio engine.
 * Captures mic input → processes via AudioProcessor → outputs to speaker/headphones.
 * Uses MIC for broad compatibility (emulator + devices).
 */
class AudioEngine(private val processor: AudioProcessor) {

    private val sampleRate = 16000  // Standard for voice; good compatibility
    private val channelIn = AudioFormat.CHANNEL_IN_MONO
    private val channelOut = AudioFormat.CHANNEL_OUT_MONO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var processingJob: Job? = null

    private val _inputLevel = MutableStateFlow(0f)
    val inputLevel: StateFlow<Float> = _inputLevel

    val isRunning: Boolean get() = processingJob?.isActive == true

    private fun computeRms(buffer: ShortArray, size: Int): Float {
        if (size == 0) return 0f
        var sum = 0.0
        var peak = 0f
        for (i in 0 until size) {
            val s = kotlin.math.abs(buffer[i] / 32768f)
            sum += s * s
            if (s > peak) peak = s
        }
        val rms = sqrt(sum / size).toFloat()
        // Use max of RMS and peak for better visibility; peak catches transients
        return max(rms, peak * 0.5f)
    }

    fun start() {
        if (isRunning) return

        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(sampleRate, channelIn, encoding),
            AudioTrack.getMinBufferSize(sampleRate, channelOut, encoding)
        ) * 2

        // MIC is most compatible (emulator + all devices); fallback if needed
        var record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, channelIn, encoding, bufferSize
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate, channelIn, encoding, bufferSize
            )
        }
        audioRecord = record

        // USAGE_ASSISTANCE_ACCESSIBILITY routes to earpiece/headphones for hearing aid
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelOut)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        // Attach system noise suppressor if available (as a pre-pass)
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(audioRecord!!.audioSessionId)
            noiseSuppressor?.enabled = true
        }

        audioRecord?.startRecording()
        audioTrack?.play()

        processingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ShortArray(bufferSize / 2)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read > 0) {
                    val rms = computeRms(buffer, read)
                    // Smoothing: fast attack, slow decay so bar is visible
                    val current = _inputLevel.value
                    _inputLevel.value = max(current * 0.85f, rms * 3f)
                    val processed = processor.process(buffer, read)
                    audioTrack?.write(processed, 0, read)
                }
            }
            _inputLevel.value = 0f
        }
    }

    fun stop() {
        processingJob?.cancel()
        processingJob = null
        _inputLevel.value = 0f

        noiseSuppressor?.release()
        noiseSuppressor = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
