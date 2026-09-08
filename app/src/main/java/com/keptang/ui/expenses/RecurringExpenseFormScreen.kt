package com.keptang.ui.expenses

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.data.db.BudgetPeriodType
import com.keptang.ui.budgets.weekdayLabelRes
import com.keptang.ui.common.formatMoneyInput
import com.keptang.ui.common.parseMoneyInput

@Composable
fun RecurringExpenseFormScreen(
    recurringId: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: RecurringExpenseFormViewModel = viewModel(factory = RecurringExpenseFormViewModel.factory(recurringId))
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val existing by viewModel.existing.collectAsStateWithLifecycle()

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var amountText by remember(existing) {
        mutableStateOf(existing?.let { formatMoneyInput(it.amountMinorUnits, settings.currencyCode) } ?: "")
    }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "") }
    var periodType by remember(existing) { mutableStateOf(existing?.periodType ?: BudgetPeriodType.MONTHLY) }
    var monthlyAnchorText by remember(existing) {
        mutableStateOf(if (existing?.periodType == BudgetPeriodType.MONTHLY) existing?.periodAnchor.toString() else "1")
    }
    var weeklyAnchor by remember(existing) {
        mutableStateOf(if (existing?.periodType == BudgetPeriodType.WEEKLY) existing?.periodAnchor ?: 1 else 1)
    }

    val amountMinorUnits = parseMoneyInput(amountText, settings.currencyCode)
    val monthlyAnchor = monthlyAnchorText.toIntOrNull()?.takeIf { it in 1..31 }
    val periodAnchor = if (periodType == BudgetPeriodType.MONTHLY) monthlyAnchor else weeklyAnchor
    val canSave = name.isNotBlank() && amountMinorUnits != null && category.isNotBlank() && periodAnchor != null

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.recurring_edit_title), style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.recurring_form_name_label)) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text(stringResource(R.string.recurring_form_amount_label, settings.currencyCode)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = amountText.isNotBlank() && amountMinorUnits == null,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        Text(
            stringResource(R.string.recurring_form_category_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
            categories.forEach { entity ->
                ChoiceButton(
                    label = entity.name,
                    selected = category == entity.name,
                    onClick = { category = entity.name }
                )
            }
        }

        Text(
            stringResource(R.string.recurring_form_frequency_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            ChoiceButton(
                label = stringResource(R.string.budget_form_period_monthly),
                selected = periodType == BudgetPeriodType.MONTHLY,
                onClick = { periodType = BudgetPeriodType.MONTHLY }
            )
            ChoiceButton(
                label = stringResource(R.string.budget_form_period_weekly),
                selected = periodType == BudgetPeriodType.WEEKLY,
                onClick = { periodType = BudgetPeriodType.WEEKLY }
            )
        }

        if (periodType == BudgetPeriodType.MONTHLY) {
            OutlinedTextField(
                value = monthlyAnchorText,
                onValueChange = { monthlyAnchorText = it },
                label = { Text(stringResource(R.string.budget_form_anchor_day_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = monthlyAnchorText.isNotBlank() && monthlyAnchor == null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                (1..7).forEach { isoDayOfWeek ->
                    ChoiceButton(
                        label = stringResource(weekdayLabelRes(isoDayOfWeek)),
                        selected = weeklyAnchor == isoDayOfWeek,
                        onClick = { weeklyAnchor = isoDayOfWeek }
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = { viewModel.delete(onSaved) }) {
                Text(stringResource(R.string.action_delete))
            }
            Row {
                OutlinedButton(onClick = onCancel, modifier = Modifier.padding(end = 8.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val minorUnits = amountMinorUnits ?: return@Button
                        val anchor = periodAnchor ?: return@Button
                        viewModel.save(name, minorUnits, category, periodType, anchor, settings.timeZoneId, settings.currencyCode, onSaved)
                    },
                    enabled = canSave
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

/** Shared with [ManualExpenseScreen]'s inline recurring-mode form. */
@Composable
internal fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = {}, enabled = false, modifier = Modifier.padding(end = 8.dp)) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.padding(end = 8.dp)) { Text(label) }
    }
}
