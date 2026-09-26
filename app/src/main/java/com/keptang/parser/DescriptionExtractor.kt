package com.keptang.parser

/**
 * Pulls out what the expense was *for* - "Starbucks", "Massage", "Night Market" - independently
 * of whether a category matched.
 *
 * It used to be [CategoryRules]' matching keyword, capitalised, which meant two things: an
 * unrecognised word produced no description at all ("Massage 300 baht" -> null), and a real
 * merchant was thrown away whenever a category keyword sat in front of it ("Coffee at Starbucks"
 * -> "Coffee"). Since the description is both what the ledger shows and what
 * [com.keptang.data.repository.LearnedCategoryRepository] learns from, it has to stand on its own.
 *
 * Strategy, in order: the phrase after "at" (a place), else the phrase after "for" (a thing),
 * else whatever content words are left once amounts, dates, accounts, payment methods and filler
 * have been stripped.
 */
object DescriptionExtractor {

    private const val MAX_WORDS = 3

    private val AMOUNT_WORDS = Regex(
        """(?:฿|\b(?:baht|thb|bahts?)\b|\b\d+(?:[.,]\d+)?\b)""",
        RegexOption.IGNORE_CASE
    )

    private val ACCOUNT_PHRASE = Regex("""\bfrom\s+(?:my\s+)?[a-z ]*?\baccount\b""", RegexOption.IGNORE_CASE)
    private val ACCOUNT_PHRASE_FR = Regex("""\b(?:depuis|de)\s+mon\s+compte\b[\p{L} ]*""", RegexOption.IGNORE_CASE)

    private val AT_PHRASE = Regex("""\bat\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val FOR_PHRASE = Regex("""\bfor\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val AT_PHRASE_FR = Regex("""\b(?:chez|au|à la)\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val FOR_PHRASE_FR = Regex("""\bpour\s+(.+)$""", RegexOption.IGNORE_CASE)

    /** Filler that never describes an expense on its own. Deliberately small - anything not listed survives. */
    private val STOPWORDS = setOf(
        "i", "we", "my", "me", "a", "an", "the", "this", "that", "it", "its",
        "paid", "pay", "spent", "spend", "bought", "buy", "got", "put", "cost", "costs",
        "on", "in", "of", "to", "with", "by", "and", "or", "was", "were", "is", "some",
        "today", "yesterday", "morning", "afternoon", "evening", "tonight", "just", "about",
        "cash", "card", "credit", "debit", "promptpay", "transfer", "bank", "account"
    )

    private val STOPWORDS_FR = setOf(
        "je", "j", "on", "nous", "mon", "ma", "mes", "le", "la", "les", "un", "une", "des", "du", "de", "d",
        "ai", "payé", "paye", "payer", "dépensé", "depense", "dépense", "acheté", "achete", "coûté", "coute",
        "en", "au", "aux", "à", "a", "avec", "et", "ou", "pour", "c", "est", "ce", "cette",
        "aujourd'hui", "hier", "matin", "midi", "soir", "ce",
        "espèces", "especes", "liquide", "carte", "virement", "compte"
    )

    private val ARTICLES = setOf("the", "a", "an", "my", "le", "la", "les", "un", "une", "des", "mon", "ma")

    fun extract(segment: String, languageCode: String = "en", accountNames: List<String> = emptyList()): String? {
        val isFrench = languageCode == "fr"
        val cleaned = clean(segment, languageCode, accountNames)
        if (cleaned.isBlank()) return null

        val placePattern = if (isFrench) AT_PHRASE_FR else AT_PHRASE
        val thingPattern = if (isFrench) FOR_PHRASE_FR else FOR_PHRASE

        placePattern.find(cleaned)?.let { match ->
            titleCase(takeWords(match.groupValues[1], languageCode))?.let { return it }
        }
        thingPattern.find(cleaned)?.let { match ->
            titleCase(takeWords(match.groupValues[1], languageCode))?.let { return it }
        }

        return titleCase(takeWords(cleaned, languageCode))
    }

    /** Removes everything that is captured by another extractor, so only the subject is left. */
    private fun clean(segment: String, languageCode: String, accountNames: List<String>): String {
        var text = DateExpressions.stripDatePhrases(segment, languageCode)
        text = if (languageCode == "fr") ACCOUNT_PHRASE_FR.replace(text, " ") else ACCOUNT_PHRASE.replace(text, " ")
        // A spoken account name is not what the expense was for. Without this, "coffee on HSBC"
        // describes itself as "Coffee On HSBC".
        for (name in accountNames) text = AccountExtractor.nameRegex(name).replace(text, " ")
        text = AMOUNT_WORDS.replace(text, " ")
        return text.replace(Regex("""[.,;!?]"""), " ").replace(Regex("""\s+"""), " ").trim()
    }

    private fun takeWords(phrase: String, languageCode: String): List<String> {
        val stopwords = if (languageCode == "fr") STOPWORDS_FR else STOPWORDS
        return phrase.split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .dropWhile { it.lowercase() in ARTICLES }
            .filterNot { it.lowercase() in stopwords }
            .take(MAX_WORDS)
    }

    private fun titleCase(words: List<String>): String? {
        if (words.isEmpty()) return null
        return words.joinToString(" ") { word ->
            // Leave words that already carry capitals alone ("iPhone", "7-Eleven", "BTS").
            if (word.any { it.isUpperCase() }) word else word.replaceFirstChar { it.uppercaseChar() }
        }
    }
}
