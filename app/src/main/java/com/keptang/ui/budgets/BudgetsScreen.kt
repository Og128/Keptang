package com.keptang.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.budget.BudgetStanding
import com.keptang.ui.common.formatCurrencyExclusionNotice
import com.keptang.ui.common.formatMoney
import com.keptang.ui.common.formatPeriodRange

@Composable
fun BudgetsScreen(
    onAddBudget: () -> Unit,
    onEditBudget: (String) -> Unit,
    viewModel: BudgetsViewModel = viewModel(factory = BudgetsViewModel.Factory)
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBudget) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.budget_add_title))
            }
        }
    ) { padding ->
        if (snapshot.overall == null && snapshot.categories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.budgets_empty), style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            snapshot.overall?.let { overall ->
                item {
                    BudgetCard(
                        title = stringResource(R.string.budgets_overall_label),
                        standing = overall,
                        currencyCode = snapshot.defaultCurrencyCode,
                        onClick = { onEditBudget(overall.budget.id) }
                    )
                }
            }
            snapshot.other?.let { other ->
                item {
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.budgets_other_label), style = MaterialTheme.typography.titleMedium)
                            Text(
                                formatMoney(other.spentMinorUnits, snapshot.defaultCurrencyCode),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                formatPeriodRange(other.periodStart, other.periodEndExclusive),
                                style = MaterialTheme.typography.bodySmall
                            )
                            formatCurrencyExclusionNotice(other.excludedByCurrency)?.let { notice ->
                                Text(notice, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            items(snapshot.categories, key = { it.budget.id }) { standing ->
                BudgetCard(
                    title = standing.budget.category.orEmpty(),
                    standing = standing,
                    currencyCode = snapshot.defaultCurrencyCode,
                    onClick = { onEditBudget(standing.budget.id) }
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(title: String, standing: BudgetStanding, currencyCode: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${formatMoney(standing.spentMinorUnits, currencyCode)} / ${formatMoney(standing.budget.amountMinorUnits, currencyCode)}",
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = {
                    if (standing.budget.amountMinorUnits <= 0L) 0f
                    else (standing.spentMinorUnits.toFloat() / standing.budget.amountMinorUnits.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
            Text(
                formatPeriodRange(standing.periodStart, standing.periodEndExclusive),
                style = MaterialTheme.typography.bodySmall
            )
            formatCurrencyExclusionNotice(standing.excludedByCurrency)?.let { notice ->
                Text(notice, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
