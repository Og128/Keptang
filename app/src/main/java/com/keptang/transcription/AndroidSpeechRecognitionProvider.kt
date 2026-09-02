package com.keptang.transcription

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.keptang.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/**
 * Wraps Android's on-device [SpeechRecognizer]. [SpeechRecognizer] must be created and driven
 * from a thread with a [Looper] (we use the main thread), and it only supports listening to the
 * live microphone - see [TranscriptionProvider] for why this shapes the interface the way it does.
 */
class AndroidSpeechRecognitionProvider(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : TranscriptionProvider {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { command -> mainHandler.post(command) }

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

    override suspend fun listen(onEndOfSpeech: () -> Unit): TranscriptionResult = withContext<TranscriptionResult>(Dispatchers.Main) {
        val endOfSpeechCallback = onEndOfSpeech
        val recognitionLanguage = recognitionLanguageTag(settingsRepository.settings.first().languageCode)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLanguage)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        // The on-device recognizer's language model is a separate download from anything
        // shown under Settings > on-device speech recognition (that page manages the
        // Assistant's own dictation models, not this API's). If ours isn't installed,
        // startListening() would otherwise fail immediately with ERROR_LANGUAGE_UNAVAILABLE;
        // trigger the download instead so a subsequent retry has a chance to succeed.
        val modelMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !isModelInstalled(intent, recognitionLanguage)

        if (modelMissing) {
            TranscriptionResult.Failed(
                "On-device $recognitionLanguage speech model isn't installed yet; download was just triggered - retry in a minute"
            )
        } else suspendCancellableCoroutine<TranscriptionResult> { continuation ->
            val recognizer = newRecognizer()
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
                override fun onEndOfSpeech() = endOfSpeechCallback()
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            continuation.invokeOnCancellation {
                mainHandler.post { runCatching { recognizer.cancel(); recognizer.destroy() } }
                activeRecognizer = null
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

    // createSpeechRecognizer() resolves to whatever the device has configured as its default
    // recognition service, which on some devices is a cloud-backed or TTS-adjacent service
    // that doesn't honor EXTRA_PREFER_OFFLINE. createOnDeviceSpeechRecognizer() asks for the
    // platform's actual on-device recognizer instead, matching what isAvailable() checked for.
    private fun newRecognizer(): SpeechRecognizer =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

    @Suppress("NewApi") // only called under an SDK_INT >= TIRAMISU guard, see listen() above
    private suspend fun isModelInstalled(intent: Intent, recognitionLanguage: String): Boolean =
        suspendCancellableCoroutine { continuation ->
        val recognizer = newRecognizer()
        recognizer.checkRecognitionSupport(
            intent,
            mainExecutor,
            object : RecognitionSupportCallback {
                override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                    val installed = recognitionSupport.installedOnDeviceLanguages.contains(recognitionLanguage)
                    if (installed) {
                        recognizer.destroy()
                    } else {
                        runCatching { recognizer.triggerModelDownload(intent) }
                        // destroy() right after triggerModelDownload() can tear down the
                        // service connection before that async request is actually dispatched,
                        // silently dropping it. Give it a moment to go out first.
                        mainHandler.postDelayed({ recognizer.destroy() }, 1000)
                    }
                    if (continuation.isActive) continuation.resume(installed)
                }

                override fun onError(error: Int) {
                    recognizer.destroy()
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        )
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
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported by the recognizer"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Requested language's offline model is unavailable"
        else -> "Unknown recognition error ($code)"
    }

    private fun recognitionLanguageTag(languageCode: String): String = when (languageCode) {
        "fr" -> "fr-FR"
        else -> "en-US"
    }
}
