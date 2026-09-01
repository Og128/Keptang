package com.keptang.ui.common

import com.keptang.core.Defaults
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

fun formatMoney(amountMinorUnits: Long, currencyCode: String): String {
    val major = amountMinorUnits / 100.0
    val symbol = if (currencyCode == "THB") "฿" else "$currencyCode "
    return "$symbol${String.format(Locale.US, "%,.2f", major)}"
}

/** Parses a user-typed major-unit amount (e.g. "150.50") into minor units for [currencyCode]. Invalid or non-positive input yields null. */
fun parseMoneyInput(majorAmountText: String, currencyCode: String): Long? {
    val major = majorAmountText.trim().toDoubleOrNull() ?: return null
    if (major <= 0.0) return null
    val exponent = Defaults.minorUnitExponent(currencyCode)
    return (major * 10.0.pow(exponent)).roundToLong()
}

private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.US)

fun formatDateTime(epochMillis: Long, timeZoneId: String): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of(timeZoneId)).format(dateTimeFormatter)
