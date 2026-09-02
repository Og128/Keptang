package com.keptang.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keptang.R
import com.keptang.data.db.ExpenseEntity
import com.keptang.ui.common.formatMoney
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WEEKDAY_HEADER_LABELS = listOf(
    R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
    R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat, R.string.weekday_sun
)

private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

@Composable
fun ExpenseCalendarView(expenses: List<ExpenseEntity>) {
    val today = remember { LocalDate.now() }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val byDay = remember(expenses) { expensesByDay(expenses) }
    val cells = remember(displayedMonth) { monthGridDays(displayedMonth) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(displayedMonth.format(monthYearFormatter), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    displayedMonth = YearMonth.from(today)
                    selectedDate = today
                }) {
                    Text(stringResource(R.string.calendar_today_action))
                }
            }
            IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        Row(Modifier.fillMaxWidth()) {
            WEEKDAY_HEADER_LABELS.forEach { labelRes ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            items(cells, key = { it.toString() }) { date ->
                DayCell(
                    date = date,
                    inCurrentMonth = YearMonth.from(date) == displayedMonth,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    dayExpenses = byDay[date].orEmpty(),
                    onClick = { selectedDate = date }
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        selectedDate?.let { date ->
            val dayExpenses = byDay[date].orEmpty()
            if (dayExpenses.isEmpty()) {
                Text(stringResource(R.string.calendar_day_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                Column {
                    dayExpenses.forEach { expense -> ExpenseCard(expense) }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    dayExpenses: List<ExpenseEntity>,
    onClick: () -> Unit
) {
    val totalsByCurrency = remember(dayExpenses) {
        dayExpenses.groupBy { it.currencyCode }.mapValues { (_, list) -> list.sumOf { it.amountMinorUnits } }
    }
    val contentAlpha = if (inCurrentMonth) 1f else 0.4f
    val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val borderModifier = if (isToday) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Column(
        Modifier
            .aspectRatio(0.8f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(borderModifier)
            .background(background)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
        )
        totalsByCurrency.forEach { (currency, total) ->
            Text(
                formatMoney(total, currency),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Every date shown in the grid for [month]: full Monday-start weeks, including the leading/trailing days from adjacent months needed to fill whole rows. */
private fun monthGridDays(month: YearMonth): List<LocalDate> {
    val firstOfMonth = month.atDay(1)
    val gridStart = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())
    val lastOfMonth = month.atEndOfMonth()
    val trailingDays = (7 - lastOfMonth.dayOfWeek.value) % 7
    val gridEnd = lastOfMonth.plusDays(trailingDays.toLong())

    val days = mutableListOf<LocalDate>()
    var current = gridStart
    while (!current.isAfter(gridEnd)) {
        days.add(current)
        current = current.plusDays(1)
    }
    return days
}

private fun expensesByDay(expenses: List<ExpenseEntity>): Map<LocalDate, List<ExpenseEntity>> =
    expenses.groupBy { expense ->
        Instant.ofEpochMilli(expense.occurredAtEpochMillis).atZone(ZoneId.of(expense.timeZoneId)).toLocalDate()
    }
