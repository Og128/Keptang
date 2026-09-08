package com.keptang.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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

private val WARNING_ORANGE = Color(0xFFFFA000)

/**
 * A subtle red/orange tint over the normal card background for statuses that need the user's
 * attention - red for an outright failed transcript, orange for one flagged needs-review. Every
 * other status keeps the plain [MaterialTheme.colorScheme.surfaceContainer] tone.
 */
@Composable
fun captureStatusContainerColor(status: CaptureStatus): Color {
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val tint = when (status) {
        CaptureStatus.FAILED -> MaterialTheme.colorScheme.error
        CaptureStatus.NEEDS_REVIEW -> WARNING_ORANGE
        else -> return surface
    }
    return tint.copy(alpha = 0.18f).compositeOver(surface)
}
