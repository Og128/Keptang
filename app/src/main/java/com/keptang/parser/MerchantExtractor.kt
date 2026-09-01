package com.keptang.parser

/** Falls back to the category-triggering keyword as a human-readable merchant/description. */
object MerchantExtractor {

    fun extract(segment: String): String? {
        val lower = segment.lowercase()
        val keyword = CategoryRules.ALL_KEYWORDS.firstOrNull { keyword ->
            Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(lower)
        }
        return keyword?.replaceFirstChar { it.uppercaseChar() }
    }
}
