package com.soundamplifier.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.soundamplifier.data.AUDIOGRAM_FREQUENCIES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays pure sine tones for audiogram threshold testing.
 * Frequencies follow standard audiometric test sequence.
 */
class AudiogramTest {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null

    /**
     * Play a pure tone at [frequency] Hz at [volumeDb] dBFS (-60 to 0).
     * Call stop() to end playback.
     */
    suspend fun playTone(frequency: Int, volumeDb: Float = -20f) = withContext(Dispatchers.IO) {
        stopTone()

        val amplitude = dbToLinear(volumeDb)
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        // Stream sine wave in chunks
        val chunkSamples = sampleRate / 10 // 100ms chunks
        val buffer = FloatArray(chunkSamples * 2) // stereo
        var phase = 0.0

        while (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
            for (i in 0 until chunkSamples) {
                val sample = (amplitude * sin(2.0 * PI * frequency * phase / sampleRate)).toFloat()
                buffer[i * 2] = sample     // left
                buffer[i * 2 + 1] = sample // right
                phase = (phase + 1) % sampleRate
            }
            audioTrack?.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        }
    }

    fun stopTone() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    /** dBFS to linear amplitude (0.0 - 1.0) */
    private fun dbToLinear(db: Float): Float {
        return Math.pow(10.0, db / 20.0).toFloat().coerceIn(0f, 1f)
    }

    companion object {
        // Test levels in dBFS from quiet to loud for threshold finding
        val TEST_LEVELS_DB = floatArrayOf(-60f, -50f, -40f, -30f, -20f, -10f, -5f)
        val FREQUENCIES = AUDIOGRAM_FREQUENCIES
    }
}
