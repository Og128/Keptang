package com.keptang.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The fixed set of category colors offered by the color picker. A validated categorical
 * palette (CVD-safe adjacent pairs, contrast-checked) rather than a free-form picker, so any
 * combination a user picks stays legible and distinguishable.
 */
object CategoryColors {
    const val BLUE = "#2a78d6"
    const val ORANGE = "#eb6834"
    const val AQUA = "#1baf7a"
    const val YELLOW = "#eda100"
    const val MAGENTA = "#e87ba4"
    const val GREEN = "#008300"
    const val VIOLET = "#4a3aa7"
    const val RED = "#e34948"

    val PALETTE = listOf(BLUE, ORANGE, AQUA, YELLOW, MAGENTA, GREEN, VIOLET, RED)

    fun parse(hex: String): Color = Color(android.graphics.Color.parseColor(hex))
}
