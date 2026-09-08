package com.keptang.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.keptang.R
import com.keptang.ui.quickadd.QuickAddActivity

/**
 * Home-screen widget: tapping it opens [QuickAddActivity], a small dialog-styled activity for
 * typing a "50 coffee"-style quick expense without voice. A real inline text field inside the
 * widget itself isn't reliable - there's no documented, guaranteed way for a widget's button
 * click to read back what was typed into an EditText in the same widget - so this opens a tiny
 * overlay instead of the app's normal UI, closing itself right after saving.
 */
class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val intent = Intent(context, QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add).apply {
                setOnClickPendingIntent(R.id.widget_quick_add_button, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
