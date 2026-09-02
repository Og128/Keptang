package com.keptang.budget

import com.keptang.data.db.BudgetPeriodType
import java.time.LocalDate
import java.time.YearMonth

/**
 * Computes a Budget's Current Period: the concrete, half-open `[start, endExclusive)` date range
 * derived from its [BudgetPeriodType] and anchor. See CONTEXT.md's "Current Period" entry and
 * ADR-0004 (period is chosen per-Budget).
 */
object BudgetPeriodCalculator {

    fun currentPeriod(periodType: BudgetPeriodType, periodAnchor: Int, today: LocalDate): Pair<LocalDate, LocalDate> =
        when (periodType) {
            BudgetPeriodType.MONTHLY -> monthlyPeriod(periodAnchor, today)
            BudgetPeriodType.WEEKLY -> weeklyPeriod(periodAnchor, today)
        }

    /**
     * The anchor day clamps to the shorter month's last day (e.g. anchor 31 in a 30-day month
     * becomes that month's 30th) and "floats" back to the real day the next time the month is
     * long enough - each month's clamp is computed independently, so this also handles leap
     * years correctly with no separate branch.
     */
    private fun anchorDateInMonth(month: YearMonth, anchorDay: Int): LocalDate =
        month.atDay(minOf(anchorDay, month.lengthOfMonth()))

    private fun monthlyPeriod(anchorDay: Int, today: LocalDate): Pair<LocalDate, LocalDate> {
        val thisMonth = YearMonth.from(today)
        val thisAnchor = anchorDateInMonth(thisMonth, anchorDay)
        return if (!today.isBefore(thisAnchor)) {
            thisAnchor to anchorDateInMonth(thisMonth.plusMonths(1), anchorDay)
        } else {
            anchorDateInMonth(thisMonth.minusMonths(1), anchorDay) to thisAnchor
        }
    }

    private fun weeklyPeriod(anchorIsoDayOfWeek: Int, today: LocalDate): Pair<LocalDate, LocalDate> {
        val daysSinceAnchor = (today.dayOfWeek.value - anchorIsoDayOfWeek + 7) % 7
        val start = today.minusDays(daysSinceAnchor.toLong())
        return start to start.plusDays(7)
    }
}
