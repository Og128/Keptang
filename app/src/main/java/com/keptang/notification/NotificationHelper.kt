package com.keptang.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.core.app.NotificationCompat
import com.keptang.R
import com.keptang.capture.ProcessOutcome
import com.keptang.di.ServiceLocator
import com.keptang.ui.MainActivity
import java.util.Locale

object NotificationIds {
    const val RECORDING = 1001
    const val RESULT_BASE = 2000
}

object NotificationActions {
    const val STOP_RECORDING = "com.keptang.action.STOP_RECORDING"
    const val CANCEL_RECORDING = "com.keptang.action.CANCEL_RECORDING"
    const val UNDO_EXPENSES = "com.keptang.action.UNDO_EXPENSES"
    const val EXTRA_CAPTURE_ID = "extra_capture_id"
}

class NotificationHelper(private val context: Context) {

    private val manager = requireNotNull(context.getSystemService(NotificationManager::class.java))

    /**
     * A [Context] wrapped with the currently-selected app language, so notification text (built
     * outside the Compose tree, unlike everything else) still follows the in-app language toggle
     * rather than the device's system locale.
     */
    private fun localizedContext(): Context {
        val languageCode = ServiceLocator.currentSettings.value.languageCode
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(languageCode))
        return context.createConfigurationContext(config)
    }

    fun ensureChannels() {
        val strings = localizedContext()
        val recording = NotificationChannel(
            CHANNEL_RECORDING,
            strings.getString(R.string.notif_channel_recording_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = strings.getString(R.string.notif_channel_recording_desc) }

        val results = NotificationChannel(
            CHANNEL_RESULTS,
            strings.getString(R.string.notif_channel_results_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = strings.getString(R.string.notif_channel_results_desc) }

        manager.createNotificationChannel(recording)
        manager.createNotificationChannel(results)
    }

    fun buildRecordingNotification(captureId: String): Notification {
        val strings = localizedContext()
        val stopIntent = actionBroadcast(NotificationActions.STOP_RECORDING, captureId, requestCode = 10)
        val cancelIntent = actionBroadcast(NotificationActions.CANCEL_RECORDING, captureId, requestCode = 11)

        return NotificationCompat.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_notification_mic)
            .setContentTitle(strings.getString(R.string.notif_recording_title))
            .setContentText(strings.getString(R.string.notif_recording_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, strings.getString(R.string.notif_action_stop), stopIntent)
            .addAction(0, strings.getString(R.string.notif_action_cancel), cancelIntent)
            .build()
    }

    /**
     * Shown (briefly, via startForeground) when the widget is tapped before the user has ever
     * granted microphone access through the app's own onboarding flow.
     */
    fun buildPermissionRequiredNotification(): Notification {
        val strings = localizedContext()
        return NotificationCompat.Builder(context, CHANNEL_RESULTS)
            .setSmallIcon(R.drawable.ic_notification_mic)
            .setContentTitle(strings.getString(R.string.app_name))
            .setContentText(strings.getString(R.string.notif_mic_permission_needed))
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    fun notifyResult(captureId: String, outcome: ProcessOutcome) {
        val strings = localizedContext()
        val text = when (outcome) {
            is ProcessOutcome.Processed -> pluralExpensesAdded(strings, outcome.approvedCount)
            is ProcessOutcome.NeedsReview -> when {
                outcome.approvedCount > 0 ->
                    strings.getString(
                        R.string.notif_result_needs_review_combo,
                        pluralExpensesAdded(strings, outcome.approvedCount),
                        outcome.reviewCount
                    )
                else -> strings.getString(R.string.notif_result_could_not_understand)
            }
            ProcessOutcome.SavedForLater -> strings.getString(R.string.notif_result_saved_for_later)
            ProcessOutcome.CouldNotUnderstand -> strings.getString(R.string.notif_result_could_not_understand)
            ProcessOutcome.AlreadyProcessed, ProcessOutcome.NotFound -> return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            captureId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_CAPTURE_ID, captureId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_RESULTS)
            .setSmallIcon(R.drawable.ic_notification_mic)
            .setContentTitle(strings.getString(R.string.app_name))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        val addedExpenses = when (outcome) {
            is ProcessOutcome.Processed -> outcome.approvedCount
            is ProcessOutcome.NeedsReview -> outcome.approvedCount
            else -> 0
        }
        if (addedExpenses > 0) {
            val undoIntent = actionBroadcast(NotificationActions.UNDO_EXPENSES, captureId, requestCode = 12)
            builder.addAction(0, strings.getString(R.string.notif_action_undo), undoIntent)
        }

        manager.notify(NotificationIds.RESULT_BASE + captureId.hashCode(), builder.build())
    }

    fun cancelRecordingNotification() {
        manager.cancel(NotificationIds.RECORDING)
    }

    fun cancelResultNotification(captureId: String) {
        manager.cancel(NotificationIds.RESULT_BASE + captureId.hashCode())
    }

    private fun pluralExpensesAdded(strings: Context, count: Int): String =
        strings.resources.getQuantityString(R.plurals.notif_expenses_added, count, count)

    private fun actionBroadcast(action: String, captureId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActions.EXTRA_CAPTURE_ID, captureId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode + captureId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_RECORDING = "keptang_recording"
        const val CHANNEL_RESULTS = "keptang_results"
    }
}
