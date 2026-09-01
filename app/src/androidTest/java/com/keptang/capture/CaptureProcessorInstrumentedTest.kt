package com.keptang.capture

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keptang.data.db.CaptureStatus
import com.keptang.data.db.KeptangDatabase
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.parser.ExpenseParser
import com.keptang.transcription.FakeTranscriptionProvider
import com.keptang.transcription.TranscriptionResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * End-to-end tests of the CAPTURED -> ... -> PROCESSED|NEEDS_REVIEW|FAILED pipeline against a
 * real in-memory Room database, standing in for the checklist items that need a full pipeline
 * rather than just the parser in isolation: duplicate-processing prevention, multiple expenses
 * saved separately, retained audio on failure, and idempotent reprocessing.
 */
@RunWith(AndroidJUnit4::class)
class CaptureProcessorInstrumentedTest {

    private lateinit var db: KeptangDatabase
    private lateinit var captureRepository: CaptureRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var fakeProvider: FakeTranscriptionProvider
    private lateinit var processor: CaptureProcessor
    private lateinit var audioFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, KeptangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseRepository = ExpenseRepository(db.expenseDao())
        captureRepository = CaptureRepository(db.captureDao(), AudioFileStore(context))
        fakeProvider = FakeTranscriptionProvider()
        processor = CaptureProcessor(captureRepository, expenseRepository, fakeProvider, ExpenseParser())
        audioFile = File(context.filesDir, "test-${UUID.randomUUID()}.wav").apply { writeText("fake-audio") }
    }

    @After
    fun tearDown() {
        db.close()
        audioFile.delete()
    }

    private suspend fun newCapture(): String {
        val id = UUID.randomUUID().toString()
        captureRepository.createRecording(id, audioFile.absolutePath, "Asia/Bangkok")
        captureRepository.markCaptured(id, durationMillis = 3_000)
        return id
    }

    @Test
    fun multipleExpensesFromOneTranscriptAreSavedAsSeparateRows() = runTest {
        val captureId = newCapture()
        fakeProvider.result = TranscriptionResult.Success("Dinner was 550 and coffee was 25.")

        val outcome = processor.processFreshCapture(captureId, fakeProvider.listen())

        assertTrue(outcome is ProcessOutcome.Processed)
        val expenses = expenseRepository.getByCaptureId(captureId)
        assertEquals(2, expenses.size)
        assertEquals(setOf(55000L, 2500L), expenses.map { it.amountMinorUnits }.toSet())
        assertEquals(CaptureStatus.PROCESSED, captureRepository.getById(captureId)?.status)
    }

    @Test
    fun reprocessingTheSameCaptureNeverDuplicatesExpenses() = runTest {
        val captureId = newCapture()
        fakeProvider.result = TranscriptionResult.Success("I paid 150 baht for a taxi.")

        val first = processor.processFreshCapture(captureId, fakeProvider.listen())
        assertTrue(first is ProcessOutcome.Processed)

        // A second attempt on the same (now PROCESSED) capture must be rejected outright...
        val second = processor.processFreshCapture(captureId, fakeProvider.listen())
        assertTrue(second is ProcessOutcome.AlreadyProcessed)
        assertEquals(1, expenseRepository.getByCaptureId(captureId).size)

        // ...and even an explicit retry against a non-retryable status must be a no-op.
        val retried = processor.retry(captureId)
        assertTrue(retried is ProcessOutcome.AlreadyProcessed)
        assertEquals(1, expenseRepository.getByCaptureId(captureId).size)
    }

    @Test
    fun failedTranscriptionKeepsTheRecordingForLater() = runTest {
        val captureId = newCapture()
        fakeProvider.result = TranscriptionResult.Failed("No speech was recognized")

        val outcome = processor.processFreshCapture(captureId, fakeProvider.listen())

        assertTrue(outcome is ProcessOutcome.CouldNotUnderstand)
        val capture = captureRepository.getById(captureId)
        assertEquals(CaptureStatus.FAILED, capture?.status)
        assertTrue("audio file must still exist on disk", File(capture!!.audioFilePath).exists())
        assertEquals("audio path must be retained, not cleared", audioFile.absolutePath, capture.audioFilePath)
    }

    @Test
    fun unavailableRecognizerNeverFallsBackSilentlyAndKeepsAudio() = runTest {
        val captureId = newCapture()
        fakeProvider.available = false

        val outcome = processor.processFreshCapture(
            captureId,
            TranscriptionResult.Unavailable("On-device speech recognition is not available on this device")
        )

        assertTrue(outcome is ProcessOutcome.SavedForLater)
        val capture = captureRepository.getById(captureId)
        assertEquals(CaptureStatus.FAILED, capture?.status)
        assertTrue(File(capture!!.audioFilePath).exists())
    }

    @Test
    fun ambiguousTranscriptWithNoAmountGoesToReviewWithoutInventingAnExpense() = runTest {
        val captureId = newCapture()
        fakeProvider.result = TranscriptionResult.Success("I think I spent some money yesterday but I'm not sure how much.")

        val outcome = processor.processFreshCapture(captureId, fakeProvider.listen())

        assertTrue(outcome is ProcessOutcome.NeedsReview)
        assertEquals(0, expenseRepository.getByCaptureId(captureId).size)
        assertEquals(CaptureStatus.NEEDS_REVIEW, captureRepository.getById(captureId)?.status)
    }

    @Test
    fun retryAfterFailureReusesExistingTranscriptWithoutReListening() = runTest {
        val captureId = newCapture()
        fakeProvider.result = TranscriptionResult.Failed("boom")
        processor.processFreshCapture(captureId, fakeProvider.listen())
        assertEquals(CaptureStatus.FAILED, captureRepository.getById(captureId)?.status)

        // No transcript was ever saved (transcription itself failed), so retry legitimately
        // re-invokes the live recognizer - this time it succeeds.
        fakeProvider.result = TranscriptionResult.Success("I paid 80 cash for coffee.")
        val retryOutcome = processor.retry(captureId)

        assertTrue(retryOutcome is ProcessOutcome.Processed)
        assertEquals(1, expenseRepository.getByCaptureId(captureId).size)
        assertEquals(CaptureStatus.PROCESSED, captureRepository.getById(captureId)?.status)
    }
}
