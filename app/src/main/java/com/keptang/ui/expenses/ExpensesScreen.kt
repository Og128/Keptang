package com.keptang.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.ExpenseEntity
import com.keptang.ui.common.formatDateHeader
import com.keptang.ui.common.formatMoney
import com.keptang.ui.common.localDateOf
import com.keptang.ui.theme.CategoryColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ExpensesViewMode { LIST, CALENDAR }

private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

@Composable
fun ExpensesScreen(
    onAddExpense: (LocalDate?) -> Unit,
    onEditExpense: (String) -> Unit,
    viewModel: ExpensesViewModel = viewModel(factory = ExpensesViewModel.Factory)
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val categoriesByName by viewModel.categoriesByName.collectAsStateWithLifecycle()
    val timeZoneId by viewModel.timeZoneId.collectAsStateWithLifecycle()
    var viewMode by remember { mutableStateOf(ExpensesViewMode.LIST) }
    var selectedCalendarDate by remember { mutableStateOf<LocalDate?>(null) }
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.nav_expenses),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                viewMode = if (viewMode == ExpensesViewMode.LIST) ExpensesViewMode.CALENDAR else ExpensesViewMode.LIST
            }) {
                if (viewMode == ExpensesViewMode.LIST) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.expenses_view_calendar))
                } else {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.expenses_view_list))
                }
            }
        }

        when (viewMode) {
            ExpensesViewMode.LIST -> {
                MonthNavigator(
                    displayedMonth = displayedMonth,
                    onPrevious = { displayedMonth = displayedMonth.minusMonths(1) },
                    onNext = { displayedMonth = displayedMonth.plusMonths(1) },
                    onReset = { displayedMonth = YearMonth.now() }
                )
                val monthExpenses = expenses.filter {
                    YearMonth.from(localDateOf(it.occurredAtEpochMillis, it.timeZoneId)) == displayedMonth
                }
                when {
                    expenses.isEmpty() -> EmptyState(stringResource(R.string.expenses_empty))
                    monthExpenses.isEmpty() -> EmptyState(stringResource(R.string.expenses_empty_period))
                    else -> ExpensesLedger(monthExpenses, categoriesByName, timeZoneId, onEditExpense)
                }
            }
            ExpensesViewMode.CALENDAR -> ExpenseCalendarView(
                expenses,
                categoriesByName,
                selectedCalendarDate,
                onDateSelected = { selectedCalendarDate = it },
                onEditExpense = onEditExpense,
                onAddExpense = onAddExpense
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MonthNavigator(
    displayedMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable(onClick = onReset)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                displayedMonth.format(monthYearFormatter),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.calendar_previous_month))
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.calendar_next_month))
            }
        }
    }
}

@Composable
private fun ExpensesLedger(
    expenses: List<ExpenseEntity>,
    categoriesByName: Map<String, CategoryEntity>,
    timeZoneId: String,
    onEditExpense: (String) -> Unit
) {
    val today = LocalDate.now(ZoneId.of(timeZoneId))
    val todayLabel = stringResource(R.string.calendar_today_action)
    val yesterdayLabel = stringResource(R.string.ledger_date_yesterday)
    val grouped = expenses.groupBy { localDateOf(it.occurredAtEpochMillis, it.timeZoneId) }
        .toSortedMap(compareByDescending { it })

    LazyColumn(Modifier.fillMaxSize()) {
        grouped.forEach { (date, dayExpenses) ->
            item(key = "header_$date") {
                DateHeaderBar(formatDateHeader(date, today, todayLabel, yesterdayLabel))
            }
            for (expense in dayExpenses) {
                item(key = expense.id) {
                    ExpenseCard(expense, categoriesByName[expense.category], onClick = { onEditExpense(expense.id) })
                }
            }
        }
    }
}

@Composable
private fun DateHeaderBar(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
internal fun CategoryTag(category: CategoryEntity?) {
    val color = category?.let { CategoryColors.parse(it.colorHex) } ?: MaterialTheme.colorScheme.outline
    val label = category?.name ?: stringResource(R.string.manual_add_default_category)
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
internal fun ExpenseCard(expense: ExpenseEntity, category: CategoryEntity?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    expense.merchant?.takeIf { it.isNotBlank() } ?: stringResource(R.string.expenses_merchant_placeholder),
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryTag(category)
                    val extra = listOfNotNull(expense.account, expense.paymentMethod).joinToString(" · ")
                    if (extra.isNotBlank()) {
                        Text(
                            " · $extra",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                "-" + formatMoney(expense.amountMinorUnits, expense.currencyCode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}
