package com.keptang.core

object Defaults {
    const val CURRENCY_CODE = "THB"
    const val TIME_ZONE_ID = "Asia/Bangkok"
    const val AUDIO_RETENTION_DAYS = 30
    const val DEFAULT_ACCOUNT = "Cash"
    const val LANGUAGE_CODE = "en"
    val PREFERRED_CATEGORIES = listOf("Transport", "Dining", "Coffee", "Groceries", "Housing", "Utilities")

    /** THB (and most currencies covered by this prototype) use 2 minor-unit decimal places. */
    fun minorUnitExponent(currencyCode: String): Int = when (currencyCode.uppercase()) {
        "THB", "USD", "EUR", "GBP" -> 2
        "JPY" -> 0
        else -> 2
    }
}
