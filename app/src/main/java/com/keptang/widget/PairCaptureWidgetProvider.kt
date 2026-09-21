package com.keptang.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import com.keptang.R
import com.keptang.capture.VoiceCaptureService

/**
 * The second voice widget: both mascots, the chihuahua at a microphone. Starts the same
 * [VoiceCaptureService] on the same terms as [VoiceCaptureWidgetProvider] - the PendingIntent
 * targets the service, never [com.keptang.ui.MainActivity], so tapping it records without opening
 * the app.
 *
 * It exists alongside the single-animal one rather than replacing it because the two answer
 * different wants: that one belongs to whichever theme is active, this one is the pair regardless.
 * Nothing here reads the theme for that reason.
 */
class PairCaptureWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context, FRAMES.first()))
        }
    }

    companion object {

        /**
         * The talking loop. Three frames rather than the single-animal widget's two, which is why
         * the capture service drives both by frame index and lets each provider wrap at its own
         * length instead of pushing one shared drawable to everything.
         */
        private val FRAMES = listOf(
            R.drawable.widget_pair_talk_1,
            R.drawable.widget_pair_talk_2,
            R.drawable.widget_pair_talk_3
        )

        fun showFrame(context: Context, index: Int) {
            render(context, FRAMES[index.mod(FRAMES.size)])
        }

        fun showResting(context: Context) {
            render(context, FRAMES.first())
        }

        private fun render(context: Context, @DrawableRes imageRes: Int) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PairCaptureWidgetProvider::class.java)
            for (appWidgetId in appWidgetManager.getAppWidgetIds(componentName)) {
                appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context, imageRes))
            }
        }

        private fun buildRemoteViews(context: Context, @DrawableRes imageRes: Int): RemoteViews {
            val toggleIntent = Intent(context, VoiceCaptureService::class.java)
                .setAction(VoiceCaptureService.ACTION_TOGGLE)
            val pendingIntent = PendingIntent.getForegroundService(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return RemoteViews(context.packageName, R.layout.widget_pair_capture).apply {
                setImageViewResource(R.id.widget_pair_button, imageRes)
                setOnClickPendingIntent(R.id.widget_pair_button, pendingIntent)
            }
        }
    }
}
