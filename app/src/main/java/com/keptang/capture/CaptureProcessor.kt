package com.keptang.capture

import com.keptang.core.Defaults
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.parser.ExpenseParser
import com.keptang.transcription.TranscriptionProvider
import com.keptang.transcription.TranscriptionResult
import java.time.ZoneId
import java.time.ZonedDateTime

sealed class ProcessOutcome {
    data class Processed(val approvedCount: Int) : ProcessOutcome()
    data class NeedsReview(val approvedCount: Int, val reviewCount: Int) : ProcessOutcome()
    data object SavedForLater : ProcessOutcome()
    data object CouldNotUnderstand : ProcessOutcome()
    data object AlreadyProcessed : ProcessOutcome()
    data object NotFound : ProcessOutcome()
}

/**
 * Owns the CAPTURED -> TRANSCRIBING -> PARSING -> (PROCESSED | NEEDS_REVIEW | FAILED) pipeline,
 * shared by [com.keptang.capture.VoiceCaptureService] (fresh captures, where transcription
 * already ran live alongside recording) and the in-app Retry action (previously-failed
 * captures). [CaptureRepository.claimForProcessing] guarantees a capture is only ever
 * transitioned out of a retryable state once, and [ExpenseRepository.saveParsedExpenses]
 * replaces rather than appends, so retrying never creates duplicate expenses.
 */
class CaptureProcessor(
    private val captureRepository: CaptureRepository,
    private val expenseRepository: ExpenseRepository,
    private val transcriptionProvider: TranscriptionProvider,
    private val expenseParser: ExpenseParser,
    private val zoneId: ZoneId = ZoneId.of(Defaults.TIME_ZONE_ID),
    private val languageCodeProvider: () -> String = { Defaults.LANGUAGE_CODE }
) {

    /** Used by the service right after recording, when a live [TranscriptionResult] is already in hand. */
    suspend fun processFreshCapture(captureId: String, transcriptionResult: TranscriptionResult): ProcessOutcome {
        if (!captureRepository.claimForProcessing(captureId)) return ProcessOutcome.AlreadyProcessed
        return finalize(captureId, transcriptionResult)
    }

    /**
     * Used by the in-app Retry action. If a transcript was already captured (parsing itself was
     * the problem), it is re-parsed as-is. Otherwise this re-invokes the live recognizer - see
     * README "Known issues" for why a saved recording can't be fed back into [TranscriptionProvider]
     * directly on Android.
     */
    suspend fun retry(captureId: String): ProcessOutcome {
        val capture = captureRepository.getById(captureId) ?: return ProcessOutcome.NotFound
        if (!captureRepository.claimForProcessing(captureId)) return ProcessOutcome.AlreadyProcessed

        val existingTranscript = capture.rawTranscript
        val result = when {
            !existingTranscript.isNullOrBlank() -> TranscriptionResult.Success(existingTranscript)
            transcriptionProvider.isAvailable() -> transcriptionProvider.listen()
            else -> TranscriptionResult.Unavailable("On-device recognition unavailable on this device")
        }
        return finalize(captureId, result)
    }

    private suspend fun finalize(captureId: String, result: TranscriptionResult): ProcessOutcome =
        when (result) {
            is TranscriptionResult.Success -> finalizeSuccess(captureId, result.transcript)
            is TranscriptionResult.Unavailable -> {
                captureRepository.markFailed(captureId, result.reason)
                ProcessOutcome.SavedForLater
            }
            is TranscriptionResult.Failed -> {
                captureRepository.markFailed(captureId, result.reason)
                ProcessOutcome.CouldNotUnderstand
            }
            TranscriptionResult.Cancelled -> {
                captureRepository.markFailed(captureId, "Transcription was cancelled")
                ProcessOutcome.CouldNotUnderstand
            }
        }

    private suspend fun finalizeSuccess(captureId: String, transcript: String): ProcessOutcome {
        captureRepository.markParsing(captureId, transcript)
        val now = ZonedDateTime.now(zoneId)
        val parsed = expenseParser.parse(transcript, captureId, now, languageCodeProvider())
        expenseRepository.saveParsedExpenses(captureId, parsed)

        return when {
            parsed.isEmpty() -> {
                captureRepository.markNeedsReview(captureId, "No expenses could be confidently understood")
                ProcessOutcome.NeedsReview(approvedCount = 0, reviewCount = 0)
            }
            parsed.any { it.needsReview } -> {
                captureRepository.markNeedsReview(captureId)
                ProcessOutcome.NeedsReview(
                    approvedCount = parsed.count { !it.needsReview },
                    reviewCount = parsed.count { it.needsReview }
                )
            }
            else -> {
                captureRepository.markProcessed(captureId)
                ProcessOutcome.Processed(approvedCount = parsed.size)
            }
        }
    }
}
