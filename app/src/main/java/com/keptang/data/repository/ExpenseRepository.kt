package com.keptang.data.repository

import com.keptang.data.db.ExpenseDao
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.db.ReviewStatus
import com.keptang.parser.ParsedExpense
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun observeAll(): Flow<List<ExpenseEntity>> = expenseDao.observeAll()

    fun observeNeedsReview(): Flow<List<ExpenseEntity>> =
        expenseDao.observeByReviewStatus(ReviewStatus.NEEDS_REVIEW)

    fun observeApproved(): Flow<List<ExpenseEntity>> =
        expenseDao.observeByReviewStatus(ReviewStatus.APPROVED)

    suspend fun getByCaptureId(captureId: String): List<ExpenseEntity> =
        expenseDao.getByCaptureId(captureId)

    fun observeByCaptureId(captureId: String): Flow<List<ExpenseEntity>> =
        expenseDao.observeByCaptureId(captureId)

    /** Persists parser output for one capture, replacing any prior attempt for the same capture. */
    suspend fun saveParsedExpenses(captureId: String, parsed: List<ParsedExpense>): List<ExpenseEntity> {
        val now = Instant.now().toEpochMilli()
        val entities = parsed.map { p ->
            ExpenseEntity(
                id = p.id,
                captureId = captureId,
                amountMinorUnits = p.amountMinorUnits,
                currencyCode = p.currencyCode,
                occurredAtEpochMillis = p.occurredAt.toInstant().toEpochMilli(),
                timeZoneId = p.occurredAt.zone.id,
                category = p.category,
                account = p.account,
                paymentMethod = p.paymentMethod,
                merchant = p.merchant,
                confidence = p.confidence,
                reviewStatus = if (p.needsReview) ReviewStatus.NEEDS_REVIEW else ReviewStatus.APPROVED,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
        }
        expenseDao.replaceForCapture(captureId, entities)
        return entities
    }

    suspend fun approve(expenseId: String) = updateReviewStatus(expenseId, ReviewStatus.APPROVED)

    suspend fun reject(expenseId: String) = updateReviewStatus(expenseId, ReviewStatus.REJECTED)

    suspend fun update(expense: ExpenseEntity) {
        expenseDao.update(expense.copy(updatedAtEpochMillis = Instant.now().toEpochMilli()))
    }

    suspend fun delete(expenseId: String) = expenseDao.deleteById(expenseId)

    /** Removes every expense produced by [captureId], e.g. in response to a notification Undo action. */
    suspend fun undoForCapture(captureId: String) = expenseDao.deleteByCaptureId(captureId)

    private suspend fun updateReviewStatus(expenseId: String, status: ReviewStatus) {
        val current = expenseDao.getById(expenseId) ?: return
        expenseDao.update(
            current.copy(reviewStatus = status, updatedAtEpochMillis = Instant.now().toEpochMilli())
        )
    }
}
