package com.keptang.parser

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Deterministic, rule-based transcript -> expenses parser. No Android dependencies, so it can
 * be exercised by plain JVM unit tests. See [ExpenseParserTest] for the required examples.
 *
 * Splitting strategy: a transcript is first split into clauses on ", and " / " and " / ",".
 * A date phrase ("today", "yesterday", or an explicit date) updates a running "current date"
 * that carries forward onto later clauses that don't mention a date of their own - this is how
 * "yesterday night I paid 550 baht for dinner and 25 baht for coffee" ends up dating the coffee
 * clause to yesterday as well. "today"/"yesterday" are always resolved against the true capture
 * instant (see [DateExpressions]), never against whatever the running date currently holds.
 *
 * A clause only becomes a [ParsedExpense] if an amount can be found in it - amounts, dates,
 * accounts, and payment methods are never invented.
 */
class ExpenseParser {

    private val commaAndRegex = Regex(""",\s*and\s+""", RegexOption.IGNORE_CASE)
    private val andRegex = Regex("""\s+and\s+""", RegexOption.IGNORE_CASE)
    private val commaAndRegexFr = Regex(""",\s*et\s+""", RegexOption.IGNORE_CASE)
    private val andRegexFr = Regex("""\s+et\s+""", RegexOption.IGNORE_CASE)
    private val commaRegex = Regex(""",\s*""")
    private val trimChars = charArrayOf('.', ',', ';', ' ', '\t', '\n')

    fun parse(
        transcript: String,
        captureId: String,
        referenceDateTime: ZonedDateTime,
        languageCode: String = "en",
        vocabulary: CategoryVocabulary = CategoryVocabulary.EMPTY,
        /** The names of the accounts that exist, so a bare "on HSBC" can be recognised. See [AccountExtractor]. */
        accountNames: List<String> = emptyList()
    ): List<ParsedExpense> {
        val segments = splitIntoClauses(transcript, languageCode)
        val today: LocalDate = referenceDateTime.toLocalDate()
        val yesterday: LocalDate = today.minusDays(1)

        var currentDate = today
        val expenses = mutableListOf<ParsedExpense>()

        for (rawSegment in segments) {
            val segment = rawSegment.trim(*trimChars)
            if (segment.isBlank()) continue

            val explicitDate = DateExpressions.extractExplicitDate(segment, referenceDateTime.year, languageCode)
            currentDate = when {
                explicitDate != null -> explicitDate
                DateExpressions.isYesterday(segment, languageCode) -> yesterday
                DateExpressions.isToday(segment, languageCode) -> today
                else -> currentDate
            }

            val amount = AmountExtractor.extract(segment, languageCode) ?: continue

            val category = CategoryRules.classify(segment, languageCode, vocabulary)
            val account = AccountExtractor.extract(segment, languageCode, accountNames)
            val paymentMethod = PaymentMethodExtractor.extract(segment, languageCode)
            val description = DescriptionExtractor.extract(segment, languageCode, accountNames)
            val confidence = ConfidenceScorer.score(hasCategory = category != null)

            val occurredAt = currentDate
                .atTime(referenceDateTime.toLocalTime())
                .atZone(referenceDateTime.zone)

            expenses += ParsedExpense(
                captureId = captureId,
                amountMinorUnits = amount.minorUnits,
                currencyCode = amount.currencyCode,
                occurredAt = occurredAt,
                category = category ?: UNCATEGORIZED,
                account = account,
                paymentMethod = paymentMethod,
                description = description,
                confidence = confidence,
                needsReview = confidence < ConfidenceScorer.AUTO_APPROVE_THRESHOLD
            )
        }

        return expenses
    }

    private fun splitIntoClauses(transcript: String, languageCode: String): List<String> {
        var text = transcript
        if (languageCode == "fr") {
            text = commaAndRegexFr.replace(text, " ~SPLIT~ ")
            text = andRegexFr.replace(text, " ~SPLIT~ ")
        } else {
            text = commaAndRegex.replace(text, " ~SPLIT~ ")
            text = andRegex.replace(text, " ~SPLIT~ ")
        }
        text = commaRegex.replace(text, " ~SPLIT~ ")
        val fragments = text.split("~SPLIT~")
            .map { it.trim(*trimChars) }
            .filter { it.isNotEmpty() }
        return mergeFragmentsWithoutTheirOwnAmount(fragments, languageCode)
    }

    /**
     * Re-joins fragments that a comma split apart but that describe a single expense.
     *
     * Speech recognisers punctuate on their own, so "Lunch 50 baht" is just as likely to arrive as
     * "Lunch, 50 baht". Splitting that on the comma produced "Lunch" (dropped outright - no
     * amount) plus "50 baht" (an amount with nothing describing it), which is how a perfectly
     * clear sentence ended up as an uncategorised expense with no description.
     *
     * A fragment only starts a new expense if it carries an amount of its own; otherwise it is
     * glued onto its neighbour. "550 baht for dinner, 25 baht for coffee" still splits in two,
     * because both halves name an amount.
     */
    private fun mergeFragmentsWithoutTheirOwnAmount(fragments: List<String>, languageCode: String): List<String> {
        val merged = mutableListOf<String>()
        for (fragment in fragments) {
            val hasAmount = AmountExtractor.extract(fragment, languageCode) != null
            val previous = merged.lastOrNull()
            val previousHasAmount = previous != null && AmountExtractor.extract(previous, languageCode) != null
            when {
                merged.isEmpty() -> merged += fragment
                // An amount-less fragment can only describe its neighbour, never stand alone.
                !hasAmount -> merged[merged.lastIndex] = "$previous $fragment"
                // An amount whose neighbour has none completes that neighbour ("Lunch" + "50 baht").
                !previousHasAmount -> merged[merged.lastIndex] = "$previous $fragment"
                else -> merged += fragment
            }
        }
        return merged
    }
}
