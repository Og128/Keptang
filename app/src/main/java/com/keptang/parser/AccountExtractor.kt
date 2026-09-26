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

    /**
     * @param knownNames the accounts that actually exist, so "coffee 50 on HSBC" resolves without
     *   the speaker having to say the word "account". Checked first, and longest-first so a
     *   "K-bank savings" never loses to a "K-bank" that is merely a prefix of it. A bare name
     *   could not be recognised any other way: without the list, any noun would qualify.
     */
    fun extract(segment: String, languageCode: String = "en", knownNames: List<String> = emptyList()): String? {
        knownNames
            .filter { it.isNotBlank() }
            .sortedByDescending { it.length }
            .firstOrNull { name -> nameRegex(name).containsMatchIn(segment) }
            ?.let { return it }

        val match = (if (languageCode == "fr") REGEX_FR else REGEX).find(segment) ?: return null
        val raw = match.groupValues[1].trim()
        if (raw.isEmpty()) return null
        return raw.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { c -> c.uppercaseChar() }
        }
    }

    /**
     * Word-bounded and escaped, since account names are user input: "K-bank" contains regex
     * metacharacters, and an unbounded match would find "TTB" inside "TTBK".
     */
    internal fun nameRegex(name: String): Regex =
        Regex("""(?<![\p{L}\p{N}])${Regex.escape(name.trim())}(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)
}
