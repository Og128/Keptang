package com.keptang.data.repository

import com.keptang.capture.AudioFileStore
import com.keptang.data.db.CaptureDao
import com.keptang.data.db.CaptureEntity
import com.keptang.data.db.CaptureStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Expense rows for a deleted capture are removed automatically via [com.keptang.data.db.ExpenseEntity]'s cascading foreign key. */
class CaptureRepository(
    private val captureDao: CaptureDao,
    private val audioFileStore: AudioFileStore
) {

    fun observeAll(): Flow<List<CaptureEntity>> = captureDao.observeAll()

    fun observeByStatuses(statuses: List<CaptureStatus>): Flow<List<CaptureEntity>> =
        captureDao.observeByStatuses(statuses)

    fun observeById(id: String): Flow<CaptureEntity?> = captureDao.observeById(id)

    suspend fun getById(id: String): CaptureEntity? = captureDao.getById(id)

    suspend fun createRecording(id: String, audioFilePath: String, timeZoneId: String) {
        val now = Instant.now().toEpochMilli()
        captureDao.insert(
            CaptureEntity(
                id = id,
                capturedAtEpochMillis = now,
                timeZoneId = timeZoneId,
                audioFilePath = audioFilePath,
                durationMillis = 0,
                rawTranscript = null,
                status = CaptureStatus.RECORDING,
                errorMessage = null,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
        )
    }

    suspend fun markCaptured(id: String, durationMillis: Long) {
        val current = captureDao.getById(id) ?: return
        captureDao.update(
            current.copy(
                status = CaptureStatus.CAPTURED,
                durationMillis = durationMillis,
                updatedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    /** Attempts to atomically move a capture into TRANSCRIBING. False means it is already being processed or done. */
    suspend fun claimForProcessing(id: String): Boolean =
        captureDao.claimForProcessing(id, Instant.now().toEpochMilli())

    suspend fun markParsing(id: String, transcript: String) {
        val current = captureDao.getById(id) ?: return
        captureDao.update(
            current.copy(
                status = CaptureStatus.PARSING,
                rawTranscript = transcript,
                updatedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun markProcessed(id: String) = setStatus(id, CaptureStatus.PROCESSED, errorMessage = null)

    suspend fun markNeedsReview(id: String, reason: String? = null) =
        setStatus(id, CaptureStatus.NEEDS_REVIEW, errorMessage = reason)

    suspend fun markFailed(id: String, errorMessage: String) =
        setStatus(id, CaptureStatus.FAILED, errorMessage = errorMessage)

    suspend fun markCancelled(id: String) {
        val current = captureDao.getById(id) ?: return
        audioFileStore.delete(current.audioFilePath)
        captureDao.update(
            current.copy(status = CaptureStatus.CANCELLED, updatedAtEpochMillis = Instant.now().toEpochMilli())
        )
    }

    private suspend fun setStatus(id: String, status: CaptureStatus, errorMessage: String?) {
        val current = captureDao.getById(id) ?: return
        captureDao.update(
            current.copy(status = status, errorMessage = errorMessage, updatedAtEpochMillis = Instant.now().toEpochMilli())
        )
    }

    suspend fun delete(id: String) {
        val current = captureDao.getById(id) ?: return
        audioFileStore.delete(current.audioFilePath)
        captureDao.deleteById(id)
    }

    /**
     * Deletes audio files for captures that finished successfully more than [retentionDays]
     * ago. Failed or not-yet-reviewed captures are never touched, per spec. The row itself
     * (transcript, expenses, history) is kept - only the audio file and its path are cleared.
     */
    suspend fun purgeExpiredAudio(retentionDays: Int) {
        val cutoff = Instant.now().toEpochMilli() - retentionDays * 24L * 60L * 60L * 1000L
        val expired = captureDao.getExpired(CaptureStatus.PROCESSED, cutoff)
        for (capture in expired) {
            audioFileStore.delete(capture.audioFilePath)
            captureDao.update(
                capture.copy(audioFilePath = "", updatedAtEpochMillis = Instant.now().toEpochMilli())
            )
        }
    }
}
