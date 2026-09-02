package com.keptang.ui.budgets

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
import com.keptang.core.Defaults
import com.keptang.data.db.BudgetPeriodType
import com.keptang.ui.common.parseMoneyInput
import java.util.Locale
import kotlin.math.pow

private sealed class TargetSelection {
    object Overall : TargetSelection()
    data class Category(val name: String) : TargetSelection()
}

private fun majorAmountText(amountMinorUnits: Long, currencyCode: String): String {
    val exponent = Defaults.minorUnitExponent(currencyCode)
    val major = amountMinorUnits / 10.0.pow(exponent)
    return if (exponent == 0) major.toLong().toString() else String.format(Locale.US, "%.${exponent}f", major)
}

private val WEEKDAY_LABELS = listOf(
    R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
    R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat,
    R.string.weekday_sun
)

@Composable
fun BudgetFormScreen(
    budgetId: String?,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: BudgetFormViewModel = viewModel(factory = BudgetFormViewModel.factory(budgetId))
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val existing by viewModel.existing.collectAsStateWithLifecycle()
    val availableTargets by viewModel.availableTargets.collectAsStateWithLifecycle()
    val isEditMode = budgetId != null

    var selectedTarget by remember { mutableStateOf<TargetSelection?>(null) }
    var amountText by remember(existing) {
        mutableStateOf(existing?.let { majorAmountText(it.amountMinorUnits, settings.currencyCode) } ?: "")
    }
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
    val targetValid = isEditMode || selectedTarget != null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(if (isEditMode) R.string.budget_edit_title else R.string.budget_add_title),
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            stringResource(R.string.budget_form_target_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        if (isEditMode) {
            Text(
                existing?.category ?: stringResource(R.string.budget_form_target_overall),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                if (availableTargets.overallAvailable) {
                    TargetButton(
                        label = stringResource(R.string.budget_form_target_overall),
                        selected = selectedTarget == TargetSelection.Overall,
                        onClick = { selectedTarget = TargetSelection.Overall }
                    )
                }
                availableTargets.availableCategories.forEach { category ->
                    TargetButton(
                        label = category,
                        selected = selectedTarget == TargetSelection.Category(category),
                        onClick = { selectedTarget = TargetSelection.Category(category) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text(stringResource(R.string.budget_form_amount_label, settings.currencyCode)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = amountText.isNotBlank() && amountMinorUnits == null,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        Text(
            stringResource(R.string.budget_form_period_type_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            TargetButton(
                label = stringResource(R.string.budget_form_period_monthly),
                selected = periodType == BudgetPeriodType.MONTHLY,
                onClick = { periodType = BudgetPeriodType.MONTHLY }
            )
            TargetButton(
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
            Text(
                stringResource(R.string.budget_form_anchor_weekday_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                WEEKDAY_LABELS.forEachIndexed { index, labelRes ->
                    val isoDayOfWeek = index + 1
                    TargetButton(
                        label = stringResource(labelRes),
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
            if (isEditMode) {
                OutlinedButton(onClick = { viewModel.delete(onSaved) }) {
                    Text(stringResource(R.string.action_delete))
                }
            } else {
                Row {}
            }
            Row {
                OutlinedButton(onClick = onCancel, modifier = Modifier.padding(end = 8.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val minorUnits = amountMinorUnits ?: return@Button
                        val anchor = periodAnchor ?: return@Button
                        val target = if (isEditMode) {
                            existing?.category
                        } else {
                            when (val selection = selectedTarget) {
                                is TargetSelection.Overall -> null
                                is TargetSelection.Category -> selection.name
                                null -> return@Button
                            }
                        }
                        viewModel.save(target, minorUnits, periodType, anchor, onSaved)
                    },
                    enabled = amountMinorUnits != null && periodAnchor != null && targetValid
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun TargetButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = {}, enabled = false, modifier = Modifier.padding(end = 8.dp)) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.padding(end = 8.dp)) { Text(label) }
    }
}
