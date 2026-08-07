package com.sharik.dbmeter

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Reads raw PCM samples from the mic and converts them to approximate dB SPL.
 *
 * Samples are normalised to dBFS first -- decibels relative to full scale, so
 * 0 dBFS is the loudest signal 16-bit PCM can represent -- and only then
 * shifted onto an SPL scale by [calibrationOffset]. That normalisation is what
 * gives the meter its range: taking 20*log10 of the raw RMS instead caps the
 * reading at 20*log10(32768) = 90.3 dB before any offset, which put anything
 * above roughly 82 dB out of reach.
 *
 * Phone mics are not calibrated instruments and vary by well over 10 dB
 * between devices, so [calibrationOffset] is meant to be user adjustable.
 * Treat readings as a close approximation, not a certified measurement.
 */
class SoundMeter(calibrationOffset: Double = DEFAULT_CALIBRATION_OFFSET) {

    /** dB added to the dBFS reading to place it on the SPL scale. */
    @Volatile
    var calibrationOffset: Double = calibrationOffset

    private var audioRecord: AudioRecord? = null
    private var bufferSize = 0
    private val sampleRate = 44100

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

    /**
     * Current level in dB SPL, or null when no audio was available to measure.
     *
     * Callers must skip nulls rather than substituting zero. The first read or
     * two after [start] routinely come back empty, and folding those into the
     * session statistics pins MIN at 0 dB for as long as the app is running.
     */
    fun readDecibel(): Double? {
        val record = audioRecord ?: return null
        if (record.state != AudioRecord.STATE_INITIALIZED) return null

        val buffer = ShortArray(bufferSize)
        val read = record.read(buffer, 0, bufferSize)
        if (read <= 0) return null

        var sumOfSquares = 0.0
        for (i in 0 until read) {
            val sample = buffer[i].toDouble()
            sumOfSquares += sample * sample
        }

        // Floor the RMS at one quantisation step: digital silence is a real
        // reading (the bottom of the scale), not a missing one.
        val rms = max(sqrt(sumOfSquares / read), 1.0)
        val dbfs = 20 * log10(rms / MAX_AMPLITUDE)

        return (dbfs + calibrationOffset).coerceIn(0.0, MAX_DB)
    }

    fun stop() {
        audioRecord?.let {
            runCatching { it.stop() }
            it.release()
        }
        audioRecord = null
    }

    companion object {
        /** Full scale for 16-bit PCM. */
        private const val MAX_AMPLITUDE = 32767.0

        private const val MAX_DB = 120.0

        /**
         * Places a full-scale signal at about 94 dB SPL, the standard 1 Pa
         * reference and a sane starting point for a phone mic.
         */
        const val DEFAULT_CALIBRATION_OFFSET = 94.0

        /** How far either side of the default a user may trim the reading. */
        const val MAX_ADJUSTMENT = 15f
    }
}
