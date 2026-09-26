package com.keptang.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountExtractorTest {

    private val known = listOf("K-bank", "TTBK", "HSBC", "K-bank savings")

    @Test
    fun `a bare account name is recognised when the account exists`() {
        assertEquals("HSBC", AccountExtractor.extract("coffee 50 baht on HSBC", knownNames = known))
    }

    @Test
    fun `matching is case insensitive, since speech recognition does not capitalise reliably`() {
        assertEquals("K-bank", AccountExtractor.extract("dinner 300 paid with k-bank", knownNames = known))
    }

    /** The stored spelling wins, so the resolver can look the account up by name afterwards. */
    @Test
    fun `the canonical name is returned rather than what was said`() {
        assertEquals("TTBK", AccountExtractor.extract("taxi 120 ttbk", knownNames = known))
    }

    @Test
    fun `the longest matching name wins over a name that is merely its prefix`() {
        assertEquals(
            "K-bank savings",
            AccountExtractor.extract("rent 12000 from K-bank savings", knownNames = known)
        )
    }

    /** Without word boundaries "TTB" would be found inside "TTBK" and the wrong account charged. */
    @Test
    fun `a name embedded in a longer word is not a match`() {
        assertNull(AccountExtractor.extract("coffee 50 ttbk", knownNames = listOf("TTB")))
    }

    /** Names are user input, so a metacharacter must be matched literally rather than compiled. */
    @Test
    fun `a name containing regex metacharacters is matched literally`() {
        assertEquals("K+bank", AccountExtractor.extract("lunch 90 K+bank", knownNames = listOf("K+bank")))
        assertNull(AccountExtractor.extract("lunch 90 Kbank", knownNames = listOf("K+bank")))
    }

    @Test
    fun `an unknown name is not invented as an account`() {
        assertNull(AccountExtractor.extract("coffee 50 baht on Mars", knownNames = known))
    }

    @Test
    fun `the spoken phrase still works when no accounts are known yet`() {
        assertEquals("Bangkok Bank", AccountExtractor.extract("paid 200 from my Bangkok Bank account"))
    }

    @Test
    fun `a known name beats the spoken phrase, so the stored spelling is kept`() {
        assertEquals(
            "HSBC",
            AccountExtractor.extract("paid 200 from my HSBC account", knownNames = known)
        )
    }
}
