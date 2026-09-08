package com.keptang.dashboard

import com.keptang.data.db.ExpenseEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CategorySpend(val category: String, val spentMinorUnits: Long)

data class DashboardSnapshot(
    val totalMinorUnits: Long,
    val transactionCount: Int,
    val byCategory: List<CategorySpend>,
    val excludedByCurrency: Map<String, Int>,
    val defaultCurrencyCode: String
)

sealed class DashboardFilter {
    data object Today : DashboardFilter()
    data object Last7Days : DashboardFilter()
    data object Last30Days : DashboardFilter()
    data class Period(val start: LocalDate, val endInclusive: LocalDate) : DashboardFilter()

    /** [start, endExclusive) for the filter, relative to [today]. */
    fun range(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
        is Today -> today to today.plusDays(1)
        is Last7Days -> today.minusDays(6) to today.plusDays(1)
        is Last30Days -> today.minusDays(29) to today.plusDays(1)
        is Period -> minOf(start, endInclusive) to maxOf(start, endInclusive).plusDays(1)
    }
}

/**
 * Sums approved Expenses by category over a date range, in memory - same approach as
 * [com.keptang.budget.BudgetCalculator] for the same reason (no aggregate queries anywhere,
 * personal-scale data, stays a plain independently-testable function).
 */
object DashboardCalculator {

    fun compute(
        approvedExpenses: List<ExpenseEntity>,
        defaultCurrencyCode: String,
        filter: DashboardFilter,
        today: LocalDate
    ): DashboardSnapshot {
        val (start, endExclusive) = filter.range(today)
        val scoped = approvedExpenses.filter { inRange(it, start, endExclusive) }
        val (counted, excluded) = scoped.partition { it.currencyCode == defaultCurrencyCode }
        val byCategory = counted.groupBy { it.category }
            .map { (category, expenses) -> CategorySpend(category, expenses.sumOf { it.amountMinorUnits }) }
            .sortedByDescending { it.spentMinorUnits }

        return DashboardSnapshot(
            totalMinorUnits = counted.sumOf { it.amountMinorUnits },
            transactionCount = counted.size,
            byCategory = byCategory,
            excludedByCurrency = excluded.groupingBy { it.currencyCode }.eachCount(),
            defaultCurrencyCode = defaultCurrencyCode
        )
    }

    private fun inRange(expense: ExpenseEntity, start: LocalDate, endExclusive: LocalDate): Boolean {
        val date = Instant.ofEpochMilli(expense.occurredAtEpochMillis)
            .atZone(ZoneId.of(expense.timeZoneId))
            .toLocalDate()
        return !date.isBefore(start) && date.isBefore(endExclusive)
    }
}
