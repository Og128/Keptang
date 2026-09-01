package com.keptang.parser

/**
 * Turns clause-extraction outcomes into a 0..1 confidence score. An amount is mandatory to
 * even produce a [ParsedExpense] (never invent an amount) - once one is found, confidence
 * only affects whether the expense is auto-approved or routed to review.
 */
object ConfidenceScorer {

    const val AUTO_APPROVE_THRESHOLD = 0.6f

    fun score(hasCategory: Boolean): Float {
        var score = 0.6f
        if (hasCategory) score += 0.4f
        return score.coerceAtMost(1f)
    }
}
