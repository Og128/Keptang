package com.keptang.parser

/**
 * Extracts a single monetary amount from an expense clause. Currency-tagged numbers
 * ("150 baht", "฿150") are always preferred over bare numbers, and bare numbers are only
 * searched for after date substrings have been stripped so a day-of-month is never mistaken
 * for an amount. Defaults to THB when no currency is spoken, per spec.
 */
object AmountExtractor {

    data class Amount(val minorUnits: Long, val currencyCode: String)

    private val CURRENCY_TAGGED_REGEX = Regex(
        """(?:฿\s*(\d+(?:\.\d+)?))|(?:(\d+(?:\.\d+)?)\s*(?:baht|thb))\b""",
        RegexOption.IGNORE_CASE
    )
    private val BARE_NUMBER_REGEX = Regex("""\b(\d+(?:\.\d+)?)\b""")

    fun extract(segment: String, languageCode: String = "en"): Amount? {
        CURRENCY_TAGGED_REGEX.find(segment)?.let { m ->
            val numStr = m.groupValues[1].ifBlank { m.groupValues[2] }
            if (numStr.isNotBlank()) return toAmount(numStr, "THB")
        }

        val stripped = DateExpressions.stripDatePhrases(segment, languageCode)
        BARE_NUMBER_REGEX.find(stripped)?.let { m ->
            return toAmount(m.groupValues[1], "THB")
        }

        NumberWords.extractValue(stripped, languageCode)?.let { value ->
            return Amount(Math.round(value * 100.0), "THB")
        }

        return null
    }

    private fun toAmount(numStr: String, currencyCode: String): Amount {
        val value = numStr.toDouble()
        return Amount(Math.round(value * 100.0), currencyCode)
    }
}
