package com.keptang.ui.expenses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.ui.common.formatDateTime
import com.keptang.ui.common.formatMoney

@Composable
fun ExpensesScreen(viewModel: ExpensesViewModel = viewModel(factory = ExpensesViewModel.Factory)) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()

    if (expenses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.expenses_empty), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        items(expenses, key = { it.id }) { expense ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        formatMoney(expense.amountMinorUnits, expense.currencyCode),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${expense.category} · ${expense.merchant ?: "-"}",
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
    }
}
