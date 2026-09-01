package com.keptang.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatMoney(amountMinorUnits: Long, currencyCode: String): String {
    val major = amountMinorUnits / 100.0
    val symbol = if (currencyCode == "THB") "฿" else "$currencyCode "
    return "$symbol${String.format(Locale.US, "%,.2f", major)}"
}

private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.US)

fun formatDateTime(epochMillis: Long, timeZoneId: String): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of(timeZoneId)).format(dateTimeFormatter)
