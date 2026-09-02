package com.keptang.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * French-language mirror of [ExpenseParserTest]'s required examples, plus the compound-number
 * cases called out in the language-switch plan as the highest-risk part of French support (see
 * [NumberWordsTest] for the isolated number-word cases this exercises end to end).
 */
class ExpenseParserFrenchTest {

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
        parser.parse(transcript, captureId = "capture-1", referenceDateTime = referenceDateTime, languageCode = "fr")

    @Test
    fun `single expense with explicit currency word`() {
        val expenses = parse("J'ai payé 150 baht pour un taxi.")

        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(15000L, expense.amountMinorUnits)
        assertEquals("THB", expense.currencyCode)
        assertEquals("Transport", expense.category)
        assertEquals(today, expense.occurredAt.toLocalDate())
        assertTrue(!expense.needsReview)
    }

    @Test
    fun `single expense with relative yesterday date`() {
        val expenses = parse("Hier j'ai dépensé 550 baht pour le dîner.")

        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(55000L, expense.amountMinorUnits)
        assertEquals("Dining", expense.category)
        assertEquals(yesterday, expense.occurredAt.toLocalDate())
    }

    @Test
    fun `two expenses joined with et default to todays date`() {
        val expenses = parse("Le dîner était 550 et le café était 25.")

        assertEquals(2, expenses.size)
        assertEquals(55000L, expenses[0].amountMinorUnits)
        assertEquals("Dining", expenses[0].category)
        assertEquals(today, expenses[0].occurredAt.toLocalDate())

        assertEquals(2500L, expenses[1].amountMinorUnits)
        assertEquals("Coffee", expenses[1].category)
    }

    @Test
    fun `two expenses with different explicit relative dates`() {
        val expenses = parse("Aujourd'hui j'ai payé 150 pour un taxi et hier 300 pour les courses.")

        assertEquals(2, expenses.size)
        assertEquals(15000L, expenses[0].amountMinorUnits)
        assertEquals("Transport", expenses[0].category)
        assertEquals(today, expenses[0].occurredAt.toLocalDate())

        assertEquals(30000L, expenses[1].amountMinorUnits)
        assertEquals("Groceries", expenses[1].category)
        assertEquals(yesterday, expenses[1].occurredAt.toLocalDate())
    }

    @Test
    fun `a date applies to multiple following expenses until a new date is mentioned`() {
        val expenses = parse(
            "Hier soir j'ai payé 550 baht pour le dîner et 25 baht pour le café, " +
                "et aujourd'hui j'ai payé 150 baht pour un taxi."
        )

        assertEquals(3, expenses.size)
        assertEquals("Dining", expenses[0].category)
        assertEquals(yesterday, expenses[0].occurredAt.toLocalDate())

        assertEquals("Coffee", expenses[1].category)
        // No date phrase of its own - inherits "hier".
        assertEquals(yesterday, expenses[1].occurredAt.toLocalDate())

        assertEquals("Transport", expenses[2].category)
        assertEquals(today, expenses[2].occurredAt.toLocalDate())
    }

    @Test
    fun `account expression in French word order is extracted`() {
        val expenses = parse("J'ai payé 700 baht pour les courses depuis mon compte Kasikorn.")

        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(70000L, expense.amountMinorUnits)
        assertEquals("Groceries", expense.category)
        assertEquals("Kasikorn", expense.account)
    }

    @Test
    fun `payment method especes and default currency`() {
        val expenses = parse("J'ai payé 80 en espèces pour un café.")

        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(8000L, expense.amountMinorUnits)
        assertEquals("THB", expense.currencyCode)
        assertEquals("Coffee", expense.category)
        assertEquals("Cash", expense.paymentMethod)
    }

    @Test
    fun `ambiguous sentence with no amount produces no expenses`() {
        val expenses = parse("Je pense avoir dépensé de l'argent hier mais je ne sais pas combien.")

        assertTrue(expenses.isEmpty())
    }

    @Test
    fun `spoken French number quatre-vingts is understood as 80`() {
        val expenses = parse("J'ai payé quatre-vingts baht pour le café.")

        assertEquals(1, expenses.size)
        assertEquals(8000L, expenses[0].amountMinorUnits)
        assertEquals("Coffee", expenses[0].category)
    }

    @Test
    fun `explicit day-then-month French date is understood`() {
        val expenses = parse("Le 5 août j'ai payé 200 baht pour le déjeuner.")

        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(20000L, expense.amountMinorUnits)
        assertEquals("Dining", expense.category)
        assertEquals(LocalDate.of(2026, 8, 5), expense.occurredAt.toLocalDate())
    }

    @Test
    fun `promptpay and virement bancaire payment methods are understood`() {
        val promptPay = parse("J'ai payé 120 baht pour un café avec PromptPay.")
        assertEquals("PromptPay", promptPay[0].paymentMethod)

        val transfer = parse("J'ai payé 1200 baht pour le loyer par virement bancaire.")
        assertEquals("Bank Transfer", transfer[0].paymentMethod)
        assertEquals("Housing", transfer[0].category)
    }

    @Test
    fun `unmatched category falls back to uncategorized without discarding the expense`() {
        val expenses = parse("J'ai payé 45 baht pour des autocollants.")

        assertEquals(1, expenses.size)
        assertEquals("Uncategorized", expenses[0].category)
        assertNull(expenses[0].paymentMethod)
    }

    // The French base-20 number system (quatre-vingts = 4x20, soixante-dix = 60+10) is the
    // highest-risk part of French support - see NumberWordsTest for the isolated cases and
    // the plan's rationale. These confirm the same values survive the full parser pipeline.
    @Test
    fun `French base-twenty compound numbers survive the full parse pipeline`() {
        assertEquals(8000L, parse("quatre-vingts baht pour un café")[0].amountMinorUnits)
        assertEquals(9900L, parse("quatre-vingt-dix-neuf baht pour un café")[0].amountMinorUnits)
        assertEquals(7500L, parse("soixante-quinze baht pour un café")[0].amountMinorUnits)
        assertEquals(15000L, parse("cent cinquante baht pour un café")[0].amountMinorUnits)
        assertEquals(2100L, parse("vingt-et-un baht pour un café")[0].amountMinorUnits)
    }

    // Regression: a real on-device transcript mis-heard "50 baht" as "handball", leaving "pour
    // un café" behind. Before the NumberWords fix, "un" (French's indefinite article, "a") was
    // read as the number 1 and silently produced a bogus 1.00 THB expense - violating the
    // parser's core "never invent an amount" rule. It must now be dropped entirely, same as any
    // other clause with no real amount (see the English `ambiguous sentence` test).
    @Test
    fun `a garbled transcript leaving only the indefinite article does not invent an amount`() {
        val expenses = parse("handball pour un café")

        assertTrue(expenses.isEmpty())
    }
}
