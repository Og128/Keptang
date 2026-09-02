package com.keptang.ui.expenses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.data.db.ExpenseEntity
import com.keptang.ui.common.formatDateTime
import com.keptang.ui.common.formatMoney

private enum class ExpensesViewMode { LIST, CALENDAR }

@Composable
fun ExpensesScreen(
    onAddExpense: () -> Unit,
    viewModel: ExpensesViewModel = viewModel(factory = ExpensesViewModel.Factory)
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    var viewMode by remember { mutableStateOf(ExpensesViewMode.LIST) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
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
                        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                            items(expenses, key = { it.id }) { expense -> ExpenseCard(expense) }
                        }
                    }
                }
                ExpensesViewMode.CALENDAR -> ExpenseCalendarView(expenses)
            }
        }
    }
}

@Composable
internal fun ExpenseCard(expense: ExpenseEntity) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                formatMoney(expense.amountMinorUnits, expense.currencyCode),
                style = MaterialTheme.typography.titleMedium
            )
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
    }
}
