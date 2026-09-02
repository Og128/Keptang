package com.keptang.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberWordsTest {

    @Test
    fun `English regression values are unaffected by the French dispatch`() {
        assertEquals(80.0, NumberWords.extractValue("eighty"))
        assertEquals(150.0, NumberWords.extractValue("one hundred fifty"))
    }

    @Test
    fun `French compound numbers 17 to 19 are additive with no special casing`() {
        assertEquals(17.0, NumberWords.extractValue("dix-sept", "fr"))
        assertEquals(18.0, NumberWords.extractValue("dix-huit", "fr"))
        assertEquals(19.0, NumberWords.extractValue("dix-neuf", "fr"))
    }

    @Test
    fun `French 70s use the soixante-dix base-twenty pattern`() {
        assertEquals(70.0, NumberWords.extractValue("soixante-dix", "fr"))
        assertEquals(71.0, NumberWords.extractValue("soixante-et-onze", "fr"))
        assertEquals(75.0, NumberWords.extractValue("soixante-quinze", "fr"))
    }

    @Test
    fun `French 80s use quatre-vingts as a multiplier not an addend`() {
        assertEquals(80.0, NumberWords.extractValue("quatre-vingts", "fr"))
        assertEquals(83.0, NumberWords.extractValue("quatre-vingt-trois", "fr"))
        assertEquals(90.0, NumberWords.extractValue("quatre-vingt-dix", "fr"))
        assertEquals(95.0, NumberWords.extractValue("quatre-vingt-quinze", "fr"))
    }

    @Test
    fun `French vingt-et-un is 21 not 24`() {
        assertEquals(21.0, NumberWords.extractValue("vingt-et-un", "fr"))
    }

    @Test
    fun `French hundred and thousand multipliers`() {
        assertEquals(100.0, NumberWords.extractValue("cent", "fr"))
        assertEquals(200.0, NumberWords.extractValue("deux cents", "fr"))
        assertEquals(1000.0, NumberWords.extractValue("mille", "fr"))
        assertEquals(1100.0, NumberWords.extractValue("mille cent", "fr"))
    }

    @Test
    fun `no number words present yields null in either language`() {
        assertNull(NumberWords.extractValue("nothing here"))
        assertNull(NumberWords.extractValue("rien ici", "fr"))
    }

    // Regression: "un"/"une" are French's indefinite article ("un café" = "a coffee") as well as
    // the digit 1. A bare, unseeded "un" must never be read as an amount on its own - only a
    // real ASR mis-transcription ("50 baht" heard as "handball") surfaced this, see conversation.
    @Test
    fun `bare un as an indefinite article is not mistaken for the number one`() {
        assertNull(NumberWords.extractValue("pour un café", "fr"))
        assertNull(NumberWords.extractValue("handball pour un café", "fr"))
        assertNull(NumberWords.extractValue("une baguette", "fr"))
    }

    @Test
    fun `un still completes a compound it trails`() {
        assertEquals(21.0, NumberWords.extractValue("vingt-et-un baht", "fr"))
        assertEquals(81.0, NumberWords.extractValue("quatre-vingt-un baht", "fr"))
    }
}
