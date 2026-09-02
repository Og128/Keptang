package com.keptang.parser

/** Falls back to the category-triggering keyword as a human-readable merchant/description. */
object MerchantExtractor {

    fun extract(segment: String, languageCode: String = "en"): String? {
        val keywords = if (languageCode == "fr") CategoryRules.ALL_KEYWORDS_FR else CategoryRules.ALL_KEYWORDS
        val lower = segment.lowercase()
        val keyword = keywords.firstOrNull { keyword ->
            Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(lower)
        }
        return keyword?.replaceFirstChar { it.uppercaseChar() }
    }
}
