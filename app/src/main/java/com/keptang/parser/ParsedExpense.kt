package com.keptang.parser

import java.time.ZonedDateTime
import java.util.UUID

/**
 * Output of [ExpenseParser]. Plain Kotlin, no Android or Room dependencies, so it can be
 * unit-tested on the plain JVM and mapped to a Room entity by the data layer.
 */
/** The category an expense carries when nothing in the vocabulary recognised it. */
const val UNCATEGORIZED = "Uncategorized"

data class ParsedExpense(
    val id: String = UUID.randomUUID().toString(),
    val captureId: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val occurredAt: ZonedDateTime,
    val category: String,
    val account: String?,
    val paymentMethod: String?,
    val description: String?,
    val confidence: Float,
    val needsReview: Boolean
)
