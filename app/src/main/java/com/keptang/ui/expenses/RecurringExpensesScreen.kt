package com.keptang.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.keptang.data.db.BudgetPeriodType
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.RecurringExpenseEntity
import com.keptang.ui.budgets.weekdayLabelRes
import com.keptang.ui.common.formatMoney
import com.keptang.ui.theme.CategoryColors

@Composable
fun RecurringExpensesSection(
    onEditRecurring: (String) -> Unit,
    viewModel: RecurringExpensesViewModel = viewModel(factory = RecurringExpensesViewModel.Factory)
) {
    val recurringExpenses by viewModel.recurringExpenses.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val categoriesByName by viewModel.categoriesByName.collectAsStateWithLifecycle()

    if (recurringExpenses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.recurring_empty), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(32.dp))
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        items(recurringExpenses, key = { it.id }) { recurring ->
            RecurringExpenseRow(
                recurring,
                categoriesByName[recurring.category],
                settings.currencyCode,
                onClick = { onEditRecurring(recurring.id) }
            )
        }
    }
}

@Composable
private fun RecurringExpenseRow(recurring: RecurringExpenseEntity, category: CategoryEntity?, currencyCode: String, onClick: () -> Unit) {
    val color = category?.let { CategoryColors.parse(it.colorHex) } ?: MaterialTheme.colorScheme.outline
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBadge(category, color, isRecurring = true)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(recurring.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${recurring.category} · ${recurringFrequencyLabel(recurring)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            formatMoney(recurring.amountMinorUnits, currencyCode),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

/** Shared with [ExpenseCard]'s subtitle, so a generated expense's frequency reads the same as its recurring definition's. */
@Composable
internal fun recurringFrequencyLabel(recurring: RecurringExpenseEntity): String = when (recurring.periodType) {
    BudgetPeriodType.MONTHLY -> stringResource(R.string.recurring_frequency_monthly, recurring.periodAnchor)
    BudgetPeriodType.WEEKLY -> stringResource(R.string.recurring_frequency_weekly, stringResource(weekdayLabelRes(recurring.periodAnchor)))
}
