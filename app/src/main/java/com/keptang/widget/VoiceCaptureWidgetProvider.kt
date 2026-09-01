package com.keptang.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.keptang.R
import com.keptang.capture.VoiceCaptureService

/**
 * Home-screen widget: a single microphone button that starts [VoiceCaptureService] directly.
 * Nothing here launches [com.keptang.ui.MainActivity] - the click PendingIntent targets the
 * foreground service, not an activity, so the widget-to-recording path never opens the app.
 *
 * Starting a foreground service from a PendingIntent triggered by a widget click is one of the
 * documented exemptions to Android 12+'s "no starting foreground services from the background"
 * restriction, since it originates from direct user interaction with a widget.
 */
class VoiceCaptureWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context))
        }
    }

    private fun buildRemoteViews(context: Context): RemoteViews {
        val toggleIntent = Intent(context, VoiceCaptureService::class.java)
            .setAction(VoiceCaptureService.ACTION_TOGGLE)
        val pendingIntent = PendingIntent.getForegroundService(
            context,
            0,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteViews(context.packageName, R.layout.widget_voice_capture).apply {
            setOnClickPendingIntent(R.id.widget_mic_button, pendingIntent)
        }
    }
}
