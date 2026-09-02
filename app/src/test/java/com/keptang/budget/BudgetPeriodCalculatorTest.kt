package com.keptang.budget

import com.keptang.data.db.BudgetPeriodType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Hand-traced regression cases for [BudgetPeriodCalculator] - the highest-risk logic in the
 * budget feature, same category of risk as the French base-20 number words in NumberWordsTest.
 */
class BudgetPeriodCalculatorTest {

    @Test
    fun `monthly simple case`() {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(
            BudgetPeriodType.MONTHLY, 1, LocalDate.of(2026, 9, 2)
        )
        assertEquals(LocalDate.of(2026, 9, 1), start)
        assertEquals(LocalDate.of(2026, 10, 1), end)
    }

    @Test
    fun `monthly anchor still ahead this cycle falls back to previous month`() {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(
            BudgetPeriodType.MONTHLY, 20, LocalDate.of(2026, 9, 2)
        )
        assertEquals(LocalDate.of(2026, 8, 20), start)
        assertEquals(LocalDate.of(2026, 9, 20), end)
    }

    @Test
    fun `monthly anchor 31 clamps in a non-leap February`() {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(
            BudgetPeriodType.MONTHLY, 31, LocalDate.of(2026, 2, 15)
        )
        assertEquals(LocalDate.of(2026, 1, 31), start)
        assertEquals(LocalDate.of(2026, 2, 28), end)
    }

    @Test
    fun `monthly anchor equals today post-clamp then floats to the real anchor day`() {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(
            BudgetPeriodType.MONTHLY, 31, LocalDate.of(2026, 2, 28)
        )
        assertEquals(LocalDate.of(2026, 2, 28), start)
        assertEquals(LocalDate.of(2026, 3, 31), end)
    }

    @Test
    fun `monthly anchor 29 needs no clamp in a leap year`() {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(
            BudgetPeriodType.MONTHLY, 29, LocalDate.of(2028, 2, 29)
        )
        assertEquals(LocalDate.of(2028, 2, 29), start)
        assertEquals(LocalDate.of(2028, 3, 29), end)
    }

    @Test
    fun `monthly anchor 29 clamps in a non-leap year`() {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(
            BudgetPeriodType.MONTHLY, 29, LocalDate.of(2026, 2, 28)
        )
        assertEquals(LocalDate.of(2026, 2, 28), start)
        assertEquals(LocalDate.of(2026, 3, 29), end)
    }

    @Test
    fun `monthly mid-month anchor ahead this cycle with no clamping involved`() {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(
            BudgetPeriodType.MONTHLY, 15, LocalDate.of(2026, 9, 2)
        )
        assertEquals(LocalDate.of(2026, 8, 15), start)
        assertEquals(LocalDate.of(2026, 9, 15), end)
    }

    @Test
    fun `weekly basic case`() {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(
            BudgetPeriodType.WEEKLY, 1, LocalDate.of(2026, 9, 2)
        )
        assertEquals(LocalDate.of(2026, 8, 31), start)
        assertEquals(LocalDate.of(2026, 9, 7), end)
    }

    @Test
    fun `weekly anchor equals today`() {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(
            BudgetPeriodType.WEEKLY, 3, LocalDate.of(2026, 9, 2)
        )
        assertEquals(LocalDate.of(2026, 9, 2), start)
        assertEquals(LocalDate.of(2026, 9, 9), end)
    }

    @Test
    fun `weekly wraparound crosses a month and year boundary`() {
        val (start, end) = BudgetPeriodCalculator.currentPeriod(
            BudgetPeriodType.WEEKLY, 7, LocalDate.of(2027, 1, 1)
        )
        assertEquals(LocalDate.of(2026, 12, 27), start)
        assertEquals(LocalDate.of(2027, 1, 3), end)
    }
}
