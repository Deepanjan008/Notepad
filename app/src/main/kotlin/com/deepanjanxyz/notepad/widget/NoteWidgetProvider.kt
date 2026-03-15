package com.deepanjanxyz.notepad.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.data.db.DatabaseHelper
import com.deepanjanxyz.notepad.ui.editor.NoteEditorActivity
import com.deepanjanxyz.notepad.ui.main.MainActivity

class NoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateWidget(context, manager, id) }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val db     = DatabaseHelper(context)
        val notes  = db.getAllNotes()
        val latest = notes.firstOrNull()

        val views = RemoteViews(context.packageName, R.layout.widget_note)

        // Show latest note or placeholder
        if (latest != null) {
            views.setTextViewText(R.id.widget_title,   latest.title.ifBlank { context.getString(R.string.untitled) })
            views.setTextViewText(R.id.widget_content, latest.content.take(120))
            views.setTextViewText(R.id.widget_date,    latest.date)
        } else {
            views.setTextViewText(R.id.widget_title,   context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_content, context.getString(R.string.widget_empty))
            views.setTextViewText(R.id.widget_date,    "")
        }

        // Open app on card click
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_card, openApp)

        // New note button
        val newNote = PendingIntent.getActivity(
            context, 1,
            Intent(context, NoteEditorActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_new, newNote)

        manager.updateAppWidget(widgetId, views)
    }
}
