package com.keptang.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The description is what the ledger shows and what category learning keys on, so it has to
 * survive words the parser doesn't recognise - that's precisely the case it needs to describe.
 */
class DescriptionExtractorTest {

    private fun extract(segment: String) = DescriptionExtractor.extract(segment)

    @Test
    fun keepsTheMerchantRatherThanTheCategoryKeywordInFrontOfIt() {
        assertEquals("Starbucks", extract("Coffee at Starbucks 120 baht"))
        assertEquals("Sizzler", extract("Lunch at Sizzler 450 baht"))
    }

    @Test
    fun describesWordsNoCategoryRuleKnows() {
        assertEquals("Massage", extract("Massage 300 baht"))
        assertEquals("Starbucks", extract("Starbucks 120 baht"))
    }

    @Test
    fun readsThroughTheVerbAndThePreposition() {
        assertEquals("Dinner", extract("I paid 550 baht for dinner"))
        assertEquals("Dinner", extract("yesterday night I paid 550 baht for dinner"))
    }

    @Test
    fun dropsTheArticleInAPlaceName() {
        assertEquals("Night Market", extract("Dinner at the night market 200 baht"))
    }

    @Test
    fun leavesExistingCapitalisationAlone() {
        assertEquals("BTS", extract("BTS 44 baht"))
    }

    @Test
    fun ignoresThePaymentMethodAndAccountAnotherExtractorAlreadyTook() {
        assertEquals("Groceries", extract("Groceries 800 baht in cash"))
        assertEquals("Dinner", extract("Dinner 300 baht from my Bangkok Bank account"))
    }

    @Test
    fun returnsNullWhenThereIsNothingButAnAmount() {
        assertNull(extract("50 baht"))
        assertNull(extract("I paid 50 baht"))
    }

    @Test
    fun capsRamblingInputSoTheLedgerStaysReadable() {
        val description = extract("Dinner at that very nice riverside place near the pier 800 baht")

        assertEquals(3, description!!.split(" ").size)
    }
}
