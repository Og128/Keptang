package com.keptang.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.data.db.CaptureStatus
import com.keptang.data.db.ExpenseEntity
import com.keptang.ui.common.InfoCard
import com.keptang.ui.common.captureStatusContainerColor
import com.keptang.ui.common.formatMoney
import com.keptang.ui.inbox.CaptureRow

@Composable
fun ReviewScreen(onOpenCapture: (String) -> Unit, viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory)) {
    val expenses by viewModel.needsReview.collectAsStateWithLifecycle()
    val failedCaptures by viewModel.failedCaptures.collectAsStateWithLifecycle()
    val emptyReviewCaptures by viewModel.emptyReviewCaptures.collectAsStateWithLifecycle()

    if (expenses.isEmpty() && failedCaptures.isEmpty() && emptyReviewCaptures.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.review_empty), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        if (failedCaptures.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.review_section_failed), MaterialTheme.colorScheme.error) }
            items(failedCaptures, key = { it.id }) { capture ->
                CaptureRow(capture, onClick = { onOpenCapture(capture.id) }, onDelete = { viewModel.deleteCapture(capture.id) })
            }
        }
        if (emptyReviewCaptures.isNotEmpty() || expenses.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.review_section_needs_review), REVIEW_ORANGE) }
        }
        items(emptyReviewCaptures, key = { it.id }) { capture ->
            CaptureRow(capture, onClick = { onOpenCapture(capture.id) }, onDelete = { viewModel.deleteCapture(capture.id) })
        }
        items(expenses, key = { it.id }) { expense ->
            ReviewCard(
                expense = expense,
                onApprove = { edited -> viewModel.update(edited); viewModel.approve(edited.id) },
                onReject = { viewModel.reject(expense.id) }
            )
        }
    }
}

private val REVIEW_ORANGE = Color(0xFFFFA000)

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ReviewCard(expense: ExpenseEntity, onApprove: (ExpenseEntity) -> Unit, onReject: () -> Unit) {
    var amountMajorText by remember(expense.id) { mutableStateOf((expense.amountMinorUnits / 100.0).toString()) }
    var category by remember(expense.id) { mutableStateOf(expense.category) }

    InfoCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        containerColor = captureStatusContainerColor(CaptureStatus.NEEDS_REVIEW)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                stringResource(
                    R.string.review_originally_heard,
                    formatMoney(expense.amountMinorUnits, expense.currencyCode)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = amountMajorText,
                onValueChange = { amountMajorText = it },
                label = { Text(stringResource(R.string.review_amount_label, expense.currencyCode)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(stringResource(R.string.review_category_label)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onReject, modifier = Modifier.padding(end = 8.dp)) {
                    Text(stringResource(R.string.action_delete))
                }
                Button(onClick = {
                    val minorUnits = ((amountMajorText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    onApprove(expense.copy(amountMinorUnits = minorUnits, category = category))
                }) {
                    Text(stringResource(R.string.action_approve))
                }
            }
        }
    }
}
