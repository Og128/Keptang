package com.keptang.dashboard

import com.keptang.data.db.ExpenseEntity
import com.keptang.data.db.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class DashboardCalculatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Bangkok")
    private val today = LocalDate.of(2026, 9, 15)
    private val defaultCurrency = "THB"

    private fun expense(
        amountMinorUnits: Long,
        category: String,
        date: LocalDate,
        currencyCode: String = defaultCurrency
    ) = ExpenseEntity(
        id = UUID.randomUUID().toString(),
        captureId = "capture",
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        occurredAtEpochMillis = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
        timeZoneId = zone.id,
        category = category,
        account = null,
        paymentMethod = null,
        merchant = null,
        confidence = 1f,
        reviewStatus = ReviewStatus.APPROVED,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L
    )

    @Test
    fun `today filter only includes expenses from today`() {
        val expenses = listOf(
            expense(500, "Coffee", today),
            expense(300, "Coffee", today.minusDays(1))
        )

        val snapshot = DashboardCalculator.compute(expenses, defaultCurrency, DashboardFilter.Today, today)

        assertEquals(500L, snapshot.totalMinorUnits)
    }

    @Test
    fun `last 7 days includes today and the six days before it`() {
        val expenses = listOf(
            expense(100, "Coffee", today),
            expense(200, "Coffee", today.minusDays(6)),
            expense(400, "Coffee", today.minusDays(7))
        )

        val snapshot = DashboardCalculator.compute(expenses, defaultCurrency, DashboardFilter.Last7Days, today)

        assertEquals(300L, snapshot.totalMinorUnits)
    }

    @Test
    fun `last 30 days includes today and the twenty-nine days before it`() {
        val expenses = listOf(
            expense(100, "Coffee", today.minusDays(29)),
            expense(400, "Coffee", today.minusDays(30))
        )

        val snapshot = DashboardCalculator.compute(expenses, defaultCurrency, DashboardFilter.Last30Days, today)

        assertEquals(100L, snapshot.totalMinorUnits)
    }

    @Test
    fun `custom period is inclusive of both endpoints`() {
        val start = LocalDate.of(2026, 9, 1)
        val end = LocalDate.of(2026, 9, 5)
        val expenses = listOf(
            expense(100, "Coffee", start),
            expense(200, "Coffee", end),
            expense(400, "Coffee", end.plusDays(1))
        )

        val snapshot = DashboardCalculator.compute(expenses, defaultCurrency, DashboardFilter.Period(start, end), today)

        assertEquals(300L, snapshot.totalMinorUnits)
    }

    @Test
    fun `spend is grouped by category and sorted descending`() {
        val expenses = listOf(
            expense(100, "Coffee", today),
            expense(500, "Dining", today),
            expense(300, "Transport", today)
        )

        val snapshot = DashboardCalculator.compute(expenses, defaultCurrency, DashboardFilter.Today, today)

        assertEquals(
            listOf("Dining" to 500L, "Transport" to 300L, "Coffee" to 100L),
            snapshot.byCategory.map { it.category to it.spentMinorUnits }
        )
    }

    @Test
    fun `non-default currency expenses are excluded from the total but grouped by currency`() {
        val expenses = listOf(
            expense(500, "Coffee", today, currencyCode = "THB"),
            expense(100, "Coffee", today, currencyCode = "USD"),
            expense(100, "Coffee", today, currencyCode = "USD")
        )

        val snapshot = DashboardCalculator.compute(expenses, defaultCurrency, DashboardFilter.Today, today)

        assertEquals(500L, snapshot.totalMinorUnits)
        assertEquals(mapOf("USD" to 2), snapshot.excludedByCurrency)
    }

    @Test
    fun `empty range yields zero total and no categories`() {
        val snapshot = DashboardCalculator.compute(emptyList(), defaultCurrency, DashboardFilter.Today, today)

        assertEquals(0L, snapshot.totalMinorUnits)
        assertTrue(snapshot.byCategory.isEmpty())
    }

    @Test
    fun `transaction count reflects only default-currency expenses in range`() {
        val expenses = listOf(
            expense(100, "Coffee", today, currencyCode = "THB"),
            expense(200, "Dining", today, currencyCode = "THB"),
            expense(300, "Coffee", today, currencyCode = "USD"),
            expense(400, "Coffee", today.minusDays(1), currencyCode = "THB")
        )

        val snapshot = DashboardCalculator.compute(expenses, defaultCurrency, DashboardFilter.Today, today)

        assertEquals(2, snapshot.transactionCount)
    }
}
