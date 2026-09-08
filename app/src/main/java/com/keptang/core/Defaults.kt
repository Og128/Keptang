package com.keptang.core

object Defaults {
    const val CURRENCY_CODE = "THB"
    const val TIME_ZONE_ID = "Asia/Bangkok"
    const val AUDIO_RETENTION_DAYS = 7

    /** The only audio retention lengths offered in Settings. */
    val AUDIO_RETENTION_OPTIONS = listOf(1, 3, 7)
    const val DEFAULT_ACCOUNT = "Cash"
    const val LANGUAGE_CODE = "en"

    /**
     * A curated list rather than every ISO 4217 code - same approach as [com.keptang.ui.theme.CategoryColors]
     * and [com.keptang.ui.theme.CategoryIcons]: a short, pickable set beats a free-text field or an
     * unfilterable list of hundreds. [CURRENCY_CODE] is always included.
     */
    val CURRENCY_OPTIONS = listOf(
        "THB", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF",
        "CNY", "SGD", "HKD", "INR", "KRW", "VND", "PHP", "IDR", "MYR", "NZD"
    )

    /** A curated set of common IANA zone IDs, not the full ~600-entry system list. [TIME_ZONE_ID] is always included. */
    val TIME_ZONE_OPTIONS = listOf(
        "UTC",
        "America/Los_Angeles", "America/Denver", "America/Chicago", "America/New_York", "America/Sao_Paulo",
        "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Moscow",
        "Africa/Cairo", "Africa/Johannesburg",
        "Asia/Dubai", "Asia/Karachi", "Asia/Kolkata", "Asia/Dhaka", "Asia/Bangkok", "Asia/Jakarta", "Asia/Ho_Chi_Minh",
        "Asia/Shanghai", "Asia/Singapore", "Asia/Hong_Kong", "Asia/Tokyo", "Asia/Seoul",
        "Australia/Sydney", "Australia/Perth", "Pacific/Auckland"
    )

    /** THB (and most currencies covered by this prototype) use 2 minor-unit decimal places. */
    fun minorUnitExponent(currencyCode: String): Int = when (currencyCode.uppercase()) {
        "THB", "USD", "EUR", "GBP" -> 2
        "JPY" -> 0
        else -> 2
    }
}
