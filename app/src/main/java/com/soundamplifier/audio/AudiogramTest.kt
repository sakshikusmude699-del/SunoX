package com.soundamplifier.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.soundamplifier.data.AUDIOGRAM_FREQUENCIES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

enum class AudiogramEar {
    LEFT,
    RIGHT,
}

/**
 * Plays pure sine tone bursts for audiogram threshold testing.
 * One channel active (left or right); the other is silent for headphone routing.
 */
class AudiogramTest {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null

    /** Standard test frequencies (Hz). */
    val frequencies: IntArray = AUDIOGRAM_FREQUENCIES

    /**
     * Play one burst: [toneSeconds] tone, then [silenceSeconds] silence. Blocking on IO dispatcher.
     * [dbHl] Hearing level 0 (sensitive) .. 80 (loud); mapped to safe playback level.
     */
    suspend fun playToneBurst(
        frequencyHz: Int,
        dbHl: Int,
        ear: AudiogramEar,
        toneSeconds: Float = 1f,
        silenceSeconds: Float = 0.5f,
    ) = withContext(Dispatchers.IO) {
        stopTone()

        val dbfs = dbHlToDbfs(dbHl.coerceIn(0, 100))
        val amplitude = dbToLinear(dbfs)

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(1024)

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
            .setBufferSizeInBytes(minBuf * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val track = audioTrack ?: return@withContext
        track.play()

        val onSamples = (sampleRate * toneSeconds).toInt()
        val offSamples = (sampleRate * silenceSeconds).toInt()
        val totalFrames = onSamples + offSamples
        var t = 0
        var phase = 0.0
        while (t < totalFrames) {
            val nFrames = minOf(512, totalFrames - t)
            val buf = FloatArray(nFrames * 2)
            for (i in 0 until nFrames) {
                val frameIdx = t + i
                val s = if (frameIdx < onSamples) {
                    (amplitude * sin(2.0 * PI * frequencyHz * phase / sampleRate)).toFloat()
                } else {
                    0f
                }
                phase += 1.0
                if (ear == AudiogramEar.LEFT) {
                    buf[i * 2] = s
                    buf[i * 2 + 1] = 0f
                } else {
                    buf[i * 2] = 0f
                    buf[i * 2 + 1] = s
                }
            }
            var written = 0
            while (written < buf.size) {
                val r = track.write(buf, written, buf.size - written, AudioTrack.WRITE_BLOCKING)
                if (r < 0) break
                written += r
            }
            t += nFrames
        }

        track.stop()
        track.release()
        audioTrack = null
    }

    fun stopTone() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    /** 0 dB HL ≈ very quiet; 80 dB HL ≈ loud but safe on phones with headphones. */
    private fun dbHlToDbfs(dbHl: Int): Float {
        val x = dbHl.coerceIn(0, 80) / 80f
        return -52f + x * 32f
    }

    private fun dbToLinear(db: Float): Float =
        10.0.pow((db / 20.0)).toFloat().coerceIn(1e-5f, 1f)

    companion object {
        val FREQUENCIES: IntArray = AUDIOGRAM_FREQUENCIES
        @Deprecated("Use instance frequencies or AUDIOGRAM_FREQUENCIES")
        val TEST_LEVELS_DB = floatArrayOf(-60f, -50f, -40f, -30f, -20f, -10f, -5f)
    }
}
