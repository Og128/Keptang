package com.keptang.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.ui.common.formatMoneyInput
import com.keptang.ui.common.localDateOf
import com.keptang.ui.common.parseMoneyInput
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateRowFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

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
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val existingExpense by viewModel.existingExpense.collectAsStateWithLifecycle()
    val isFromVoiceCapture by viewModel.isFromVoiceCapture.collectAsStateWithLifecycle()
    val isEditMode = expenseId != null

    var amountText by remember { mutableStateOf("") }
    var currencyCode by remember(settings.currencyCode) { mutableStateOf(settings.currencyCode) }
    var category by remember { mutableStateOf("") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var account by remember(settings.defaultAccount) { mutableStateOf(settings.defaultAccount) }
    var paymentMethod by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(initialDate ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMoreInfo by remember { mutableStateOf(false) }
    var prefilled by remember { mutableStateOf(false) }
    var showEditWarning by remember { mutableStateOf(false) }

    LaunchedEffect(existingExpense) {
        val expense = existingExpense
        if (expense != null && !prefilled) {
            amountText = formatMoneyInput(expense.amountMinorUnits, expense.currencyCode)
            currencyCode = expense.currencyCode
            category = expense.category
            merchant = expense.merchant.orEmpty()
            notes = expense.notes.orEmpty()
            account = expense.account.orEmpty()
            paymentMethod = expense.paymentMethod.orEmpty()
            selectedDate = localDateOf(expense.occurredAtEpochMillis, expense.timeZoneId)
            prefilled = true
        }
    }

    val amountMinorUnits = parseMoneyInput(amountText, currencyCode)
    val defaultCategory = stringResource(R.string.manual_add_default_category)

    fun performSave() {
        val minorUnits = amountMinorUnits ?: return
        val zone = ZoneId.of(settings.timeZoneId)
        val occurredAtEpochMillis = selectedDate.atTime(ZonedDateTime.now(zone).toLocalTime())
            .atZone(zone).toInstant().toEpochMilli()
        viewModel.save(
            amountMinorUnits = minorUnits,
            currencyCode = currencyCode,
            category = category.ifBlank { defaultCategory },
            account = account,
            paymentMethod = paymentMethod,
            merchant = merchant,
            notes = notes,
            timeZoneId = settings.timeZoneId,
            occurredAtEpochMillis = occurredAtEpochMillis,
            onSaved = onSaved
        )
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
                Text(stringResource(R.string.action_cancel))
            }
            Text(
                stringResource(if (isEditMode) R.string.manual_edit_title else R.string.manual_add_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center)
            )
            TextButton(
                onClick = { if (isEditMode && isFromVoiceCapture) showEditWarning = true else performSave() },
                enabled = amountMinorUnits != null,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
        HorizontalDivider()

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
            Text(
                stringResource(R.string.manual_details_section),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                if (isEditMode) {
                    FormRow(label = stringResource(R.string.manual_status_label)) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(stringResource(R.string.manual_status_approved), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                FormRow(label = stringResource(R.string.manual_add_date), onClick = { showDatePicker = true }) {
                    Text(selectedDate.format(dateRowFormatter), style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                }
                Box {
                    FormRow(label = stringResource(R.string.manual_add_category), onClick = { categoryMenuExpanded = true }) {
                        CategoryTag(categories.find { it.name == category })
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                    }
                    DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        categories.forEach { entity ->
                            DropdownMenuItem(
                                text = { CategoryTag(entity) },
                                onClick = { category = entity.name; categoryMenuExpanded = false }
                            )
                        }
                    }
                }
                FormRow(label = stringResource(R.string.manual_add_amount)) {
                    InlineValueField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    InlineValueField(
                        value = currencyCode,
                        onValueChange = { currencyCode = it.uppercase() },
                        modifier = Modifier.width(64.dp).padding(start = 8.dp)
                    )
                }
                FormRow(label = stringResource(R.string.manual_add_merchant), isLast = true) {
                    InlineValueField(value = merchant, onValueChange = { merchant = it }, modifier = Modifier.weight(1f))
                }
            }

            Text(
                stringResource(R.string.manual_add_notes),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 16.dp)
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text(stringResource(R.string.manual_add_notes_placeholder)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showMoreInfo = !showMoreInfo }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.manual_more_info_section),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(if (showMoreInfo) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }
            if (showMoreInfo) {
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    FormRow(label = stringResource(R.string.manual_add_account)) {
                        InlineValueField(value = account, onValueChange = { account = it }, modifier = Modifier.weight(1f))
                    }
                    FormRow(label = stringResource(R.string.manual_add_payment_method), isLast = true) {
                        InlineValueField(value = paymentMethod, onValueChange = { paymentMethod = it }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
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

@Composable
private fun FormRow(
    label: String,
    onClick: (() -> Unit)? = null,
    isLast: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                trailingContent()
            }
        }
        if (!isLast) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}

/**
 * A borderless, right-aligned text field sized to actually fit its value - the stock Material3
 * [androidx.compose.material3.TextField] reserves ~16dp of horizontal padding on each side even
 * with its container/indicator hidden, which left almost no room for digits in a compact row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineValueField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
    ) { innerTextField ->
        TextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(vertical = 8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}
