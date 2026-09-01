package com.keptang.parser

/**
 * Best-effort conversion of spoken English numbers ("eighty", "one hundred fifty") to a
 * numeric value. Only used as a fallback when no digit sequence is present in a clause -
 * digits found via [AmountExtractor] always take precedence since they are unambiguous.
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

    /**
     * Scans [text] for the first contiguous run of number-words and returns its value, or
     * null if no number-word is present.
     */
    fun extractValue(text: String): Double? {
        val words = text.lowercase().split(Regex("[^a-z]+")).filter { it.isNotBlank() }

        var runStart = -1
        for (i in words.indices) {
            if (words[i] in NUMBER_WORDS) {
                runStart = i
                break
            }
        }
        if (runStart == -1) return null

        var runEnd = runStart
        while (runEnd + 1 < words.size && words[runEnd + 1] in NUMBER_WORDS) {
            runEnd++
        }

        var total = 0
        var current = 0
        for (i in runStart..runEnd) {
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
}
