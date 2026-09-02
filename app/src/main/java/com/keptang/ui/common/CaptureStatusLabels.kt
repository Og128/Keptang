package com.keptang.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.keptang.R
import com.keptang.data.db.CaptureStatus

@Composable
fun captureStatusLabel(status: CaptureStatus): String = when (status) {
    CaptureStatus.RECORDING -> stringResource(R.string.status_recording)
    CaptureStatus.CAPTURED -> stringResource(R.string.status_captured)
    CaptureStatus.TRANSCRIBING -> stringResource(R.string.status_transcribing)
    CaptureStatus.PARSING -> stringResource(R.string.status_parsing)
    CaptureStatus.PROCESSED -> stringResource(R.string.status_processed)
    CaptureStatus.NEEDS_REVIEW -> stringResource(R.string.status_needs_review)
    CaptureStatus.FAILED -> stringResource(R.string.status_failed)
    CaptureStatus.CANCELLED -> stringResource(R.string.status_cancelled)
}
