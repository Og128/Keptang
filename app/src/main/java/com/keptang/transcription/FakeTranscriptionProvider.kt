package com.keptang.transcription

/**
 * Debug/test double for [TranscriptionProvider]. Returns a caller-configured result
 * immediately instead of touching the microphone or [android.speech.SpeechRecognizer], so
 * capture-pipeline behavior (status transitions, parsing, notifications) can be exercised in
 * automated tests without a device recognizer.
 */
class FakeTranscriptionProvider(
    var available: Boolean = true,
    var result: TranscriptionResult = TranscriptionResult.Failed("No fake result configured")
) : TranscriptionProvider {

    var listenCallCount: Int = 0
        private set
    var stopListeningCalled: Boolean = false
        private set
    var cancelCalled: Boolean = false
        private set

    override suspend fun isAvailable(): Boolean = available

    override suspend fun listen(onEndOfSpeech: () -> Unit): TranscriptionResult {
        listenCallCount++
        if (!available) return TranscriptionResult.Unavailable("Fake provider configured as unavailable")
        return result
    }

    override fun stopListening() {
        stopListeningCalled = true
    }

    override fun cancel() {
        cancelCalled = true
    }
}
