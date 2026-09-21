package com.keptang.ui.common

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.keptang.ui.theme.TabularFigures

/**
 * An amount, set the way this app sets every amount: tabular figures so a column of them lines up
 * and nothing jitters as values change, and the currency mark at 60% of the figures' size, lifted
 * off the baseline. A ฿ set at full size next to a display-scale total reads as a seventh digit.
 */
@Composable
fun MoneyText(
    amountMinorUnits: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    animate: Boolean = false
) {
    // The count-up runs on the value, not on a formatted string, so the figures stay legal money
    // at every frame of the animation rather than briefly showing a number nobody has.
    val shown by animateIntAsState(
        targetValue = amountMinorUnits.toInt(),
        animationSpec = tween(durationMillis = if (animate) COUNT_UP_MILLIS else 0),
        label = "money"
    )
    val text = remember(shown, currencyCode) {
        splitCurrency(formatMoney(shown.toLong(), currencyCode))
    }
    Text(text, modifier = modifier, style = style.merge(TabularFigures))
}

private const val COUNT_UP_MILLIS = 650

private val MARK_SIZE: TextUnit = 0.6.em

/**
 * [formatMoney] returns the mark and the figures as one string, so the mark is found by taking
 * everything before the first digit - which also covers the "USD 12.00" form, where the code and
 * its space are the mark.
 */
private fun splitCurrency(formatted: String): AnnotatedString {
    val firstDigit = formatted.indexOfFirst { it.isDigit() }
    if (firstDigit <= 0) return AnnotatedString(formatted)
    return buildAnnotatedString {
        withStyle(SpanStyle(fontSize = MARK_SIZE, baselineShift = BaselineShift(0.35f), fontWeight = FontWeight.Medium)) {
            append(formatted.substring(0, firstDigit))
        }
        append(formatted.substring(firstDigit))
    }
}
