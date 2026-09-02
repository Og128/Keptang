package com.keptang.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.keptang.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val periodDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** e.g. "Sep 1 – Sep 30" for the half-open period `[start, endExclusive)`. */
fun formatPeriodRange(start: LocalDate, endExclusive: LocalDate): String =
    "${start.format(periodDateFormatter)} – ${endExclusive.minusDays(1).format(periodDateFormatter)}"

/** e.g. "2 expenses in USD not counted" - null when nothing was excluded. */
@Composable
fun formatCurrencyExclusionNotice(excludedByCurrency: Map<String, Int>): String? {
    if (excludedByCurrency.isEmpty()) return null
    val parts = excludedByCurrency.entries.map { (currency, count) ->
        pluralStringResource(R.plurals.budget_currency_excluded_notice, count, count, currency)
    }
    return parts.joinToString(", ")
}
