package com.keptang.parser

/**
 * Maps a clause to a category, deterministically. Three sources are consulted in order of how
 * much they know about *this* user:
 *
 *  1. what the user taught by correcting a category by hand ([CategoryVocabulary.learned]),
 *  2. the user's own category names ([CategoryVocabulary.categoryNames]), each matching itself,
 *  3. the built-in synonym lists below, which only exist to make a fresh install useful on day one.
 *
 * Within a source the longest keyword wins, so "iced coffee" beats "coffee" when both are known.
 */
object CategoryRules {

    private val RULES: List<Pair<List<String>, String>> = listOf(
        listOf(
            "taxi", "grab", "bolt", "tuk tuk", "tuktuk", "motorbike", "motorbike taxi", "moto",
            "bts", "mrt", "skytrain", "metro", "subway", "bus", "train", "ferry", "boat",
            "fuel", "petrol", "gas station", "parking", "flight", "plane ticket"
        ) to "Transport",
        listOf(
            "dinner", "lunch", "breakfast", "brunch", "supper", "restaurant", "food", "meal",
            "street food", "noodles", "pad thai", "som tam", "snack", "dessert", "ice cream",
            "bar", "beer", "drinks", "cocktail", "wine", "takeaway", "delivery", "food delivery"
        ) to "Dining",
        listOf("coffee", "cafe", "café", "latte", "espresso", "cappuccino", "iced coffee", "tea", "starbucks") to "Coffee",
        listOf(
            "groceries", "grocery", "supermarket", "market", "fresh market", "7-eleven", "seven eleven",
            "711", "7 11", "convenience store", "big c", "lotus", "makro", "tops"
        ) to "Groceries",
        listOf("rent", "deposit", "condo", "apartment", "maintenance fee") to "Housing",
        listOf("electricity", "water", "internet", "phone", "wifi", "mobile", "sim", "top up", "utility bill") to "Utilities"
    )

    private val RULES_FR: List<Pair<List<String>, String>> = listOf(
        listOf(
            "taxi", "grab", "bolt", "tuk tuk", "moto", "scooter", "métro", "metro", "bus",
            "train", "bateau", "essence", "carburant", "parking", "vol", "billet d'avion"
        ) to "Transport",
        listOf(
            "dîner", "diner", "déjeuner", "dejeuner", "petit déjeuner", "petit dejeuner",
            "restaurant", "resto", "repas", "nourriture", "bouffe", "snack", "dessert", "glace",
            "bar", "bière", "biere", "verre", "vin", "livraison"
        ) to "Dining",
        listOf("café", "cafe", "latte", "expresso", "espresso", "thé", "the", "starbucks") to "Coffee",
        listOf("courses", "supermarché", "supermarche", "marché", "marche", "épicerie", "epicerie", "7-eleven") to "Groceries",
        listOf("loyer", "caution", "appartement", "charges") to "Housing",
        listOf("électricité", "electricite", "eau", "internet", "téléphone", "telephone", "wifi", "forfait", "recharge") to "Utilities"
    )

    val ALL_KEYWORDS: List<String> = RULES.flatMap { it.first }
    val ALL_KEYWORDS_FR: List<String> = RULES_FR.flatMap { it.first }

    fun classify(
        segment: String,
        languageCode: String = "en",
        vocabulary: CategoryVocabulary = CategoryVocabulary.EMPTY
    ): String? {
        val lower = segment.lowercase()

        vocabulary.learned
            .filterKeys { keyword -> containsWord(lower, keyword.lowercase()) }
            .maxByOrNull { (keyword, _) -> keyword.length }
            ?.let { return it.value }

        vocabulary.categoryNames
            .filter { name -> containsWord(lower, name.lowercase()) }
            .maxByOrNull { name -> name.length }
            ?.let { return it }

        val rules = if (languageCode == "fr") RULES_FR else RULES
        return rules
            .flatMap { (keywords, category) -> keywords.map { keyword -> keyword to category } }
            .filter { (keyword, _) -> containsWord(lower, keyword) }
            .maxByOrNull { (keyword, _) -> keyword.length }
            ?.second
    }

    /** Whole-word match, so "the" never matches inside "theatre" and "711" never inside "7115". */
    internal fun containsWord(lowerSegment: String, lowerKeyword: String): Boolean {
        if (lowerKeyword.isBlank()) return false
        return Regex("(?<![\\p{L}\\p{N}])${Regex.escape(lowerKeyword)}(?![\\p{L}\\p{N}])")
            .containsMatchIn(lowerSegment)
    }
}
