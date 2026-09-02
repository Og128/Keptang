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

    private val RULES_FR: List<Pair<List<String>, String>> = listOf(
        listOf("taxi", "grab", "bolt") to "Transport",
        listOf("dîner", "diner", "déjeuner", "dejeuner", "restaurant") to "Dining",
        listOf("café", "cafe") to "Coffee",
        listOf("courses", "supermarché", "supermarche") to "Groceries",
        listOf("loyer") to "Housing",
        listOf("électricité", "electricite", "eau", "internet", "téléphone", "telephone") to "Utilities"
    )

    val ALL_KEYWORDS: List<String> = RULES.flatMap { it.first }
    val ALL_KEYWORDS_FR: List<String> = RULES_FR.flatMap { it.first }

    fun classify(segment: String, languageCode: String = "en"): String? {
        val rules = if (languageCode == "fr") RULES_FR else RULES
        val lower = segment.lowercase()
        for ((keywords, category) in rules) {
            if (keywords.any { keyword -> Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(lower) }) {
                return category
            }
        }
        return null
    }
}
