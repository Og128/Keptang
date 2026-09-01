package com.keptang.capture

import kotlin.math.sqrt

/**
 * Tracks trailing silence across a stream of audio chunks and reports when the caller should
 * stop recording. Pure Kotlin/JVM logic (no Android dependency) so it is trivially unit-testable.
 * Leading silence (before any speech has been heard at all) never triggers a stop, so recording
 * doesn't immediately end if the user takes a beat to start talking.
 *
 * A single fixed RMS threshold isn't reliable across devices/rooms - mic sensitivity and ambient
 * noise floor vary enough that a constant which works in one environment can sit permanently
 * below the ambient level in another, in which case "speech" never stops being detected and
 * trailing silence never accumulates. To avoid that, every chunk seen before speech is first
 * detected is treated as ambient-noise sampling: it feeds an exponential moving average that
 * becomes [noiseFloorRms], and the actual speech/silence threshold for the rest of the recording
 * is [noiseFloorRms] + [noiseFloorMarginRms], floored at [silenceThresholdRms] so a near-silent
 * room doesn't make the detector trigger on breath noise. Before any ambient sample has been
 * collected there's nothing to calibrate against yet, so that very first chunk is judged against
 * [silenceThresholdRms] * [uncalibratedThresholdMultiplier] instead of the bare static floor -
 * high enough that ordinary ambient noise doesn't get mistaken for speech and skip calibration
 * before it has a chance to run, while unambiguous speech (usually an order of magnitude above
 * ambient) still clears it immediately.
 */
class SilenceDetector(
    private val silenceThresholdRms: Double = 180.0,
    private val requiredSilenceDurationMillis: Long = 1500,
    private val chunkDurationMillis: Long = 100,
    private val noiseFloorMarginRms: Double = 150.0,
    private val noiseFloorSmoothingFactor: Double = 0.2,
    private val uncalibratedThresholdMultiplier: Double = 3.0
) {
    private var silenceAccumulatedMillis = 0L
    private var hasHeardSpeech = false
    private var noiseFloorRms = 0.0
    private var noiseFloorInitialized = false

    fun reset() {
        silenceAccumulatedMillis = 0L
        hasHeardSpeech = false
        noiseFloorRms = 0.0
        noiseFloorInitialized = false
    }

    /** Feed the RMS amplitude of one chunk. Returns true once enough trailing silence has passed. */
    fun onChunk(rms: Double): Boolean {
        val threshold = effectiveThreshold()
        if (rms >= threshold) {
            hasHeardSpeech = true
            silenceAccumulatedMillis = 0L
            return false
        }
        if (!hasHeardSpeech) {
            noiseFloorRms = if (!noiseFloorInitialized) rms else noiseFloorRms + noiseFloorSmoothingFactor * (rms - noiseFloorRms)
            noiseFloorInitialized = true
            return false
        }
        silenceAccumulatedMillis += chunkDurationMillis
        return silenceAccumulatedMillis >= requiredSilenceDurationMillis
    }

    private fun effectiveThreshold(): Double = if (noiseFloorInitialized) {
        maxOf(silenceThresholdRms, noiseFloorRms + noiseFloorMarginRms)
    } else {
        silenceThresholdRms * uncalibratedThresholdMultiplier
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
