package com.keptang.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.keptang.ui.common.formatMoneyInput
import com.keptang.ui.common.localDateOf
import com.keptang.ui.common.parseMoneyInput
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualExpenseScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    initialDate: LocalDate? = null,
    expenseId: String? = null,
    viewModel: ManualExpenseViewModel = viewModel(factory = ManualExpenseViewModel.factory(expenseId))
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val categoryNames by viewModel.categoryNames.collectAsStateWithLifecycle()
    val existingExpense by viewModel.existingExpense.collectAsStateWithLifecycle()
    val isFromVoiceCapture by viewModel.isFromVoiceCapture.collectAsStateWithLifecycle()
    val isEditMode = expenseId != null

    var amountText by remember { mutableStateOf("") }
    var currencyCode by remember(settings.currencyCode) { mutableStateOf(settings.currencyCode) }
    var category by remember { mutableStateOf("") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var merchant by remember { mutableStateOf("") }
    var account by remember(settings.defaultAccount) { mutableStateOf(settings.defaultAccount) }
    var paymentMethod by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf((initialDate ?: LocalDate.now()).toString()) }
    var prefilled by remember { mutableStateOf(false) }
    var showEditWarning by remember { mutableStateOf(false) }

    LaunchedEffect(existingExpense) {
        val expense = existingExpense
        if (expense != null && !prefilled) {
            amountText = formatMoneyInput(expense.amountMinorUnits, expense.currencyCode)
            currencyCode = expense.currencyCode
            category = expense.category
            merchant = expense.merchant.orEmpty()
            account = expense.account.orEmpty()
            paymentMethod = expense.paymentMethod.orEmpty()
            dateText = localDateOf(expense.occurredAtEpochMillis, expense.timeZoneId).toString()
            prefilled = true
        }
    }

    val amountMinorUnits = parseMoneyInput(amountText, currencyCode)
    val selectedDate = runCatching { LocalDate.parse(dateText) }.getOrNull()
    val defaultCategory = stringResource(R.string.manual_add_default_category)

    fun performSave() {
        val minorUnits = amountMinorUnits ?: return
        val date = selectedDate ?: return
        val zone = ZoneId.of(settings.timeZoneId)
        val occurredAtEpochMillis = date.atTime(ZonedDateTime.now(zone).toLocalTime())
            .atZone(zone).toInstant().toEpochMilli()
        viewModel.save(
            amountMinorUnits = minorUnits,
            currencyCode = currencyCode,
            category = category.ifBlank { defaultCategory },
            account = account,
            paymentMethod = paymentMethod,
            merchant = merchant,
            timeZoneId = settings.timeZoneId,
            occurredAtEpochMillis = occurredAtEpochMillis,
            onSaved = onSaved
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(if (isEditMode) R.string.manual_edit_title else R.string.manual_add_title),
            style = MaterialTheme.typography.titleLarge
        )

        Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.manual_add_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountText.isNotBlank() && amountMinorUnits == null,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = currencyCode,
                onValueChange = { currencyCode = it.uppercase() },
                label = { Text(stringResource(R.string.manual_add_currency)) },
                modifier = Modifier.width(100.dp).padding(start = 8.dp)
            )
        }
        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            label = { Text(stringResource(R.string.manual_add_date)) },
            isError = selectedDate == null,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        val categorySuggestions = categoryNames.filter { it.contains(category, ignoreCase = true) }
        ExposedDropdownMenuBox(
            expanded = categoryMenuExpanded && categorySuggestions.isNotEmpty(),
            onExpandedChange = { categoryMenuExpanded = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it; categoryMenuExpanded = true },
                label = { Text(stringResource(R.string.manual_add_category)) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryMenuExpanded && categorySuggestions.isNotEmpty(),
                onDismissRequest = { categoryMenuExpanded = false }
            ) {
                categorySuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            category = suggestion
                            categoryMenuExpanded = false
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text(stringResource(R.string.manual_add_merchant)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
            value = account,
            onValueChange = { account = it },
            label = { Text(stringResource(R.string.manual_add_account)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
            value = paymentMethod,
            onValueChange = { paymentMethod = it },
            label = { Text(stringResource(R.string.manual_add_payment_method)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.padding(end = 8.dp)) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = {
                    if (isEditMode && isFromVoiceCapture) {
                        showEditWarning = true
                    } else {
                        performSave()
                    }
                },
                enabled = amountMinorUnits != null && selectedDate != null
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    if (showEditWarning) {
        AlertDialog(
            onDismissRequest = { showEditWarning = false },
            title = { Text(stringResource(R.string.manual_edit_warning_title)) },
            text = { Text(stringResource(R.string.manual_edit_warning_message)) },
            confirmButton = {
                Button(onClick = { showEditWarning = false; performSave() }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditWarning = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
