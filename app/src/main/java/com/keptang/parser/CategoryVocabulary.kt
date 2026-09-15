package com.keptang.parser

/**
 * What the parser knows about categories for one parse, assembled by the caller from the user's
 * own data rather than hardcoded here - [CategoryRules]' built-in keywords only cover six
 * categories that happen to share the names of the six seeded ones, so a renamed or newly created
 * category is invisible to the parser without this.
 *
 * Kept as a plain value passed into [ExpenseParser.parse] so the parser stays free of Android and
 * Room and can still be exercised from plain JVM tests.
 *
 * @param learned keyword -> category, taught by the user correcting a category by hand. Highest
 *   priority, so a correction always beats a built-in rule ("massage" can mean Entertainment here
 *   and Health elsewhere - only the user knows which).
 * @param categoryNames the user's own category names, each matching itself, so creating a
 *   "Massage" category is enough for "Massage 300 baht" to land in it.
 */
data class CategoryVocabulary(
    val learned: Map<String, String> = emptyMap(),
    val categoryNames: List<String> = emptyList()
) {
    companion object {
        /** Built-in keywords only - what the parser used before categories became user-editable. */
        val EMPTY = CategoryVocabulary()
    }
}
