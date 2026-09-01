package com.keptang.ui.capturedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.data.db.CaptureStatus
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

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Status: ${capture.status}", style = MaterialTheme.typography.titleMedium)
        Text(
            formatDateTime(capture.capturedAtEpochMillis, capture.timeZoneId),
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        Text("Transcript", style = MaterialTheme.typography.titleSmall)
        Text(
            capture.rawTranscript?.takeIf { it.isNotBlank() } ?: "(none yet)",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (capture.errorMessage != null) {
            Text(
                "Error: ${capture.errorMessage}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (state.expenses.isNotEmpty()) {
            Text("Expenses from this capture", style = MaterialTheme.typography.titleSmall)
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
            OutlinedButton(onClick = { viewModel.delete(); onDeleted() }) {
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
}
