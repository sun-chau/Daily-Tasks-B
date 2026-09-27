package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TimetableWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_SCHEDULE) {
            triggerUpdate(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        const val ACTION_REFRESH_SCHEDULE = "com.example.widget.ACTION_REFRESH_SCHEDULE"

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TimetableWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                updateAllWidgets(context, appWidgetManager, ids)
            }
        }

        private fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("action_open_timetable", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                200,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("action_open_add_lecture_dialog", true)
            }
            val quickAddPendingIntent = PendingIntent.getActivity(
                context,
                201,
                quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dayCode = SimpleDateFormat("EEE", Locale.US).format(Date()).uppercase(Locale.US)
                    val rawSchedule = db.taskDao().getTimetableForDay(dayCode).firstOrNull() ?: emptyList()
                    val schedule = rawSchedule.filter { event ->
                        !event.isHolidayCancelled &&
                        (event.daysOfWeek.split(",").map { it.trim().uppercase(Locale.US) }
                            .any { it == dayCode || it == "ALL" } || event.daysOfWeek.isBlank())
                    }

                    val nowCal = Calendar.getInstance()
                    val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

                    val sortedSchedule = schedule.sortedBy { event ->
                        com.example.ui.TaskViewModel.parseTimeToMinutes(event.startTime)
                    }

                    val ongoingClass = sortedSchedule.firstOrNull { event ->
                        val start = com.example.ui.TaskViewModel.parseTimeToMinutes(event.startTime)
                        val end = com.example.ui.TaskViewModel.parseTimeToMinutes(event.endTime)
                        start <= nowMinutes && nowMinutes < end
                    }

                    val nextClass = if (ongoingClass == null) {
                        sortedSchedule.firstOrNull { event ->
                            com.example.ui.TaskViewModel.parseTimeToMinutes(event.startTime) > nowMinutes
                        }
                    } else null

                    val isLive = ongoingClass != null
                    val heroClass = ongoingClass ?: nextClass ?: sortedSchedule.firstOrNull()

                    // Schedule exact AlarmManager tick to refresh widget at boundary (start or end of current/next class)
                    scheduleNextWidgetTick(context, ongoingClass, nextClass)

                    for (widgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_timetable)
                        views.setOnClickPendingIntent(R.id.timetable_widget_root, pendingIntent)
                        views.setOnClickPendingIntent(R.id.timetable_quick_add_btn, quickAddPendingIntent)

                        val headerStatus = when {
                            isLive -> "● LIVE NOW"
                            nextClass != null -> "UP NEXT"
                            sortedSchedule.isNotEmpty() -> "ALL DONE"
                            else -> "NO CLASSES"
                        }
                        views.setTextViewText(R.id.timetable_widget_day, "$dayCode • $headerStatus")

                        if (heroClass == null) {
                            views.setViewVisibility(R.id.timetable_hero_container, View.GONE)
                            views.setTextViewText(R.id.timetable_slot_1, "No lectures scheduled today")
                            views.setViewVisibility(R.id.timetable_slot_2, View.GONE)
                            views.setViewVisibility(R.id.timetable_slot_3, View.GONE)
                            views.setTextViewText(R.id.timetable_widget_footer, "TAP [+] TO ADD • TAP TO OPEN")
                        } else {
                            views.setViewVisibility(R.id.timetable_hero_container, View.VISIBLE)
                            views.setTextViewText(R.id.timetable_hero_title, heroClass.title)
                            views.setTextViewText(R.id.timetable_hero_time, "${heroClass.startTime} - ${heroClass.endTime}")

                            val venue = if (heroClass.location.isNotBlank()) "📍 ${heroClass.location}" else "📍 Classroom TBA"
                            val prof = if (heroClass.professor.isNotBlank()) " • 👨‍🏫 ${heroClass.professor}" else ""
                            views.setTextViewText(R.id.timetable_hero_venue, "$venue$prof")

                            if (isLive) {
                                views.setTextViewText(R.id.timetable_hero_badge, "⚡ LIVE NOW")
                                val start = com.example.ui.TaskViewModel.parseTimeToMinutes(heroClass.startTime)
                                val end = com.example.ui.TaskViewModel.parseTimeToMinutes(heroClass.endTime)
                                val duration = maxOf(1, end - start)
                                val elapsed = (nowMinutes - start).coerceIn(0, duration)
                                val progress = ((elapsed.toFloat() / duration.toFloat()) * 100).toInt()
                                views.setProgressBar(R.id.timetable_hero_progressbar, 100, progress, false)
                                views.setViewVisibility(R.id.timetable_hero_progressbar, View.VISIBLE)
                            } else if (nextClass != null) {
                                views.setTextViewText(R.id.timetable_hero_badge, "▶ UP NEXT")
                                views.setViewVisibility(R.id.timetable_hero_progressbar, View.GONE)
                            } else {
                                views.setTextViewText(R.id.timetable_hero_badge, "✓ COMPLETED")
                                views.setViewVisibility(R.id.timetable_hero_progressbar, View.GONE)
                            }

                            // Secondary slots
                            val otherClasses = sortedSchedule.filter { it.id != heroClass.id }
                            if (otherClasses.isNotEmpty()) {
                                views.setViewVisibility(R.id.timetable_slot_1, View.VISIBLE)
                                val s1 = otherClasses[0]
                                val v1 = if (s1.location.isNotBlank()) " @ ${s1.location}" else ""
                                views.setTextViewText(R.id.timetable_slot_1, "• ${s1.startTime} ${s1.title}$v1")
                            } else {
                                views.setViewVisibility(R.id.timetable_slot_1, View.GONE)
                            }

                            if (otherClasses.size > 1) {
                                views.setViewVisibility(R.id.timetable_slot_2, View.VISIBLE)
                                val s2 = otherClasses[1]
                                val v2 = if (s2.location.isNotBlank()) " @ ${s2.location}" else ""
                                views.setTextViewText(R.id.timetable_slot_2, "• ${s2.startTime} ${s2.title}$v2")
                            } else {
                                views.setViewVisibility(R.id.timetable_slot_2, View.GONE)
                            }

                            if (otherClasses.size > 2) {
                                views.setViewVisibility(R.id.timetable_slot_3, View.VISIBLE)
                                val remaining = otherClasses.size - 2
                                views.setTextViewText(R.id.timetable_slot_3, "+$remaining more scheduled lectures")
                            } else {
                                views.setViewVisibility(R.id.timetable_slot_3, View.GONE)
                            }

                            views.setTextViewText(R.id.timetable_widget_footer, "${sortedSchedule.size} CLASSES TODAY • TAP TO OPEN")
                        }

                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                } catch (e: Exception) {
                    for (widgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_timetable)
                        views.setOnClickPendingIntent(R.id.timetable_widget_root, pendingIntent)
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }
            }
        }

        private fun scheduleNextWidgetTick(
            context: Context,
            ongoingClass: com.example.data.TaskEntity?,
            nextClass: com.example.data.TaskEntity?
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val tickMinutes = when {
                ongoingClass != null -> com.example.ui.TaskViewModel.parseTimeToMinutes(ongoingClass.endTime)
                nextClass != null -> com.example.ui.TaskViewModel.parseTimeToMinutes(nextClass.startTime)
                else -> return
            }

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, tickMinutes / 60)
                set(Calendar.MINUTE, tickMinutes % 60)
                set(Calendar.SECOND, 1)
            }

            if (cal.timeInMillis <= System.currentTimeMillis()) return

            val intent = Intent(context, TimetableWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_SCHEDULE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                777,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                }
            } catch (_: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
        }
    }
}
