package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_TASK) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
            if (taskId != -1L) {
                // Play tactile and sound feedback
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    audioManager?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK)

                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                        vibratorManager?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(45, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(45)
                    }
                } catch (ignored: Exception) {}

                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val tasks = db.taskDao().getDailyTasks(todayStr).firstOrNull() ?: emptyList()
                    val task = tasks.firstOrNull { it.id == taskId }
                    if (task != null) {
                        val updated = task.copy(
                            isCompleted = !task.isCompleted,
                            completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
                        )
                        db.taskDao().updateTask(updated)
                        triggerUpdate(context)
                    }
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        const val ACTION_TOGGLE_TASK = "com.example.widget.ACTION_TOGGLE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TaskWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_tasks_listview)
                updateAllWidgets(context, appWidgetManager, ids)
            }
        }

        private fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            // Quick Add Intent
            val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("action_open_add_dialog", true)
            }
            val quickAddPendingIntent = PendingIntent.getActivity(
                context,
                101,
                quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val tasks = db.taskDao().getDailyTasks(todayStr).firstOrNull() ?: emptyList()

                    val total = tasks.size
                    val done = tasks.count { it.isCompleted }

                    for (widgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_daily_tasks)

                        // Bind header click
                        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)
                        views.setOnClickPendingIntent(R.id.widget_status, openAppPendingIntent)
                        views.setOnClickPendingIntent(R.id.widget_footer, openAppPendingIntent)

                        // Quick Add button click
                        views.setOnClickPendingIntent(R.id.widget_quick_add_btn, quickAddPendingIntent)

                        views.setTextViewText(R.id.widget_status, "$done/$total DONE")

                        // Service intent for RemoteViewsService (ListView)
                        val serviceIntent = Intent(context, TaskWidgetService::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                        }
                        views.setRemoteAdapter(R.id.widget_tasks_listview, serviceIntent)
                        views.setEmptyView(R.id.widget_tasks_listview, R.id.widget_empty_view)

                        // Template Intent for handling item clicks (toggle checkbox)
                        val toggleIntent = Intent(context, TaskWidgetProvider::class.java).apply {
                            action = ACTION_TOGGLE_TASK
                        }
                        val togglePendingIntent = PendingIntent.getBroadcast(
                            context,
                            0,
                            toggleIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0)
                        )
                        views.setPendingIntentTemplate(R.id.widget_tasks_listview, togglePendingIntent)

                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                } catch (e: Exception) {
                    for (widgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_daily_tasks)
                        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }
            }
        }
    }
}
