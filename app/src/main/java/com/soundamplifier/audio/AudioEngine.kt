package com.soundamplifier.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Core real-time audio engine.
 * Captures mic input → processes via AudioProcessor → outputs to headphones.
 */
class AudioEngine(private val processor: AudioProcessor) {

    private val sampleRate = 16000
    private val channelIn = AudioFormat.CHANNEL_IN_MONO
    private val channelOut = AudioFormat.CHANNEL_OUT_MONO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var processingJob: Job? = null

    val isRunning: Boolean get() = processingJob?.isActive == true

    fun start() {
        if (isRunning) return

        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(sampleRate, channelIn, encoding),
            AudioTrack.getMinBufferSize(sampleRate, channelOut, encoding)
        ) * 2

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, channelIn, encoding, bufferSize
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate, channelOut, encoding,
            bufferSize, AudioTrack.MODE_STREAM
        )

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
                    val processed = processor.process(buffer, read)
                    audioTrack?.write(processed, 0, read)
                }
            }
        }
    }

    fun stop() {
        processingJob?.cancel()
        processingJob = null

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
