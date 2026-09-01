package com.keptang.parser

/** Deterministic keyword -> category mapping, per spec's initial category rules. */
object CategoryRules {

    private val RULES: List<Pair<List<String>, String>> = listOf(
        listOf("taxi", "grab", "bolt") to "Transport",
        listOf("dinner", "lunch", "restaurant") to "Dining",
        listOf("coffee", "cafe", "café") to "Coffee",
        listOf("groceries", "supermarket") to "Groceries",
        listOf("rent") to "Housing",
        listOf("electricity", "water", "internet", "phone") to "Utilities"
    )

    val ALL_KEYWORDS: List<String> = RULES.flatMap { it.first }

    fun classify(segment: String): String? {
        val lower = segment.lowercase()
        for ((keywords, category) in RULES) {
            if (keywords.any { keyword -> Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(lower) }) {
                return category
            }
        }
        return null
    }
}
