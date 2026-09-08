package com.keptang.recurring

import com.keptang.data.db.BudgetPeriodType
import java.time.LocalDate
import java.time.YearMonth

/**
 * Computes due dates for a [com.keptang.data.db.RecurringExpenseEntity]. Shares the same
 * anchor semantics as [com.keptang.budget.BudgetPeriodCalculator] (monthly anchor clamps to the
 * shorter month; weekly anchor is an ISO day-of-week), but answers a different question: not
 * "what period are we in" but "what's the next single due date on or after this one".
 */
object RecurringPeriodCalculator {

    /** The first due date on or after [from] - used when a recurrence is created or edited. */
    fun firstDueOnOrAfter(periodType: BudgetPeriodType, periodAnchor: Int, from: LocalDate): LocalDate =
        when (periodType) {
            BudgetPeriodType.MONTHLY -> {
                val candidate = anchorDateInMonth(YearMonth.from(from), periodAnchor)
                if (!candidate.isBefore(from)) candidate else anchorDateInMonth(YearMonth.from(from).plusMonths(1), periodAnchor)
            }
            BudgetPeriodType.WEEKLY -> {
                val daysUntilAnchor = (periodAnchor - from.dayOfWeek.value + 7) % 7
                from.plusDays(daysUntilAnchor.toLong())
            }
        }

    /** The due date strictly after [current] - used to advance a recurrence past a date it just generated an expense for. */
    fun nextAfter(periodType: BudgetPeriodType, periodAnchor: Int, current: LocalDate): LocalDate =
        when (periodType) {
            BudgetPeriodType.MONTHLY -> anchorDateInMonth(YearMonth.from(current).plusMonths(1), periodAnchor)
            BudgetPeriodType.WEEKLY -> current.plusDays(7)
        }

    private fun anchorDateInMonth(month: YearMonth, anchorDay: Int): LocalDate =
        month.atDay(minOf(anchorDay, month.lengthOfMonth()))
}
