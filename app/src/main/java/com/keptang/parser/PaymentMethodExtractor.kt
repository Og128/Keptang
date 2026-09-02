package com.keptang.parser

/** Recognizes payment methods: cash, card, PromptPay, bank transfer. */
object PaymentMethodExtractor {

    fun extract(segment: String, languageCode: String = "en"): String? {
        val lower = segment.lowercase()
        return if (languageCode == "fr") extractFr(lower) else extractEn(lower)
    }

    private fun extractEn(lower: String): String? = when {
        Regex("""\bpromptpay\b""").containsMatchIn(lower) -> "PromptPay"
        Regex("""\bbank transfer\b|\btransfer\b""").containsMatchIn(lower) -> "Bank Transfer"
        Regex("""\bcredit card\b|\bdebit card\b|\bcard\b""").containsMatchIn(lower) -> "Card"
        Regex("""\bcash\b""").containsMatchIn(lower) -> "Cash"
        else -> null
    }

    private fun extractFr(lower: String): String? = when {
        Regex("""\bpromptpay\b""").containsMatchIn(lower) -> "PromptPay"
        Regex("""\bvirement(?:\s+bancaire)?\b""").containsMatchIn(lower) -> "Bank Transfer"
        Regex("""\bcarte(?:\s+de\s+cr[ée]dit|\s+de\s+d[ée]bit)?\b""").containsMatchIn(lower) -> "Card"
        Regex("""\besp[eè]ces\b|\bliquide\b""").containsMatchIn(lower) -> "Cash"
        else -> null
    }
}
