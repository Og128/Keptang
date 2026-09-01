package com.keptang.transcription

/**
 * Abstraction over on-device speech recognition. Android's public [android.speech.SpeechRecognizer]
 * API only supports listening to the live microphone (there is no public API to recognize an
 * already-recorded audio file), so this abstraction models a live listen session rather than a
 * "transcribe this file" call. That keeps the door open to swap in a different offline engine
 * later (e.g. one that accepts raw PCM/WAV directly) without touching [com.keptang.capture.VoiceCaptureService].
 */
interface TranscriptionProvider {

    /** Whether on-device recognition can be used right now on this device. */
    suspend fun isAvailable(): Boolean

    /**
     * Starts listening and suspends until a final result, error, or [stopListening]/[cancel]
     * ends the session. Must only be called after [isAvailable] returned true.
     *
     * [onEndOfSpeech] fires as soon as the recognizer's own endpointer decides the user has
     * stopped talking - well before the final result is ready. Callers that are also running
     * their own parallel audio capture (see [com.keptang.capture.VoiceCaptureService]) can use
     * this as the signal to stop that capture too, since the recognizer's endpointing is far
     * more robust to background noise than a raw amplitude threshold.
     */
    suspend fun listen(onEndOfSpeech: () -> Unit = {}): TranscriptionResult

    /** Requests a graceful end to the current [listen] call, returning whatever was heard so far. */
    fun stopListening()

    /** Aborts the current [listen] call immediately, discarding any partial result. */
    fun cancel()
}

sealed class TranscriptionResult {
    data class Success(val transcript: String) : TranscriptionResult()
    data class Unavailable(val reason: String) : TranscriptionResult()
    data class Failed(val reason: String) : TranscriptionResult()
    data object Cancelled : TranscriptionResult()
}
