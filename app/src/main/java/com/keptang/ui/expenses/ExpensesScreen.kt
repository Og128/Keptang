package com.keptang.ui.expenses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.ExpenseEntity
import com.keptang.ui.common.formatDateHeader
import com.keptang.ui.common.formatDateTime
import com.keptang.ui.common.formatMoney
import com.keptang.ui.common.localDateOf
import com.keptang.ui.theme.CategoryColors
import com.keptang.ui.theme.CategoryIcons
import java.time.LocalDate
import java.time.ZoneId

private enum class ExpensesViewMode { LIST, CALENDAR }

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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                onAddExpense(if (viewMode == ExpensesViewMode.CALENDAR) selectedCalendarDate else null)
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.manual_add_title))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                if (viewMode == ExpensesViewMode.LIST) {
                    Button(onClick = {}, enabled = false, modifier = Modifier.padding(end = 8.dp)) {
                        Text(stringResource(R.string.expenses_view_list))
                    }
                } else {
                    OutlinedButton(onClick = { viewMode = ExpensesViewMode.LIST }, modifier = Modifier.padding(end = 8.dp)) {
                        Text(stringResource(R.string.expenses_view_list))
                    }
                }
                if (viewMode == ExpensesViewMode.CALENDAR) {
                    Button(onClick = {}, enabled = false) {
                        Text(stringResource(R.string.expenses_view_calendar))
                    }
                } else {
                    OutlinedButton(onClick = { viewMode = ExpensesViewMode.CALENDAR }) {
                        Text(stringResource(R.string.expenses_view_calendar))
                    }
                }
            }

            when (viewMode) {
                ExpensesViewMode.LIST -> {
                    if (expenses.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.expenses_empty), style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        ExpensesLedger(expenses, categoriesByName, timeZoneId, onEditExpense)
                    }
                }
                ExpensesViewMode.CALENDAR -> ExpenseCalendarView(
                    expenses,
                    categoriesByName,
                    selectedCalendarDate,
                    onDateSelected = { selectedCalendarDate = it },
                    onEditExpense = onEditExpense
                )
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

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        grouped.forEach { (date, dayExpenses) ->
            item(key = "header_$date") {
                Text(
                    formatDateHeader(date, today, todayLabel, yesterdayLabel),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
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
internal fun ExpenseCard(expense: ExpenseEntity, category: CategoryEntity?, onClick: () -> Unit) {
    val tint = category?.let { CategoryColors.parse(it.colorHex).copy(alpha = 0.15f) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = if (tint != null) CardDefaults.cardColors(containerColor = tint) else CardDefaults.cardColors()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (category != null) {
                Icon(
                    CategoryIcons.iconFor(category.iconKey),
                    contentDescription = null,
                    tint = CategoryColors.parse(category.colorHex),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${expense.category} · ${expense.merchant ?: stringResource(R.string.expenses_merchant_placeholder)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    formatDateTime(expense.occurredAtEpochMillis, expense.timeZoneId) +
                        (expense.account?.let { " · $it" } ?: "") +
                        (expense.paymentMethod?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                formatMoney(expense.amountMinorUnits, expense.currencyCode),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.End
            )
        }
    }
}
