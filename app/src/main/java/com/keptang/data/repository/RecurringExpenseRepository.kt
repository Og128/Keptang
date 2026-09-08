package com.keptang.data.repository

import com.keptang.data.db.BudgetPeriodType
import com.keptang.data.db.RecurringExpenseDao
import com.keptang.data.db.RecurringExpenseEntity
import com.keptang.recurring.RecurringPeriodCalculator
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class RecurringExpenseRepository(private val recurringExpenseDao: RecurringExpenseDao) {

    fun observeAll(): Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.observeAll()

    suspend fun getById(id: String): RecurringExpenseEntity? = recurringExpenseDao.getById(id)

    suspend fun create(
        name: String,
        amountMinorUnits: Long,
        category: String,
        periodType: BudgetPeriodType,
        periodAnchor: Int,
        timeZoneId: String
    ): RecurringExpenseEntity {
        val today = LocalDate.now(ZoneId.of(timeZoneId))
        val now = Instant.now().toEpochMilli()
        val entity = RecurringExpenseEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            amountMinorUnits = amountMinorUnits,
            category = category,
            periodType = periodType,
            periodAnchor = periodAnchor,
            nextDueAtEpochMillis = RecurringPeriodCalculator.firstDueOnOrAfter(periodType, periodAnchor, today)
                .atStartOfDay(ZoneId.of(timeZoneId)).toInstant().toEpochMilli(),
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        recurringExpenseDao.insert(entity)
        return entity
    }

    /** Re-derives the next due date from today, same as [create] - edits are rare enough that recomputing is simpler than reconciling the old schedule. */
    suspend fun update(
        id: String,
        name: String,
        amountMinorUnits: Long,
        category: String,
        periodType: BudgetPeriodType,
        periodAnchor: Int,
        timeZoneId: String
    ) {
        val current = recurringExpenseDao.getById(id) ?: return
        val today = LocalDate.now(ZoneId.of(timeZoneId))
        recurringExpenseDao.update(
            current.copy(
                name = name,
                amountMinorUnits = amountMinorUnits,
                category = category,
                periodType = periodType,
                periodAnchor = periodAnchor,
                nextDueAtEpochMillis = RecurringPeriodCalculator.firstDueOnOrAfter(periodType, periodAnchor, today)
                    .atStartOfDay(ZoneId.of(timeZoneId)).toInstant().toEpochMilli(),
                updatedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun delete(id: String) = recurringExpenseDao.deleteById(id)

    suspend fun getDue(nowEpochMillis: Long): List<RecurringExpenseEntity> = recurringExpenseDao.getDue(nowEpochMillis)

    suspend fun advancePastDueDate(recurring: RecurringExpenseEntity, generatedForDate: LocalDate, timeZoneId: String) {
        val next = RecurringPeriodCalculator.nextAfter(recurring.periodType, recurring.periodAnchor, generatedForDate)
        recurringExpenseDao.update(
            recurring.copy(
                nextDueAtEpochMillis = next.atStartOfDay(ZoneId.of(timeZoneId)).toInstant().toEpochMilli(),
                updatedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }
}
