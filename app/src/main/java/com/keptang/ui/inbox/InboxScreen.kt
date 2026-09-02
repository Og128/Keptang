package com.keptang.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.keptang.data.db.CaptureEntity
import com.keptang.ui.common.captureStatusLabel
import com.keptang.ui.common.formatDateTime

@Composable
fun InboxScreen(
    onOpenCapture: (String) -> Unit,
    viewModel: InboxViewModel = viewModel(factory = InboxViewModel.Factory)
) {
    val captures by viewModel.captures.collectAsStateWithLifecycle()

    if (captures.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.inbox_empty), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        items(captures, key = { it.id }) { capture ->
            CaptureRow(
                capture = capture,
                onClick = { onOpenCapture(capture.id) },
                onDelete = { viewModel.delete(capture.id) }
            )
        }
    }
}

@Composable
private fun CaptureRow(capture: CaptureEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(captureStatusLabel(capture.status), style = MaterialTheme.typography.labelLarge)
                Text(
                    formatDateTime(capture.capturedAtEpochMillis, capture.timeZoneId),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    capture.rawTranscript?.takeIf { it.isNotBlank() } ?: stringResource(R.string.inbox_no_transcript),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}
