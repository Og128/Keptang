package com.keptang.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Classification against the user's own vocabulary, and the comma handling that used to send
 * perfectly clear sentences to "Uncategorized".
 */
class CategoryVocabularyTest {

    private val parser = ExpenseParser()
    private val now = ZonedDateTime.of(2026, 9, 15, 12, 0, 0, 0, ZoneId.of("Asia/Bangkok"))

    private fun parse(transcript: String, vocabulary: CategoryVocabulary = CategoryVocabulary.EMPTY) =
        parser.parse(transcript, "c1", now, "en", vocabulary)

    // --- A comma is not a new expense unless it brings its own amount ---

    @Test
    fun keepsAnExpenseWholeWhenTheRecognizerPunctuatesBeforeTheAmount() {
        val expenses = parse("Lunch, 50 baht")

        assertEquals(1, expenses.size)
        assertEquals("Dining", expenses[0].category)
        assertEquals("Lunch", expenses[0].description)
        assertEquals(5000, expenses[0].amountMinorUnits)
    }

    @Test
    fun keepsAnExpenseWholeWhenThePunctuationFollowsTheAmount() {
        val expenses = parse("50 baht, coffee")

        assertEquals(1, expenses.size)
        assertEquals("Coffee", expenses[0].category)
    }

    @Test
    fun stillSplitsWhenBothHalvesNameTheirOwnAmount() {
        val expenses = parse("550 baht for dinner, 25 baht for coffee")

        assertEquals(2, expenses.size)
        assertEquals("Dining", expenses[0].category)
        assertEquals(55000, expenses[0].amountMinorUnits)
        assertEquals("Coffee", expenses[1].category)
        assertEquals(2500, expenses[1].amountMinorUnits)
    }

    // --- The user's own categories and corrections ---

    @Test
    fun classifiesIntoAUserCreatedCategoryByItsOwnName() {
        val expenses = parse("Massage 300 baht", CategoryVocabulary(categoryNames = listOf("Massage", "Coffee")))

        assertEquals("Massage", expenses[0].category)
    }

    @Test
    fun aLearnedCorrectionBeatsTheCategoryNameItWasCorrectedAwayFrom() {
        val vocabulary = CategoryVocabulary(
            learned = mapOf("massage" to "Entertainment"),
            categoryNames = listOf("Massage", "Entertainment")
        )

        assertEquals("Entertainment", parse("Massage 300 baht", vocabulary)[0].category)
    }

    @Test
    fun aLearnedCorrectionBeatsABuiltInKeyword() {
        val vocabulary = CategoryVocabulary(learned = mapOf("coffee" to "Groceries"))

        assertEquals("Groceries", parse("Coffee 50 baht", vocabulary)[0].category)
    }

    @Test
    fun theLongestMatchingKeywordWins() {
        val vocabulary = CategoryVocabulary(
            learned = mapOf("coffee" to "Coffee", "iced coffee" to "Treats")
        )

        assertEquals("Treats", parse("Iced coffee 70 baht", vocabulary)[0].category)
    }

    @Test
    fun fallsBackToUncategorizedWhenNothingKnowsTheWord() {
        val expenses = parse("Massage 300 baht")

        assertEquals("Uncategorized", expenses[0].category)
    }

    // --- Built-in vocabulary widened for everyday Thai spending ---

    @Test
    fun recognizesTheEverydayWordsThatUsedToFallThrough() {
        assertEquals("Dining", parse("Breakfast 80 baht")[0].category)
        assertEquals("Dining", parse("Beer 100 baht")[0].category)
        assertEquals("Transport", parse("BTS 44 baht")[0].category)
        assertEquals("Groceries", parse("7-Eleven 120 baht")[0].category)
    }

    @Test
    fun classifyReturnsNullRatherThanGuessingWhenAClauseSaysNothing() {
        assertNull(CategoryRules.classify("something entirely unknown"))
    }
}
