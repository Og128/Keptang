package com.keptang.capture

import kotlin.math.sqrt

/**
 * Tracks trailing silence across a stream of audio chunks and reports when the caller should
 * stop recording. Pure Kotlin/JVM logic (no Android dependency) so it is trivially unit-testable.
 * Leading silence (before any speech has been heard at all) never triggers a stop, so recording
 * doesn't immediately end if the user takes a beat to start talking.
 */
class SilenceDetector(
    private val silenceThresholdRms: Double = 700.0,
    private val requiredSilenceDurationMillis: Long = 1500,
    private val chunkDurationMillis: Long = 100
) {
    private var silenceAccumulatedMillis = 0L
    private var hasHeardSpeech = false

    fun reset() {
        silenceAccumulatedMillis = 0L
        hasHeardSpeech = false
    }

    /** Feed the RMS amplitude of one chunk. Returns true once enough trailing silence has passed. */
    fun onChunk(rms: Double): Boolean {
        if (rms >= silenceThresholdRms) {
            hasHeardSpeech = true
            silenceAccumulatedMillis = 0L
            return false
        }
        if (!hasHeardSpeech) return false
        silenceAccumulatedMillis += chunkDurationMillis
        return silenceAccumulatedMillis >= requiredSilenceDurationMillis
    }

    companion object {
        fun rms(buffer: ShortArray, length: Int): Double {
            if (length <= 0) return 0.0
            var sum = 0.0
            for (i in 0 until length) {
                val sample = buffer[i].toDouble()
                sum += sample * sample
            }
            return sqrt(sum / length)
        }
    }
}
