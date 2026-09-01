package com.keptang.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeptangDatabaseTest {

    private lateinit var db: KeptangDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KeptangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun sampleCapture(id: String, status: CaptureStatus) = CaptureEntity(
        id = id,
        capturedAtEpochMillis = 1_000L,
        timeZoneId = "Asia/Bangkok",
        audioFilePath = "/data/captures/$id.wav",
        durationMillis = 2_000L,
        rawTranscript = null,
        status = status,
        errorMessage = null,
        createdAtEpochMillis = 1_000L,
        updatedAtEpochMillis = 1_000L
    )

    @Test
    fun insertAndReadCapture() = runTest {
        db.captureDao().insert(sampleCapture("c1", CaptureStatus.CAPTURED))
        val loaded = db.captureDao().getById("c1")
        assertEquals(CaptureStatus.CAPTURED, loaded?.status)
    }

    @Test
    fun claimForProcessing_onlySucceedsOnce() = runTest {
        db.captureDao().insert(sampleCapture("c2", CaptureStatus.CAPTURED))

        val firstClaim = db.captureDao().claimForProcessing("c2", updatedAtEpochMillis = 2_000L)
        val secondClaim = db.captureDao().claimForProcessing("c2", updatedAtEpochMillis = 3_000L)

        assertTrue("first claim on a CAPTURED row should succeed", firstClaim)
        assertFalse("second claim while already TRANSCRIBING must be rejected", secondClaim)
        assertEquals(CaptureStatus.TRANSCRIBING, db.captureDao().getById("c2")?.status)
    }

    @Test
    fun claimForProcessing_rejectsNonRetryableStatus() = runTest {
        db.captureDao().insert(sampleCapture("c3", CaptureStatus.PROCESSED))
        assertFalse(db.captureDao().claimForProcessing("c3", updatedAtEpochMillis = 2_000L))
    }

    @Test
    fun replaceForCapture_isIdempotentAndNeverDuplicates() = runTest {
        db.captureDao().insert(sampleCapture("c4", CaptureStatus.PARSING))

        fun expense(id: String, minorUnits: Long) = ExpenseEntity(
            id = id,
            captureId = "c4",
            amountMinorUnits = minorUnits,
            currencyCode = "THB",
            occurredAtEpochMillis = 1_000L,
            timeZoneId = "Asia/Bangkok",
            category = "Dining",
            account = null,
            paymentMethod = null,
            merchant = "Dinner",
            confidence = 1f,
            reviewStatus = ReviewStatus.APPROVED,
            createdAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 1_000L
        )

        db.expenseDao().replaceForCapture("c4", listOf(expense("e1", 5000), expense("e2", 2500)))
        assertEquals(2, db.expenseDao().getByCaptureId("c4").size)

        // Simulate a retry that reprocesses the same transcript into a fresh set of expenses.
        db.expenseDao().replaceForCapture("c4", listOf(expense("e3", 5000)))
        val afterRetry = db.expenseDao().getByCaptureId("c4")
        assertEquals("retry must replace, not append", 1, afterRetry.size)
        assertEquals("e3", afterRetry[0].id)
    }

    @Test
    fun deletingCapture_cascadesToItsExpenses() = runTest {
        db.captureDao().insert(sampleCapture("c5", CaptureStatus.PROCESSED))
        val expense = ExpenseEntity(
            id = "e5",
            captureId = "c5",
            amountMinorUnits = 1000,
            currencyCode = "THB",
            occurredAtEpochMillis = 1_000L,
            timeZoneId = "Asia/Bangkok",
            category = "Coffee",
            account = null,
            paymentMethod = null,
            merchant = "Coffee",
            confidence = 1f,
            reviewStatus = ReviewStatus.APPROVED,
            createdAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 1_000L
        )
        db.expenseDao().insertAll(listOf(expense))

        db.captureDao().deleteById("c5")

        assertTrue(db.expenseDao().getByCaptureId("c5").isEmpty())
    }
}
