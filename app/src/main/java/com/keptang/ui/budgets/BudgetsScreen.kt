package com.keptang.ui.budgets

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.budget.BudgetStanding
import com.keptang.ui.common.formatCurrencyExclusionNotice
import com.keptang.ui.common.formatMoney
import com.keptang.ui.common.formatPeriodRange
import com.keptang.ui.theme.CategoryColors
import java.time.LocalDate

@Composable
fun BudgetsScreen(
    onAddBudget: () -> Unit,
    onEditBudget: (String) -> Unit,
    viewModel: BudgetsViewModel = viewModel(factory = BudgetsViewModel.Factory)
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryColors = categories.associate { it.name to CategoryColors.parse(it.colorHex) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.nav_budgets),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAddBudget) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.budget_add_title))
            }
        }

        val overall = snapshot.overall
        overall?.let { PeriodBar(it.periodStart, it.periodEndExclusive) }

        if (overall == null && snapshot.categories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.budgets_empty), style = MaterialTheme.typography.bodyLarge)
            }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            overall?.let { standing ->
                item { OverviewCard(standing, snapshot.defaultCurrencyCode) }
            }

            item {
                Text(
                    stringResource(R.string.budgets_breakdown_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 4.dp)
                )
                BreakdownHeaderRow()
            }

            snapshot.other?.let { other ->
                item {
                    BreakdownRow(
                        name = stringResource(R.string.budgets_other_label),
                        color = MaterialTheme.colorScheme.outline,
                        budgetedLabel = stringResource(R.string.budgets_no_budget_label),
                        activityMinorUnits = other.spentMinorUnits,
                        availableLabel = "-",
                        availableColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        currencyCode = snapshot.defaultCurrencyCode,
                        onClick = null
                    )
                }
            }

            items(snapshot.categories, key = { it.budget.id }) { standing ->
                val category = standing.budget.category.orEmpty()
                val available = standing.budget.amountMinorUnits - standing.spentMinorUnits
                BreakdownRow(
                    name = category,
                    color = categoryColors[category] ?: MaterialTheme.colorScheme.outline,
                    budgetedLabel = formatMoney(standing.budget.amountMinorUnits, snapshot.defaultCurrencyCode),
                    activityMinorUnits = standing.spentMinorUnits,
                    availableLabel = formatSignedMoney(available, snapshot.defaultCurrencyCode),
                    availableColor = if (available >= 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    currencyCode = snapshot.defaultCurrencyCode,
                    onClick = { onEditBudget(standing.budget.id) }
                )
            }
        }
    }
}

@Composable
private fun PeriodBar(periodStart: LocalDate, periodEndExclusive: LocalDate) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(formatPeriodRange(periodStart, periodEndExclusive), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OverviewCard(overall: BudgetStanding, currencyCode: String) {
    val available = overall.budget.amountMinorUnits - overall.spentMinorUnits

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp)) {
        Text(stringResource(R.string.budgets_overview_label), style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            stringResource(R.string.budgets_current_expenses_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(formatMoney(overall.spentMinorUnits, currencyCode), style = MaterialTheme.typography.titleLarge)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.budgets_you_budgeted_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(formatMoney(overall.budget.amountMinorUnits, currencyCode), style = MaterialTheme.typography.titleLarge)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.budgets_available_label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatSignedMoney(available, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (available >= 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        formatCurrencyExclusionNotice(overall.excludedByCurrency)?.let { notice ->
            Text(notice, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun BreakdownHeaderRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.budgets_column_budgeted),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            stringResource(R.string.budgets_column_activity),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            stringResource(R.string.budgets_column_available),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun BreakdownRow(
    name: String,
    color: Color,
    budgetedLabel: String,
    activityMinorUnits: Long,
    availableLabel: String,
    availableColor: Color,
    currencyCode: String,
    onClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, color = color)
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Spacer(Modifier.weight(1f))
            Text(
                budgetedLabel,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Text(
                formatMoney(activityMinorUnits, currencyCode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Text(
                availableLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = availableColor,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
        HorizontalDivider(Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

private fun formatSignedMoney(amountMinorUnits: Long, currencyCode: String): String =
    if (amountMinorUnits < 0L) "-" + formatMoney(-amountMinorUnits, currencyCode) else formatMoney(amountMinorUnits, currencyCode)
