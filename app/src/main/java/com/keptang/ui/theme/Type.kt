package com.keptang.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.keptang.R

/**
 * Bricolage Grotesque, embedded.
 *
 * It was picked on a measured constraint rather than taste: of six candidates, only this and
 * Schibsted Grotesk actually carry ฿ (U+0E3F), which this app prints on every single amount. The
 * other four would have dropped each amount into a system fallback face - the most repeated
 * glyph in the UI, set in a different typeface from everything around it.
 *
 * What kept it over Schibsted is the optical-size axis: display forms at the hero total, calm
 * forms in a list, out of one file. That is the one structural thing a static face cannot do.
 */
private val Bricolage = FontFamily(
    Font(R.font.bricolage_grotesque, FontWeight.Light),
    Font(R.font.bricolage_grotesque, FontWeight.Normal),
    Font(R.font.bricolage_grotesque, FontWeight.Medium),
    Font(R.font.bricolage_grotesque, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque, FontWeight.Bold),
    Font(R.font.bricolage_grotesque, FontWeight.ExtraBold)
)

/**
 * Tabular, decimal-aligned figures. Every amount in the app carries this: a money column whose
 * digits change width jitters as it updates, and the count-up animation on the period total would
 * make that jitter continuous.
 */
val TabularFigures = TextStyle(fontFeatureSettings = "tnum, lnum")

/**
 * Sizes stay on the Material scale so the system font-size setting keeps working - the audit
 * found zero hard-coded text sizes in this codebase and that must not regress. What changes is
 * the face, the weights and the tracking: display sizes run tight and heavy, body stays calm.
 */
val KeptangTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.04).em),
        displayMedium = displayMedium.copy(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em),
        displaySmall = displaySmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
        headlineLarge = headlineLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
        headlineMedium = headlineMedium.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em),
        headlineSmall = headlineSmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge.copy(fontFamily = Bricolage),
        bodyMedium = bodyMedium.copy(fontFamily = Bricolage),
        bodySmall = bodySmall.copy(fontFamily = Bricolage),
        labelLarge = labelLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = Bricolage, fontWeight = FontWeight.Medium),
        labelSmall = labelSmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Medium, letterSpacing = 0.08.em)
    )
}
