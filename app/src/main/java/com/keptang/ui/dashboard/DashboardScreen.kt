package com.keptang.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.budget.BudgetSnapshot
import com.keptang.dashboard.DashboardFilter
import com.keptang.ui.common.formatCurrencyExclusionNotice
import com.keptang.ui.common.formatMoney
import com.keptang.ui.common.formatPeriodRange
import com.keptang.ui.expenses.ExpenseCard
import com.keptang.ui.theme.CategoryColors
import java.time.LocalDate

@Composable
fun DashboardScreen(
    onOpenBudgets: () -> Unit = {},
    onOpenReview: () -> Unit = {},
    onEditExpense: (String) -> Unit = {},
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val budgetSnapshot by viewModel.budgetSnapshot.collectAsStateWithLifecycle()
    val reviewCount by viewModel.reviewCount.collectAsStateWithLifecycle()
    val recentExpenses by viewModel.recentExpenses.collectAsStateWithLifecycle()
    val categoryColors = categories.associate { it.name to CategoryColors.parse(it.colorHex) }
    val categoriesByName = categories.associateBy { it.name }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.nav_dashboard), style = MaterialTheme.typography.headlineSmall)

        if (reviewCount > 0) {
            ReviewCard(reviewCount, onOpenReview)
        }

        Text(
            stringResource(R.string.dashboard_spending_label),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp)
        )

        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            FilterButton(
                label = stringResource(R.string.dashboard_filter_today),
                selected = currentFilter is DashboardFilter.Today,
                onClick = { viewModel.setFilter(DashboardFilter.Today) },
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            )
            FilterButton(
                label = stringResource(R.string.dashboard_filter_7d),
                selected = currentFilter is DashboardFilter.Last7Days,
                onClick = { viewModel.setFilter(DashboardFilter.Last7Days) },
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            FilterButton(
                label = stringResource(R.string.dashboard_filter_30d),
                selected = currentFilter is DashboardFilter.Last30Days,
                onClick = { viewModel.setFilter(DashboardFilter.Last30Days) },
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            )
            FilterButton(
                label = stringResource(R.string.dashboard_filter_period),
                selected = currentFilter is DashboardFilter.Period,
                onClick = {
                    val today = LocalDate.now()
                    viewModel.setFilter(DashboardFilter.Period(today.minusDays(6), today))
                },
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        val periodFilter = currentFilter as? DashboardFilter.Period
        if (periodFilter != null) {
            PeriodPicker(periodFilter, onChange = viewModel::setFilter)
        }

        Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.dashboard_total_label), style = MaterialTheme.typography.labelLarge)
                Text(formatMoney(snapshot.totalMinorUnits, snapshot.defaultCurrencyCode), style = MaterialTheme.typography.headlineMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.dashboard_transactions_label), style = MaterialTheme.typography.labelLarge)
                Text(snapshot.transactionCount.toString(), style = MaterialTheme.typography.headlineMedium)
            }
        }
        formatCurrencyExclusionNotice(snapshot.excludedByCurrency)?.let { notice ->
            Text(notice, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }

        if (snapshot.byCategory.isEmpty()) {
            Text(
                stringResource(R.string.dashboard_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            Column(Modifier.padding(top = 16.dp)) {
                snapshot.byCategory.forEach { spend ->
                    CategoryBarRow(
                        label = spend.category,
                        color = categoryColors[spend.category] ?: MaterialTheme.colorScheme.outline,
                        amountMinorUnits = spend.spentMinorUnits,
                        fractionOfTotal = if (snapshot.totalMinorUnits > 0) {
                            spend.spentMinorUnits.toFloat() / snapshot.totalMinorUnits.toFloat()
                        } else 0f,
                        currencyCode = snapshot.defaultCurrencyCode
                    )
                }
            }
        }

        Text(
            stringResource(R.string.dashboard_budget_label),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 28.dp)
        )
        BudgetSection(budgetSnapshot = budgetSnapshot, onOpenBudgets = onOpenBudgets)

        if (recentExpenses.isNotEmpty()) {
            Text(
                stringResource(R.string.dashboard_recent_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 28.dp)
            )
            Column(Modifier.padding(top = 8.dp)) {
                recentExpenses.forEach { expense ->
                    ExpenseCard(expense, categoriesByName[expense.category], onClick = { onEditExpense(expense.id) })
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(reviewCount: Int, onOpenReview: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                stringResource(R.string.dashboard_review_count, reviewCount),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            Button(onClick = onOpenReview) {
                Text(stringResource(R.string.dashboard_review_action))
            }
        }
    }
}

@Composable
private fun BudgetSection(budgetSnapshot: BudgetSnapshot, onOpenBudgets: () -> Unit) {
    val overall = budgetSnapshot.overall
    if (overall == null && budgetSnapshot.categories.isEmpty()) {
        Column(Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.dashboard_budget_empty), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onOpenBudgets, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.dashboard_budget_setup_action))
            }
        }
        return
    }

    Column(Modifier.padding(top = 8.dp)) {
        overall?.let { standing ->
            CategoryBarRow(
                label = stringResource(R.string.budgets_overall_label),
                color = MaterialTheme.colorScheme.primary,
                amountMinorUnits = standing.spentMinorUnits,
                fractionOfTotal = if (standing.budget.amountMinorUnits > 0L) {
                    (standing.spentMinorUnits.toFloat() / standing.budget.amountMinorUnits.toFloat()).coerceIn(0f, 1f)
                } else 0f,
                currencyCode = budgetSnapshot.defaultCurrencyCode,
                totalLabel = formatMoney(standing.budget.amountMinorUnits, budgetSnapshot.defaultCurrencyCode)
            )
            Text(
                formatPeriodRange(standing.periodStart, standing.periodEndExclusive),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        budgetSnapshot.categories.forEach { standing ->
            CategoryBarRow(
                label = standing.budget.category.orEmpty(),
                color = MaterialTheme.colorScheme.primary,
                amountMinorUnits = standing.spentMinorUnits,
                fractionOfTotal = if (standing.budget.amountMinorUnits > 0L) {
                    (standing.spentMinorUnits.toFloat() / standing.budget.amountMinorUnits.toFloat()).coerceIn(0f, 1f)
                } else 0f,
                currencyCode = budgetSnapshot.defaultCurrencyCode,
                totalLabel = formatMoney(standing.budget.amountMinorUnits, budgetSnapshot.defaultCurrencyCode)
            )
        }
        TextButton(onClick = onOpenBudgets, modifier = Modifier.padding(top = 4.dp)) {
            Text(stringResource(R.string.dashboard_budget_view_all))
        }
    }
}

/** A category/budget name, a colored progress bar, and its amount - used for both the Spending and Budget breakdowns. */
@Composable
private fun CategoryBarRow(
    label: String,
    color: Color,
    amountMinorUnits: Long,
    fractionOfTotal: Float,
    currencyCode: String,
    totalLabel: String? = null
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
            Text(
                if (totalLabel != null) "${formatMoney(amountMinorUnits, currencyCode)} / $totalLabel" else formatMoney(amountMinorUnits, currencyCode),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        LinearProgressIndicator(
            progress = { fractionOfTotal.coerceIn(0f, 1f) },
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Button(onClick = {}, enabled = false, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Composable
private fun PeriodPicker(filter: DashboardFilter.Period, onChange: (DashboardFilter.Period) -> Unit) {
    var startText by remember(filter.start) { mutableStateOf(filter.start.toString()) }
    var endText by remember(filter.endInclusive) { mutableStateOf(filter.endInclusive.toString()) }

    Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        OutlinedTextField(
            value = startText,
            onValueChange = { text ->
                startText = text
                runCatching { LocalDate.parse(text) }.getOrNull()?.let { onChange(filter.copy(start = it)) }
            },
            label = { Text(stringResource(R.string.dashboard_period_start_label)) },
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        OutlinedTextField(
            value = endText,
            onValueChange = { text ->
                endText = text
                runCatching { LocalDate.parse(text) }.getOrNull()?.let { onChange(filter.copy(endInclusive = it)) }
            },
            label = { Text(stringResource(R.string.dashboard_period_end_label)) },
            modifier = Modifier.weight(1f)
        )
    }
}
