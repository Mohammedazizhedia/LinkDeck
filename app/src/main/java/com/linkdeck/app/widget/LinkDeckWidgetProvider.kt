package com.linkdeck.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.linkdeck.app.MainActivity
import com.linkdeck.app.R

/** Tinted widget (background #0F766E): tapping opens the app. */
class LinkDeckWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_linkdeck).apply {
                setOnClickPendingIntent(R.id.widget_root, open)
            }
            manager.updateAppWidget(id, views)
        }
    }
}
