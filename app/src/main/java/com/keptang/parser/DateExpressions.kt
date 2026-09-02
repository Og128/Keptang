package com.keptang.parser

import java.time.LocalDate

/**
 * Recognizes date phrases inside a single expense clause. "today"/"yesterday" are always
 * resolved relative to the real capture instant, never relative to a previously-inherited
 * date, so a clause that repeats "today" always means the capture day regardless of what an
 * earlier clause set [ExpenseParser]'s running date state to.
 */
object DateExpressions {

    private val MONTHS = listOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december"
    )

    private val MONTHS_FR = listOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre"
    )

    private val ISO_DATE_REGEX = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""")

    private val MONTH_DAY_REGEX = Regex(
        """\b(${MONTHS.joinToString("|")})\s+(\d{1,2})(?:st|nd|rd|th)?\b""",
        RegexOption.IGNORE_CASE
    )
    private val DAY_MONTH_REGEX = Regex(
        """\b(\d{1,2})(?:st|nd|rd|th)?\s+(${MONTHS.joinToString("|")})\b""",
        RegexOption.IGNORE_CASE
    )
    private val TODAY_REGEX = Regex("""\btoday\b""", RegexOption.IGNORE_CASE)
    private val YESTERDAY_REGEX = Regex("""\byesterday\b""", RegexOption.IGNORE_CASE)

    // French dates are always day-then-month ("5 août", never "août 5"), and use no ordinal
    // suffix except "1er" for the first of the month - unlike English's st/nd/rd/th.
    private val DAY_MONTH_REGEX_FR = Regex(
        """\b(\d{1,2})(?:er)?\s+(${MONTHS_FR.joinToString("|")})\b""",
        RegexOption.IGNORE_CASE
    )
    private val TODAY_REGEX_FR = Regex("""\baujourd'?hui\b""", RegexOption.IGNORE_CASE)
    private val YESTERDAY_REGEX_FR = Regex("""\bhier\b""", RegexOption.IGNORE_CASE)

    fun isToday(segment: String, languageCode: String = "en"): Boolean =
        (if (languageCode == "fr") TODAY_REGEX_FR else TODAY_REGEX).containsMatchIn(segment)

    fun isYesterday(segment: String, languageCode: String = "en"): Boolean =
        (if (languageCode == "fr") YESTERDAY_REGEX_FR else YESTERDAY_REGEX).containsMatchIn(segment)

    /** Returns an absolute date if [segment] contains an explicit (non-relative) date, else null. */
    fun extractExplicitDate(segment: String, referenceYear: Int, languageCode: String = "en"): LocalDate? {
        ISO_DATE_REGEX.find(segment)?.let { m ->
            return runCatching {
                LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
            }.getOrNull()
        }
        if (languageCode == "fr") {
            DAY_MONTH_REGEX_FR.find(segment)?.let { m ->
                val day = m.groupValues[1].toIntOrNull() ?: return null
                val month = MONTHS_FR.indexOf(m.groupValues[2].lowercase()) + 1
                return runCatching { LocalDate.of(referenceYear, month, day) }.getOrNull()
            }
            return null
        }
        MONTH_DAY_REGEX.find(segment)?.let { m ->
            val month = MONTHS.indexOf(m.groupValues[1].lowercase()) + 1
            val day = m.groupValues[2].toIntOrNull() ?: return null
            return runCatching { LocalDate.of(referenceYear, month, day) }.getOrNull()
        }
        DAY_MONTH_REGEX.find(segment)?.let { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return null
            val month = MONTHS.indexOf(m.groupValues[2].lowercase()) + 1
            return runCatching { LocalDate.of(referenceYear, month, day) }.getOrNull()
        }
        return null
    }

    /** Removes explicit date substrings so amount extraction never mistakes a day/year for money. */
    fun stripDatePhrases(segment: String, languageCode: String = "en"): String {
        var result = ISO_DATE_REGEX.replace(segment, " ")
        if (languageCode == "fr") {
            result = DAY_MONTH_REGEX_FR.replace(result, " ")
        } else {
            result = MONTH_DAY_REGEX.replace(result, " ")
            result = DAY_MONTH_REGEX.replace(result, " ")
        }
        return result
    }
}
