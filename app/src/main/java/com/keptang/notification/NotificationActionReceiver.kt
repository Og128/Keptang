package com.keptang.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.keptang.capture.VoiceCaptureService
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val captureId = intent.getStringExtra(NotificationActions.EXTRA_CAPTURE_ID) ?: return

        when (intent.action) {
            NotificationActions.STOP_RECORDING -> {
                context.startService(
                    Intent(context, VoiceCaptureService::class.java)
                        .setAction(VoiceCaptureService.ACTION_STOP)
                        .putExtra(NotificationActions.EXTRA_CAPTURE_ID, captureId)
                )
            }
            NotificationActions.CANCEL_RECORDING -> {
                context.startService(
                    Intent(context, VoiceCaptureService::class.java)
                        .setAction(VoiceCaptureService.ACTION_CANCEL)
                        .putExtra(NotificationActions.EXTRA_CAPTURE_ID, captureId)
                )
            }
            NotificationActions.UNDO_EXPENSES -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ServiceLocator.expenseRepository.undoForCapture(captureId)
                        ServiceLocator.captureRepository.markNeedsReview(captureId, "Expenses undone by user")
                        ServiceLocator.notificationHelper.cancelResultNotification(captureId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
