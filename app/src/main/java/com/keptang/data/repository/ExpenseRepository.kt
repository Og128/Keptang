package com.keptang.data.repository

import androidx.room.withTransaction
import com.keptang.account.AccountResolver
import com.keptang.data.db.CaptureDao
import com.keptang.data.db.ExpenseDao
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.db.KeptangDatabase
import com.keptang.data.db.PaymentMethod
import com.keptang.data.db.ReviewStatus
import com.keptang.parser.ParsedExpense
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

class ExpenseRepository(
    private val database: KeptangDatabase,
    private val expenseDao: ExpenseDao,
    private val captureDao: CaptureDao,
    private val accountResolver: AccountResolver
) {

    fun observeAll(): Flow<List<ExpenseEntity>> = expenseDao.observeAll()

    fun observeNeedsReview(): Flow<List<ExpenseEntity>> =
        expenseDao.observeByReviewStatus(ReviewStatus.NEEDS_REVIEW)

    fun observeApproved(): Flow<List<ExpenseEntity>> =
        expenseDao.observeByReviewStatus(ReviewStatus.APPROVED)

    suspend fun getByCaptureId(captureId: String): List<ExpenseEntity> =
        expenseDao.getByCaptureId(captureId)

    suspend fun getById(id: String): ExpenseEntity? = expenseDao.getById(id)

    /** Most recently created expenses, for the Dashboard's "recent" list. */
    fun observeRecent(limit: Int): Flow<List<ExpenseEntity>> = expenseDao.observeRecent(limit)

    fun observeByCaptureId(captureId: String): Flow<List<ExpenseEntity>> =
        expenseDao.observeByCaptureId(captureId)

    fun observeDistinctCategories(): Flow<List<String>> = expenseDao.observeDistinctCategories()

    /** Persists parser output for one capture, replacing any prior attempt for the same capture. */
    suspend fun saveParsedExpenses(captureId: String, parsed: List<ParsedExpense>): List<ExpenseEntity> {
        val now = Instant.now().toEpochMilli()
        val entities = parsed.map { p ->
            // The parser reports what was said ("HSBC", "cash"); only here, with the account
            // table in reach, does that become an account to charge. See [AccountResolver].
            val resolved = accountResolver.resolve(p.account, p.paymentMethod)
            ExpenseEntity(
                id = p.id,
                captureId = captureId,
                amountMinorUnits = p.amountMinorUnits,
                currencyCode = p.currencyCode,
                occurredAtEpochMillis = p.occurredAt.toInstant().toEpochMilli(),
                timeZoneId = p.occurredAt.zone.id,
                category = p.category,
                accountId = resolved.accountId,
                paymentMethod = resolved.paymentMethod,
                description = p.description,
                confidence = p.confidence,
                reviewStatus = if (p.needsReview) ReviewStatus.NEEDS_REVIEW else ReviewStatus.APPROVED,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
        }
        expenseDao.replaceForCapture(captureId, entities)
        return entities
    }

    /** Inserts a hand-typed expense under [captureId], pre-approved since a human just entered it. */
    suspend fun createManual(
        captureId: String,
        amountMinorUnits: Long,
        currencyCode: String,
        occurredAtEpochMillis: Long,
        timeZoneId: String,
        category: String,
        accountId: String?,
        paymentMethod: PaymentMethod?,
        description: String?,
        notes: String? = null,
        recurringExpenseId: String? = null
    ): ExpenseEntity {
        val now = Instant.now().toEpochMilli()
        val entity = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            captureId = captureId,
            amountMinorUnits = amountMinorUnits,
            currencyCode = currencyCode,
            occurredAtEpochMillis = occurredAtEpochMillis,
            timeZoneId = timeZoneId,
            category = category,
            accountId = accountId,
            paymentMethod = paymentMethod,
            description = description,
            notes = notes,
            recurringExpenseId = recurringExpenseId,
            confidence = 1.0f,
            reviewStatus = ReviewStatus.APPROVED,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        expenseDao.insertAll(listOf(entity))
        return entity
    }

    suspend fun approve(expenseId: String) = updateReviewStatus(expenseId, ReviewStatus.APPROVED)

    suspend fun reject(expenseId: String) = updateReviewStatus(expenseId, ReviewStatus.REJECTED)

    suspend fun update(expense: ExpenseEntity) {
        expenseDao.update(expense.copy(updatedAtEpochMillis = Instant.now().toEpochMilli()))
    }

    /**
     * Deletes an expense, plus the placeholder capture behind it when that capture exists purely
     * to host it (manual entry, quick-add widget, generated recurrence) and has no expenses left.
     * Without this, every deleted manual expense leaves a "Manually entered" ghost row in the
     * Inbox. Real voice captures are always kept - their transcript and audio are the user's
     * data, not a side effect of the expense.
     */
    suspend fun delete(expenseId: String) = database.withTransaction {
        val expense = expenseDao.getById(expenseId) ?: return@withTransaction
        expenseDao.deleteById(expenseId)
        val capture = captureDao.getById(expense.captureId)
        if (capture != null && capture.isManual && expenseDao.getByCaptureId(capture.id).isEmpty()) {
            captureDao.deleteById(capture.id)
        }
    }

    /** Removes every expense produced by [captureId], e.g. in response to a notification Undo action. */
    suspend fun undoForCapture(captureId: String) = expenseDao.deleteByCaptureId(captureId)

    private suspend fun updateReviewStatus(expenseId: String, status: ReviewStatus) {
        val current = expenseDao.getById(expenseId) ?: return
        expenseDao.update(
            current.copy(reviewStatus = status, updatedAtEpochMillis = Instant.now().toEpochMilli())
        )
    }
}
