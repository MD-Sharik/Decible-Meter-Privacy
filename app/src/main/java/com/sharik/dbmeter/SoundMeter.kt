package com.sharik.dbmeter

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Reads raw PCM samples from the mic and converts them to an approximate
 * dB SPL reading. Not hardware-calibrated, so treat values as relative
 * loudness rather than a certified sound-level-meter measurement.
 */
class SoundMeter {

    private var audioRecord: AudioRecord? = null
    private var bufferSize = 0
    private val sampleRate = 44100

    // Empirical offset so that normal room ambience reads in a plausible
    // 30-50 dB range and raised voices push into the 60-80 dB range.
    private val calibrationOffset = 8.0

    @SuppressLint("MissingPermission")
    fun start() {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        bufferSize = if (minBuffer > 0) minBuffer else 4096

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        ).also { it.startRecording() }
    }

    fun readDecibel(): Double {
        val record = audioRecord ?: return 0.0
        if (record.state != AudioRecord.STATE_INITIALIZED) return 0.0

        val buffer = ShortArray(bufferSize)
        val read = record.read(buffer, 0, bufferSize)
        if (read <= 0) return 0.0

        var sumOfSquares = 0.0
        for (i in 0 until read) {
            val sample = buffer[i].toDouble()
            sumOfSquares += sample * sample
        }
        val rms = sqrt(sumOfSquares / read)
        if (rms < 1.0) return 0.0

        val db = 20 * log10(rms) - calibrationOffset
        return db.coerceIn(0.0, 120.0)
    }

    fun stop() {
        audioRecord?.let {
            runCatching { it.stop() }
            it.release()
        }
        audioRecord = null
    }
}
