package com.keptang.budget

import com.keptang.data.db.BudgetEntity
import com.keptang.data.db.BudgetPeriodType
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.db.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class BudgetCalculatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Bangkok")
    private val today = LocalDate.of(2026, 9, 2)
    private val defaultCurrency = "THB"

    private fun expense(
        amountMinorUnits: Long,
        category: String,
        date: LocalDate,
        currencyCode: String = defaultCurrency,
        account: String? = null
    ) = ExpenseEntity(
        id = UUID.randomUUID().toString(),
        captureId = "capture",
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        occurredAtEpochMillis = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
        timeZoneId = zone.id,
        category = category,
        account = account,
        paymentMethod = null,
        merchant = null,
        confidence = 1f,
        reviewStatus = ReviewStatus.APPROVED,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L
    )

    private fun budget(
        category: String?,
        amountMinorUnits: Long = 100_00L,
        periodType: BudgetPeriodType = BudgetPeriodType.MONTHLY,
        periodAnchor: Int = 1
    ) = BudgetEntity(
        id = UUID.randomUUID().toString(),
        category = category,
        amountMinorUnits = amountMinorUnits,
        periodType = periodType,
        periodAnchor = periodAnchor,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L
    )

    @Test
    fun `overall and category budgets compute independently over their own periods`() {
        val overall = budget(category = null, periodType = BudgetPeriodType.MONTHLY, periodAnchor = 1) // Sept 1 - Oct 1
        val coffee = budget(category = "Coffee", periodType = BudgetPeriodType.WEEKLY, periodAnchor = 1) // Aug 31 - Sept 7

        val expenses = listOf(
            expense(500, "Coffee", LocalDate.of(2026, 9, 5)), // inside both periods
            expense(300, "Coffee", LocalDate.of(2026, 9, 10)) // inside Overall's period only
        )

        val snapshot = BudgetCalculator.compute(listOf(overall, coffee), expenses, defaultCurrency, today)

        assertEquals(800L, snapshot.overall!!.spentMinorUnits)
        assertEquals(500L, snapshot.categories.single().spentMinorUnits)
    }

    @Test
    fun `other bucket only includes unbudgeted-category spend within the overall budget's own period`() {
        val overall = budget(category = null, periodAnchor = 1) // Sept 1 - Oct 1
        val coffee = budget(category = "Coffee", periodAnchor = 1)

        val expenses = listOf(
            expense(500, "Coffee", LocalDate.of(2026, 9, 5)), // budgeted category -> not Other
            expense(700, "Dining", LocalDate.of(2026, 9, 6)), // unbudgeted category, in period -> Other
            expense(200, "Dining", LocalDate.of(2026, 8, 20)) // unbudgeted category, outside Overall's period -> excluded
        )

        val snapshot = BudgetCalculator.compute(listOf(overall, coffee), expenses, defaultCurrency, today)

        assertEquals(700L, snapshot.other!!.spentMinorUnits)
    }

    @Test
    fun `a category budget on the literal Uncategorized category removes those expenses from Other`() {
        val overall = budget(category = null, periodAnchor = 1)
        val uncategorized = budget(category = "Uncategorized", periodAnchor = 1)

        val expenses = listOf(
            expense(400, "Uncategorized", LocalDate.of(2026, 9, 5)),
            expense(300, "Dining", LocalDate.of(2026, 9, 5))
        )

        val snapshot = BudgetCalculator.compute(listOf(overall, uncategorized), expenses, defaultCurrency, today)

        assertEquals(300L, snapshot.other!!.spentMinorUnits)
        assertEquals(400L, snapshot.categories.single { it.budget.category == "Uncategorized" }.spentMinorUnits)
    }

    @Test
    fun `expenses in a non-default currency are excluded from the total but grouped by currency`() {
        val overall = budget(category = null, periodAnchor = 1)
        val expenses = listOf(
            expense(500, "Coffee", LocalDate.of(2026, 9, 5), currencyCode = "THB"),
            expense(100, "Coffee", LocalDate.of(2026, 9, 5), currencyCode = "USD"),
            expense(100, "Coffee", LocalDate.of(2026, 9, 6), currencyCode = "USD"),
            expense(50, "Coffee", LocalDate.of(2026, 9, 6), currencyCode = "EUR")
        )

        val snapshot = BudgetCalculator.compute(listOf(overall), expenses, defaultCurrency, today)

        assertEquals(500L, snapshot.overall!!.spentMinorUnits)
        assertEquals(mapOf("USD" to 2, "EUR" to 1), snapshot.overall!!.excludedByCurrency)
    }

    @Test
    fun `no overall budget means Other is null even with unbudgeted-category spend`() {
        val coffee = budget(category = "Coffee", periodAnchor = 1)
        val expenses = listOf(expense(300, "Dining", LocalDate.of(2026, 9, 5)))

        val snapshot = BudgetCalculator.compute(listOf(coffee), expenses, defaultCurrency, today)

        assertNull(snapshot.overall)
        assertNull(snapshot.other)
    }

    @Test
    fun `account differences never affect totals since budgets pool across accounts`() {
        val overall = budget(category = null, periodAnchor = 1)
        val expenses = listOf(
            expense(500, "Coffee", LocalDate.of(2026, 9, 5), account = "Cash"),
            expense(300, "Coffee", LocalDate.of(2026, 9, 5), account = "Kasikorn")
        )

        val snapshot = BudgetCalculator.compute(listOf(overall), expenses, defaultCurrency, today)

        assertEquals(800L, snapshot.overall!!.spentMinorUnits)
        assertTrue(snapshot.categories.isEmpty())
    }
}
