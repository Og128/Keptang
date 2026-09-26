package com.keptang.recurring

import androidx.room.withTransaction
import com.keptang.account.AccountResolver
import com.keptang.data.db.KeptangDatabase
import com.keptang.data.db.RecurringExpenseEntity
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.RecurringExpenseRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Runs once per app launch (see [com.keptang.ui.MainActivity]): turns every recurring expense
 * whose next due date has arrived into a real, pre-approved [com.keptang.data.db.ExpenseEntity] -
 * exactly like a manually-typed one, since there's no audio or transcript behind it either. If
 * several due dates were missed while the app was closed, each one gets its own expense, capped
 * at [MAX_CATCH_UP_OCCURRENCES] per recurrence so a very old or misconfigured entry can't spam
 * the ledger.
 */
class RecurringExpenseGenerator(
    private val database: KeptangDatabase,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val captureRepository: CaptureRepository,
    private val expenseRepository: ExpenseRepository,
    private val accountResolver: AccountResolver
) {

    suspend fun generateDueExpenses(timeZoneId: String, currencyCode: String) {
        val zone = ZoneId.of(timeZoneId)
        val now = Instant.now().toEpochMilli()
        val due = recurringExpenseRepository.getDue(now)

        for (recurring in due) {
            var dueDate = Instant.ofEpochMilli(recurring.nextDueAtEpochMillis).atZone(zone).toLocalDate()
            var occurrences = 0
            var latest: RecurringExpenseEntity = recurring

            while (!dueDate.atStartOfDay(zone).toInstant().toEpochMilli().let { it > now } && occurrences < MAX_CATCH_UP_OCCURRENCES) {
                // Both writes happen in one transaction so a process death between them can never
                // regenerate the same occurrence twice on the next launch.
                database.withTransaction {
                    generateExpense(latest, dueDate, timeZoneId, currencyCode)
                    recurringExpenseRepository.advancePastDueDate(latest, dueDate, timeZoneId)
                }
                latest = recurringExpenseRepository.getById(latest.id) ?: break
                dueDate = Instant.ofEpochMilli(latest.nextDueAtEpochMillis).atZone(zone).toLocalDate()
                occurrences++
            }
        }
    }

    private suspend fun generateExpense(recurring: RecurringExpenseEntity, dueDate: LocalDate, timeZoneId: String, currencyCode: String) {
        val captureId = captureRepository.createManualEntry(timeZoneId)
        expenseRepository.createManual(
            captureId = captureId,
            amountMinorUnits = recurring.amountMinorUnits,
            currencyCode = currencyCode,
            occurredAtEpochMillis = dueDate.atStartOfDay(ZoneId.of(timeZoneId)).toInstant().toEpochMilli(),
            timeZoneId = timeZoneId,
            category = recurring.category,
            // A subscription is charged somewhere; the default account is the only honest guess,
            // and the payment method stays unknown rather than being invented.
            accountId = accountResolver.defaultAccount()?.id,
            paymentMethod = null,
            description = recurring.name,
            recurringExpenseId = recurring.id
        )
    }

    companion object {
        private const val MAX_CATCH_UP_OCCURRENCES = 24
    }
}
