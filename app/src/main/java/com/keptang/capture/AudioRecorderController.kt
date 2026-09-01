package com.keptang.capture

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class RecordingOutcome(val durationMillis: Long, val stopReason: StopReason)

enum class StopReason { SILENCE, MANUAL, MAX_DURATION, CANCELLED, ERROR }

/**
 * Captures raw microphone audio straight to a WAV file using [AudioRecord], so the recording is
 * durably saved incrementally rather than depending on any recognizer to persist it. Runs its
 * own amplitude-based [SilenceDetector] check on every chunk it reads.
 */
class AudioRecorderController {

    companion object {
        const val SAMPLE_RATE_HZ = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_DURATION_MILLIS = 100L
    }

    @Volatile private var manualStopRequested = false
    @Volatile private var cancelRequested = false
    @Volatile private var audioRecord: AudioRecord? = null

    fun requestManualStop() {
        manualStopRequested = true
    }

    fun requestCancel() {
        cancelRequested = true
    }

    /**
     * Records until silence, a manual stop request, or [maxDurationMillis] is reached, appending
     * raw PCM to [outputFile] as it goes and finalizing a valid WAV header when it returns.
     */
    suspend fun record(
        outputFile: File,
        maxDurationMillis: Long,
        silenceDetector: SilenceDetector,
        onAmplitude: (rms: Double) -> Unit = {}
    ): RecordingOutcome = withContext(Dispatchers.IO) {
        manualStopRequested = false
        cancelRequested = false
        silenceDetector.reset()

        val chunkSamples = (SAMPLE_RATE_HZ * CHUNK_DURATION_MILLIS / 1000L).toInt()
        val minBufferBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, ENCODING)
        val bufferSizeBytes = maxOf(minBufferBytes, chunkSamples * 2 * 4)

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE_HZ,
            CHANNEL_CONFIG,
            ENCODING,
            bufferSizeBytes
        )
        audioRecord = record

        var stopReason = StopReason.ERROR
        val startElapsed = SystemClock.elapsedRealtime()

        try {
            record.startRecording()
            RandomAccessFile(outputFile, "rw").use { raf ->
                WavFileWriter.reserveHeader(raf)

                val pcmBuffer = ShortArray(chunkSamples)
                val byteBuffer = ByteBuffer.allocate(chunkSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

                loop@ while (true) {
                    val samplesRead = record.read(pcmBuffer, 0, pcmBuffer.size)
                    if (samplesRead > 0) {
                        byteBuffer.clear()
                        for (i in 0 until samplesRead) byteBuffer.putShort(pcmBuffer[i])
                        raf.write(byteBuffer.array(), 0, samplesRead * 2)

                        val rms = SilenceDetector.rms(pcmBuffer, samplesRead)
                        onAmplitude(rms)
                        if (silenceDetector.onChunk(rms)) {
                            stopReason = StopReason.SILENCE
                            break@loop
                        }
                    }

                    if (cancelRequested) {
                        stopReason = StopReason.CANCELLED
                        break@loop
                    }
                    if (manualStopRequested) {
                        stopReason = StopReason.MANUAL
                        break@loop
                    }
                    if (SystemClock.elapsedRealtime() - startElapsed >= maxDurationMillis) {
                        stopReason = StopReason.MAX_DURATION
                        break@loop
                    }
                }

                WavFileWriter.finalize(raf, SAMPLE_RATE_HZ, channelCount = 1, bitsPerSample = 16)
            }
        } finally {
            runCatching { record.stop() }
            record.release()
            audioRecord = null
        }

        RecordingOutcome(SystemClock.elapsedRealtime() - startElapsed, stopReason)
    }
}
