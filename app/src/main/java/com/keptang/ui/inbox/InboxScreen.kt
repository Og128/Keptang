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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.data.db.CaptureEntity
import com.keptang.ui.common.captureStatusLabel
import com.keptang.ui.common.formatDateTime
import com.keptang.ui.review.ReviewScreen

private enum class InboxViewMode { TRANSCRIPTS, REVIEW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    onOpenCapture: (String) -> Unit,
    startOnReview: Boolean = false,
    viewModel: InboxViewModel = viewModel(factory = InboxViewModel.Factory)
) {
    val captures by viewModel.captures.collectAsStateWithLifecycle()
    var viewMode by remember { mutableStateOf(if (startOnReview) InboxViewMode.REVIEW else InboxViewMode.TRANSCRIPTS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_inbox)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                if (viewMode == InboxViewMode.TRANSCRIPTS) {
                    Button(onClick = {}, enabled = false, modifier = Modifier.padding(end = 8.dp)) {
                        Text(stringResource(R.string.inbox_view_transcripts))
                    }
                } else {
                    OutlinedButton(onClick = { viewMode = InboxViewMode.TRANSCRIPTS }, modifier = Modifier.padding(end = 8.dp)) {
                        Text(stringResource(R.string.inbox_view_transcripts))
                    }
                }
                if (viewMode == InboxViewMode.REVIEW) {
                    Button(onClick = {}, enabled = false) {
                        Text(stringResource(R.string.nav_review))
                    }
                } else {
                    OutlinedButton(onClick = { viewMode = InboxViewMode.REVIEW }) {
                        Text(stringResource(R.string.nav_review))
                    }
                }
            }

            when (viewMode) {
                InboxViewMode.TRANSCRIPTS -> {
                    if (captures.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.inbox_empty), style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
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
                }
                InboxViewMode.REVIEW -> ReviewScreen()
            }
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
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(captureStatusLabel(capture.status), style = MaterialTheme.typography.labelLarge)
                Text(
                    formatDateTime(capture.capturedAtEpochMillis, capture.timeZoneId),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    capture.rawTranscript?.takeIf { it.isNotBlank() } ?: stringResource(R.string.inbox_no_transcript),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}
