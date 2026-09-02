package com.keptang.data.db

/**
 * The recurring cadence a [BudgetEntity] resets on. [BudgetEntity.periodAnchor] means different
 * things depending on this: for [MONTHLY] it's a day-of-month (1-31, clamped to shorter months);
 * for [WEEKLY] it's an ISO day-of-week (1=Monday..7=Sunday).
 */
enum class BudgetPeriodType {
    MONTHLY,
    WEEKLY
}
