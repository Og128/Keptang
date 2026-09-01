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
    private val commaRegex = Regex(""",\s*""")
    private val trimChars = charArrayOf('.', ',', ';', ' ', '\t', '\n')

    fun parse(transcript: String, captureId: String, referenceDateTime: ZonedDateTime): List<ParsedExpense> {
        val segments = splitIntoClauses(transcript)
        val today: LocalDate = referenceDateTime.toLocalDate()
        val yesterday: LocalDate = today.minusDays(1)

        var currentDate = today
        val expenses = mutableListOf<ParsedExpense>()

        for (rawSegment in segments) {
            val segment = rawSegment.trim(*trimChars)
            if (segment.isBlank()) continue

            val explicitDate = DateExpressions.extractExplicitDate(segment, referenceDateTime.year)
            currentDate = when {
                explicitDate != null -> explicitDate
                DateExpressions.isYesterday(segment) -> yesterday
                DateExpressions.isToday(segment) -> today
                else -> currentDate
            }

            val amount = AmountExtractor.extract(segment) ?: continue

            val category = CategoryRules.classify(segment)
            val account = AccountExtractor.extract(segment)
            val paymentMethod = PaymentMethodExtractor.extract(segment)
            val merchant = MerchantExtractor.extract(segment)
            val confidence = ConfidenceScorer.score(hasCategory = category != null)

            val occurredAt = currentDate
                .atTime(referenceDateTime.toLocalTime())
                .atZone(referenceDateTime.zone)

            expenses += ParsedExpense(
                captureId = captureId,
                amountMinorUnits = amount.minorUnits,
                currencyCode = amount.currencyCode,
                occurredAt = occurredAt,
                category = category ?: "Uncategorized",
                account = account,
                paymentMethod = paymentMethod,
                merchant = merchant,
                confidence = confidence,
                needsReview = confidence < ConfidenceScorer.AUTO_APPROVE_THRESHOLD
            )
        }

        return expenses
    }

    private fun splitIntoClauses(transcript: String): List<String> {
        var text = transcript
        text = commaAndRegex.replace(text, " ~SPLIT~ ")
        text = andRegex.replace(text, " ~SPLIT~ ")
        text = commaRegex.replace(text, " ~SPLIT~ ")
        return text.split("~SPLIT~")
            .map { it.trim(*trimChars) }
            .filter { it.isNotEmpty() }
    }
}
