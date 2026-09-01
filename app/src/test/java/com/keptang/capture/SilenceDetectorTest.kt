package com.keptang.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceDetectorTest {

    @Test
    fun `leading silence before any speech never triggers a stop`() {
        val detector = SilenceDetector(
            silenceThresholdRms = 700.0,
            requiredSilenceDurationMillis = 1500,
            chunkDurationMillis = 100
        )

        repeat(50) {
            assertFalse(detector.onChunk(rms = 10.0))
        }
    }

    @Test
    fun `stops after required silence duration once speech has been heard`() {
        val detector = SilenceDetector(
            silenceThresholdRms = 700.0,
            requiredSilenceDurationMillis = 1500,
            chunkDurationMillis = 100
        )

        assertFalse(detector.onChunk(rms = 2000.0)) // speech

        var stopped = false
        repeat(15) { // 15 * 100ms = 1500ms of silence
            stopped = detector.onChunk(rms = 10.0)
        }

        assertTrue(stopped)
    }

    @Test
    fun `speech resets the silence counter`() {
        val detector = SilenceDetector(
            silenceThresholdRms = 700.0,
            requiredSilenceDurationMillis = 1500,
            chunkDurationMillis = 100
        )

        detector.onChunk(rms = 2000.0)
        repeat(10) { detector.onChunk(rms = 10.0) } // 1000ms silence, not enough yet
        assertFalse(detector.onChunk(rms = 2000.0)) // speaks again, resets counter

        repeat(10) { // another 1000ms - still short of the 1500ms requirement
            assertFalse(detector.onChunk(rms = 10.0))
        }
    }
}
