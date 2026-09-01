package com.keptang.transcription

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Wraps Android's on-device [SpeechRecognizer]. [SpeechRecognizer] must be created and driven
 * from a thread with a [Looper] (we use the main thread), and it only supports listening to the
 * live microphone - see [TranscriptionProvider] for why this shapes the interface the way it does.
 */
class AndroidSpeechRecognitionProvider(private val context: Context) : TranscriptionProvider {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var activeRecognizer: SpeechRecognizer? = null

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else {
            // Pre-API 31 there is no public on-device-only capability check; EXTRA_PREFER_OFFLINE
            // below is the best available hint, so we fall back to the general availability check.
            SpeechRecognizer.isRecognitionAvailable(context)
        }
    }

    override suspend fun listen(): TranscriptionResult = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            activeRecognizer = recognizer

            fun finish(result: TranscriptionResult) {
                if (continuation.isActive) continuation.resume(result)
                activeRecognizer = null
                recognizer.destroy()
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val transcript = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                    if (transcript.isNullOrBlank()) {
                        finish(TranscriptionResult.Failed("No speech was recognized"))
                    } else {
                        finish(TranscriptionResult.Success(transcript))
                    }
                }

                override fun onError(error: Int) {
                    finish(TranscriptionResult.Failed(describeError(error)))
                }

                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            continuation.invokeOnCancellation {
                mainHandler.post { runCatching { recognizer.cancel(); recognizer.destroy() } }
                activeRecognizer = null
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer.startListening(intent)
        }
    }

    override fun stopListening() {
        mainHandler.post { runCatching { activeRecognizer?.stopListening() } }
    }

    override fun cancel() {
        mainHandler.post { runCatching { activeRecognizer?.cancel() } }
    }

    private fun describeError(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognized"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error"
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Recognizer client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing microphone permission"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
        SpeechRecognizer.ERROR_SERVER -> "Recognition service error"
        else -> "Unknown recognition error ($code)"
    }
}
