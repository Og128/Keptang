package com.keptang.parser

/** Recognizes payment methods: cash, card, PromptPay, bank transfer. */
object PaymentMethodExtractor {

    fun extract(segment: String): String? {
        val lower = segment.lowercase()
        return when {
            Regex("""\bpromptpay\b""").containsMatchIn(lower) -> "PromptPay"
            Regex("""\bbank transfer\b|\btransfer\b""").containsMatchIn(lower) -> "Bank Transfer"
            Regex("""\bcredit card\b|\bdebit card\b|\bcard\b""").containsMatchIn(lower) -> "Card"
            Regex("""\bcash\b""").containsMatchIn(lower) -> "Cash"
            else -> null
        }
    }
}
