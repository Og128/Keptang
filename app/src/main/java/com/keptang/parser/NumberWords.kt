package com.keptang.parser

/**
 * Best-effort conversion of spoken numbers ("eighty" / "quatre-vingts", "one hundred fifty" /
 * "cent cinquante") to a numeric value. Only used as a fallback when no digit sequence is
 * present in a clause - digits found via [AmountExtractor] always take precedence since they
 * are unambiguous.
 */
object NumberWords {

    private val ONES = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
        "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19
    )

    private val TENS = mapOf(
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
        "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
    )

    private val NUMBER_WORDS = ONES.keys + TENS.keys + setOf("hundred", "thousand")

    // French number words don't decompose as cleanly as English: 17-19 and 70-79 are additive
    // compounds of ONES_FR entries (dix-sept = 10+7, soixante-dix-sept = 60+10+7) and need no
    // special casing, but "vingt" (20) and "cent" (100) are multiplicative when preceded by a
    // digit (quatre-vingts = 4x20, deux cents = 2x100) exactly like English "hundred" - so they
    // live in MULTIPLIERS_FR and share that logic, rather than being simply additive like TENS.
    private val ONES_FR = mapOf(
        "zéro" to 0, "un" to 1, "une" to 1, "deux" to 2, "trois" to 3, "quatre" to 4,
        "cinq" to 5, "six" to 6, "sept" to 7, "huit" to 8, "neuf" to 9,
        "dix" to 10, "onze" to 11, "douze" to 12, "treize" to 13, "quatorze" to 14,
        "quinze" to 15, "seize" to 16
    )

    private val TENS_FR = mapOf(
        "trente" to 30, "quarante" to 40, "cinquante" to 50, "soixante" to 60
    )

    private val MULTIPLIERS_FR = mapOf(
        "vingt" to 20, "vingts" to 20, "cent" to 100, "cents" to 100
    )

    private val SKIP_FR = setOf("et")

    private val NUMBER_WORDS_FR = ONES_FR.keys + TENS_FR.keys + MULTIPLIERS_FR.keys + setOf("mille") + SKIP_FR

    // "un"/"une" double as French's indefinite article ("un café" = "a coffee"), not just the
    // digit 1 - unlike English, which uses "a"/"an" rather than "one" for that role. Matching a
    // bare "un" anywhere in a sentence as an amount would misfire constantly on completely
    // ordinary French (see NumberWordsTest / the "un café" false-positive this fixes), so a run
    // may only *start* on an unambiguous word; "un"/"une" can still complete a compound they
    // trail (vingt-et-un, quatre-vingt-un) since French numbers never lead with them.
    private val STRONG_NUMBER_WORDS_FR = (ONES_FR.keys - setOf("un", "une")) + TENS_FR.keys + MULTIPLIERS_FR.keys + setOf("mille")

    /**
     * Scans [text] for the first contiguous run of number-words and returns its value, or
     * null if no number-word is present.
     */
    fun extractValue(text: String, languageCode: String = "en"): Double? =
        if (languageCode == "fr") extractValueFr(text) else extractValueEn(text)

    private fun extractValueEn(text: String): Double? {
        val words = tokenize(text)
        val run = findRun(words, seedVocabulary = NUMBER_WORDS) ?: return null

        var total = 0
        var current = 0
        for (i in run) {
            val w = words[i]
            when {
                w == "hundred" -> current = (if (current == 0) 1 else current) * 100
                w == "thousand" -> {
                    total += (if (current == 0) 1 else current) * 1000
                    current = 0
                }
                ONES.containsKey(w) -> current += ONES.getValue(w)
                TENS.containsKey(w) -> current += TENS.getValue(w)
            }
        }
        return (total + current).toDouble()
    }

    private fun extractValueFr(text: String): Double? {
        val words = tokenize(text)
        val run = findRun(words, seedVocabulary = STRONG_NUMBER_WORDS_FR, extendVocabulary = NUMBER_WORDS_FR) ?: return null

        var total = 0
        var current = 0
        for (i in run) {
            val w = words[i]
            when {
                w == "mille" -> {
                    total += (if (current == 0) 1 else current) * 1000
                    current = 0
                }
                MULTIPLIERS_FR.containsKey(w) -> current = (if (current == 0) 1 else current) * MULTIPLIERS_FR.getValue(w)
                ONES_FR.containsKey(w) -> current += ONES_FR.getValue(w)
                TENS_FR.containsKey(w) -> current += TENS_FR.getValue(w)
                w in SKIP_FR -> Unit
            }
        }
        return (total + current).toDouble()
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase().split(Regex("[^a-zàâäéèêëïîôöùûüÿœæç]+")).filter { it.isNotBlank() }

    /**
     * Finds the first contiguous run of number-words, as an index range. The run may only
     * *start* on a word from [seedVocabulary] (unambiguous number words), but once started, it
     * extends through any adjacent word from [extendVocabulary] - which may be broader, to also
     * pick up words that are only valid as part of a compound (see [STRONG_NUMBER_WORDS_FR]).
     */
    private fun findRun(words: List<String>, seedVocabulary: Set<String>, extendVocabulary: Set<String> = seedVocabulary): IntRange? {
        val runStart = words.indices.firstOrNull { words[it] in seedVocabulary } ?: return null
        var runEnd = runStart
        while (runEnd + 1 < words.size && words[runEnd + 1] in extendVocabulary) {
            runEnd++
        }
        return runStart..runEnd
    }
}
