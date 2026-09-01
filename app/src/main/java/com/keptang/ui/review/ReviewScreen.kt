package com.keptang.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.keptang.ui.common.formatMoney

@Composable
fun ReviewScreen(viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory)) {
    val expenses by viewModel.needsReview.collectAsStateWithLifecycle()

    if (expenses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.review_empty), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        items(expenses, key = { it.id }) { expense ->
            ReviewCard(
                expense = expense,
                onApprove = { edited -> viewModel.update(edited); viewModel.approve(edited.id) },
                onReject = { viewModel.reject(expense.id) }
            )
        }
    }
}

@Composable
private fun ReviewCard(expense: ExpenseEntity, onApprove: (ExpenseEntity) -> Unit, onReject: () -> Unit) {
    var amountMajorText by remember(expense.id) { mutableStateOf((expense.amountMinorUnits / 100.0).toString()) }
    var category by remember(expense.id) { mutableStateOf(expense.category) }

    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Originally heard: ${formatMoney(expense.amountMinorUnits, expense.currencyCode)}",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = amountMajorText,
                onValueChange = { amountMajorText = it },
                label = { Text("Amount (${expense.currencyCode})") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onReject, modifier = Modifier.padding(end = 8.dp)) {
                    Text(stringResource(R.string.action_delete))
                }
                Button(onClick = {
                    val minorUnits = ((amountMajorText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    onApprove(expense.copy(amountMinorUnits = minorUnits, category = category))
                }) {
                    Text(stringResource(R.string.action_approve))
                }
            }
        }
    }
}
