package com.keptang.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ExpenseParserTest {

    private lateinit var parser: ExpenseParser
    private lateinit var referenceDateTime: ZonedDateTime
    private lateinit var today: LocalDate
    private lateinit var yesterday: LocalDate
    private val zone: ZoneId = ZoneId.of("Asia/Bangkok")

    @Before
    fun setUp() {
        parser = ExpenseParser()
        referenceDateTime = ZonedDateTime.of(2026, 6, 15, 19, 30, 0, 0, zone)
        today = referenceDateTime.toLocalDate()
        yesterday = today.minusDays(1)
    }

    private fun parse(transcript: String) =
        parser.parse(transcript, captureId = "capture-1", referenceDateTime = referenceDateTime)

    // 1. "I paid 150 baht for a taxi."
    @Test
    fun `single expense with explicit currency word`() {
        val expenses = parse("I paid 150 baht for a taxi.")

        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(15000L, expense.amountMinorUnits)
        assertEquals("THB", expense.currencyCode)
        assertEquals("Transport", expense.category)
        assertEquals(today, expense.occurredAt.toLocalDate())
        assertTrue(!expense.needsReview)
    }

    // 2. "Yesterday I spent 550 baht on dinner."
    @Test
    fun `single expense with relative yesterday date`() {
        val expenses = parse("Yesterday I spent 550 baht on dinner.")

        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(55000L, expense.amountMinorUnits)
        assertEquals("THB", expense.currencyCode)
        assertEquals("Dining", expense.category)
        assertEquals(yesterday, expense.occurredAt.toLocalDate())
    }

    // 3. "Dinner was 550 and coffee was 25."
    @Test
    fun `two expenses joined with and default to todays date`() {
        val expenses = parse("Dinner was 550 and coffee was 25.")

        assertEquals(2, expenses.size)

        assertEquals(55000L, expenses[0].amountMinorUnits)
        assertEquals("THB", expenses[0].currencyCode)
        assertEquals("Dining", expenses[0].category)
        assertEquals(today, expenses[0].occurredAt.toLocalDate())

        assertEquals(2500L, expenses[1].amountMinorUnits)
        assertEquals("Coffee", expenses[1].category)
        assertEquals(today, expenses[1].occurredAt.toLocalDate())
    }

    // 4. "Today I paid 150 for a taxi and yesterday 300 for groceries."
    @Test
    fun `two expenses with different explicit relative dates`() {
        val expenses = parse("Today I paid 150 for a taxi and yesterday 300 for groceries.")

        assertEquals(2, expenses.size)

        assertEquals(15000L, expenses[0].amountMinorUnits)
        assertEquals("Transport", expenses[0].category)
        assertEquals(today, expenses[0].occurredAt.toLocalDate())
        assertEquals("THB", expenses[0].currencyCode)

        assertEquals(30000L, expenses[1].amountMinorUnits)
        assertEquals("Groceries", expenses[1].category)
        assertEquals(yesterday, expenses[1].occurredAt.toLocalDate())
    }

    // 5. "Yesterday night I paid 550 baht for dinner and 25 baht for coffee, and today I paid 150 baht for a taxi."
    @Test
    fun `a date applies to multiple following expenses until a new date is mentioned`() {
        val expenses = parse(
            "Yesterday night I paid 550 baht for dinner and 25 baht for coffee, " +
                "and today I paid 150 baht for a taxi."
        )

        assertEquals(3, expenses.size)

        assertEquals(55000L, expenses[0].amountMinorUnits)
        assertEquals("Dining", expenses[0].category)
        assertEquals(yesterday, expenses[0].occurredAt.toLocalDate())

        assertEquals(2500L, expenses[1].amountMinorUnits)
        assertEquals("Coffee", expenses[1].category)
        // The coffee clause has no date phrase of its own - it inherits "yesterday".
        assertEquals(yesterday, expenses[1].occurredAt.toLocalDate())

        assertEquals(15000L, expenses[2].amountMinorUnits)
        assertEquals("Transport", expenses[2].category)
        assertEquals(today, expenses[2].occurredAt.toLocalDate())
    }

    // 6. "I paid 700 baht for groceries from my Bangkok Bank account."
    @Test
    fun `account expression is extracted`() {
        val expenses = parse("I paid 700 baht for groceries from my Bangkok Bank account.")

        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(70000L, expense.amountMinorUnits)
        assertEquals("Groceries", expense.category)
        assertEquals("Bangkok Bank", expense.account)
    }

    // 7. "I paid 80 cash for coffee."
    @Test
    fun `payment method cash and default currency`() {
        val expenses = parse("I paid 80 cash for coffee.")

        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(8000L, expense.amountMinorUnits)
        assertEquals("THB", expense.currencyCode)
        assertEquals("Coffee", expense.category)
        assertEquals("Cash", expense.paymentMethod)
    }

    // 8. Ambiguous sentence with no amount -> must be sent to review (no invented amount).
    @Test
    fun `ambiguous sentence with no amount produces no expenses`() {
        val expenses = parse("I think I spent some money yesterday but I'm not sure how much.")

        assertTrue(expenses.isEmpty())
    }

    @Test
    fun `spoken number amounts are understood`() {
        val expenses = parse("I paid eighty baht for coffee.")

        assertEquals(1, expenses.size)
        assertEquals(8000L, expenses[0].amountMinorUnits)
        assertEquals("Coffee", expenses[0].category)
    }

    @Test
    fun `explicit month and day date is understood`() {
        val expenses = parse("On August 5 I paid 200 baht for lunch.")

        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(20000L, expense.amountMinorUnits)
        assertEquals("Dining", expense.category)
        assertEquals(LocalDate.of(2026, 8, 5), expense.occurredAt.toLocalDate())
    }

    @Test
    fun `promptpay and bank transfer payment methods are understood`() {
        val promptPay = parse("I paid 120 baht for coffee with PromptPay.")
        assertEquals("PromptPay", promptPay[0].paymentMethod)

        val transfer = parse("I paid 1200 baht for rent by bank transfer.")
        assertEquals("Bank Transfer", transfer[0].paymentMethod)
        assertEquals("Housing", transfer[0].category)
    }

    @Test
    fun `unmatched category falls back to uncategorized without discarding the expense`() {
        val expenses = parse("I paid 45 baht for stickers.")

        assertEquals(1, expenses.size)
        assertEquals("Uncategorized", expenses[0].category)
        assertNull(expenses[0].paymentMethod)
    }
}
