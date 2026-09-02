package com.keptang.parser

/**
 * Recognizes account expressions such as "from my Bangkok Bank account" / French "depuis mon
 * compte Kasikorn" - the preposition and "compte"/"account" swap sides of the account name, so
 * this needs a genuinely different pattern rather than a vocabulary swap.
 */
object AccountExtractor {

    private val REGEX = Regex(
        """from\s+(?:my\s+)?([a-zA-Z][a-zA-Z ]*?)\s+account\b""",
        RegexOption.IGNORE_CASE
    )

    private val REGEX_FR = Regex(
        """(?:depuis|de)\s+mon\s+compte\s+([a-zA-Zàâäéèêëïîôöùûüÿœæç][a-zA-Zàâäéèêëïîôöùûüÿœæç ]*?)(?=[.,;]|\s+(?:pour|avec|en)\b|$)""",
        RegexOption.IGNORE_CASE
    )

    fun extract(segment: String, languageCode: String = "en"): String? {
        val match = (if (languageCode == "fr") REGEX_FR else REGEX).find(segment) ?: return null
        val raw = match.groupValues[1].trim()
        if (raw.isEmpty()) return null
        return raw.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { c -> c.uppercaseChar() }
        }
    }
}
