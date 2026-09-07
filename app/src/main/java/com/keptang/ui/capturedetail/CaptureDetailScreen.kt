package com.keptang.ui.capturedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.ui.common.captureStatusLabel
import com.keptang.ui.common.formatDateTime
import com.keptang.ui.common.formatMoney

@Composable
fun CaptureDetailScreen(
    captureId: String,
    onDeleted: () -> Unit,
    viewModel: CaptureDetailViewModel = viewModel(factory = CaptureDetailViewModel.factory(captureId))
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val capture = state.capture ?: return
    val hasExpenses = state.expenses.isNotEmpty()

    var isEditing by remember { mutableStateOf(false) }
    var editedTranscript by remember(capture.id) { mutableStateOf(capture.rawTranscript.orEmpty()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.capture_detail_status_prefix, captureStatusLabel(capture.status)),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            formatDateTime(capture.capturedAtEpochMillis, capture.timeZoneId),
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.capture_detail_transcript_title), style = MaterialTheme.typography.titleSmall)
            if (!isEditing) {
                IconButton(onClick = {
                    editedTranscript = capture.rawTranscript.orEmpty()
                    isEditing = true
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                }
            }
        }

        if (isEditing) {
            OutlinedTextField(
                value = editedTranscript,
                onValueChange = { editedTranscript = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.padding(end = 8.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(onClick = { showEditConfirm = true }) {
                    Text(stringResource(R.string.action_save))
                }
            }
        } else {
            Text(
                capture.rawTranscript?.takeIf { it.isNotBlank() } ?: stringResource(R.string.capture_detail_transcript_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (capture.errorMessage != null) {
            Text(
                stringResource(R.string.capture_detail_error_prefix, capture.errorMessage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (hasExpenses) {
            Text(stringResource(R.string.capture_detail_expenses_title), style = MaterialTheme.typography.titleSmall)
            state.expenses.forEach { expense ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        "${formatMoney(expense.amountMinorUnits, expense.currencyCode)} · ${expense.category} · ${expense.reviewStatus}",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { showDeleteConfirm = true }) {
                Text(stringResource(R.string.action_delete))
            }
            if (capture.status.isRetryable()) {
                Button(onClick = { viewModel.retry() }, enabled = !state.isRetrying) {
                    if (state.isRetrying) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.capture_detail_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        if (hasExpenses) R.string.capture_detail_delete_confirm_message_with_expense
                        else R.string.capture_detail_delete_confirm_message_plain
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                    onDeleted()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showEditConfirm) {
        AlertDialog(
            onDismissRequest = { showEditConfirm = false },
            title = { Text(stringResource(R.string.capture_detail_edit_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        if (hasExpenses) R.string.capture_detail_edit_confirm_message_with_expense
                        else R.string.capture_detail_edit_confirm_message_plain
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    showEditConfirm = false
                    isEditing = false
                    viewModel.editTranscript(editedTranscript)
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
