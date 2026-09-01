package com.keptang.parser

/** Recognizes account expressions such as "from my Bangkok Bank account". */
object AccountExtractor {

    private val REGEX = Regex(
        """from\s+(?:my\s+)?([a-zA-Z][a-zA-Z ]*?)\s+account\b""",
        RegexOption.IGNORE_CASE
    )

    fun extract(segment: String): String? {
        val match = REGEX.find(segment) ?: return null
        val raw = match.groupValues[1].trim()
        if (raw.isEmpty()) return null
        return raw.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { c -> c.uppercaseChar() }
        }
    }
}
