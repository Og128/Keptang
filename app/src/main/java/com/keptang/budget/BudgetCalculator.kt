package com.keptang.budget

import com.keptang.data.db.BudgetEntity
import com.keptang.data.db.ExpenseEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class BudgetStanding(
    val budget: BudgetEntity,
    val periodStart: LocalDate,
    val periodEndExclusive: LocalDate,
    val spentMinorUnits: Long,
    val excludedByCurrency: Map<String, Int>
)

data class OtherStanding(
    val periodStart: LocalDate,
    val periodEndExclusive: LocalDate,
    val spentMinorUnits: Long,
    val excludedByCurrency: Map<String, Int>
)

data class BudgetSnapshot(
    val overall: BudgetStanding?,
    val other: OtherStanding?,
    val categories: List<BudgetStanding>,
    val defaultCurrencyCode: String
)

/**
 * Sums approved Expenses against Budgets, in memory rather than via SQL - the app has no
 * aggregate queries anywhere and this is personal-scale data, so this stays a plain, independently
 * testable function rather than new DAO/ViewModel complexity.
 */
object BudgetCalculator {

    fun compute(
        budgets: List<BudgetEntity>,
        approvedExpenses: List<ExpenseEntity>,
        defaultCurrencyCode: String,
        today: LocalDate
    ): BudgetSnapshot {
        val overallBudget = budgets.firstOrNull { it.category == null }
        val categoryBudgets = budgets.filter { it.category != null }
        val budgetedCategories = categoryBudgets.mapNotNull { it.category }.toSet()

        val overallStanding = overallBudget?.let {
            standingFor(it, approvedExpenses, defaultCurrencyCode, today) { true }
        }

        // "Other" always uses the Overall Budget's own Current Period, and only exists when an
        // Overall Budget is defined - see CONTEXT.md's "Other" entry.
        val other = overallBudget?.let {
            val (start, end) = BudgetPeriodCalculator.currentPeriod(it.periodType, it.periodAnchor, today)
            val scoped = approvedExpenses.filter { e -> inPeriod(e, start, end) && e.category !in budgetedCategories }
            val (counted, excluded) = scoped.partition { e -> e.currencyCode == defaultCurrencyCode }
            OtherStanding(
                periodStart = start,
                periodEndExclusive = end,
                spentMinorUnits = counted.sumOf { it.amountMinorUnits },
                excludedByCurrency = excluded.groupingBy { it.currencyCode }.eachCount()
            )
        }

        val categoryStandings = categoryBudgets.map { budget ->
            standingFor(budget, approvedExpenses, defaultCurrencyCode, today) { e -> e.category == budget.category }
        }

        return BudgetSnapshot(overallStanding, other, categoryStandings, defaultCurrencyCode)
    }

    private fun standingFor(
        budget: BudgetEntity,
        approvedExpenses: List<ExpenseEntity>,
        defaultCurrencyCode: String,
        today: LocalDate,
        categoryMatches: (ExpenseEntity) -> Boolean
    ): BudgetStanding {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(budget.periodType, budget.periodAnchor, today)
        val scoped = approvedExpenses.filter { e -> inPeriod(e, start, end) && categoryMatches(e) }
        val (counted, excluded) = scoped.partition { e -> e.currencyCode == defaultCurrencyCode }
        return BudgetStanding(
            budget = budget,
            periodStart = start,
            periodEndExclusive = end,
            spentMinorUnits = counted.sumOf { it.amountMinorUnits },
            excludedByCurrency = excluded.groupingBy { it.currencyCode }.eachCount()
        )
    }

    private fun inPeriod(expense: ExpenseEntity, start: LocalDate, endExclusive: LocalDate): Boolean {
        val date = Instant.ofEpochMilli(expense.occurredAtEpochMillis)
            .atZone(ZoneId.of(expense.timeZoneId))
            .toLocalDate()
        return !date.isBefore(start) && date.isBefore(endExclusive)
    }
}
