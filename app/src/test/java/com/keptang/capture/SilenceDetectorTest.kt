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

        assertFalse(detector.onChunk(rms = 3000.0)) // speech

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

        detector.onChunk(rms = 3000.0)
        repeat(10) { detector.onChunk(rms = 10.0) } // 1000ms silence, not enough yet
        assertFalse(detector.onChunk(rms = 3000.0)) // speaks again, resets counter

        repeat(10) { // another 1000ms - still short of the 1500ms requirement
            assertFalse(detector.onChunk(rms = 10.0))
        }
    }

    @Test
    fun `high ambient noise floor raises the effective threshold so speech still ends in silence`() {
        val detector = SilenceDetector(
            silenceThresholdRms = 180.0,
            requiredSilenceDurationMillis = 1500,
            chunkDurationMillis = 100,
            noiseFloorMarginRms = 150.0
        )

        // Leading silence in a noisy room: well above the static 180 default, so a fixed
        // threshold would never see this room's silence as silence.
        repeat(10) { assertFalse(detector.onChunk(rms = 400.0)) }

        assertFalse(detector.onChunk(rms = 3000.0)) // speech

        // Silence at this room's ambient level (400) - below the calibrated threshold
        // (400 + 150 margin = 550) so it must count as trailing silence, not speech.
        var stopped = false
        repeat(15) { // 15 * 100ms = 1500ms
            stopped = detector.onChunk(rms = 400.0)
        }

        assertTrue(stopped)
    }

    @Test
    fun `calibrated threshold never drops below the static floor in a near-silent room`() {
        val detector = SilenceDetector(
            silenceThresholdRms = 180.0,
            requiredSilenceDurationMillis = 1500,
            chunkDurationMillis = 100,
            noiseFloorMarginRms = 150.0
        )

        repeat(10) { detector.onChunk(rms = 5.0) } // near-silent leading period, floor ~5
        assertFalse(detector.onChunk(rms = 3000.0)) // speech

        // A quiet breath at 100 rms should still count as speech since it's above the static
        // 180 floor - the low ambient measurement must not pull the threshold down below that.
        assertFalse(detector.onChunk(rms = 200.0))
    }
}
