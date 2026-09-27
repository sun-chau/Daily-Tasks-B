package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.TaskEntity
import com.example.ui.TaskViewModel
import java.util.Calendar

object ReminderScheduler {

    fun scheduleTaskReminder(context: Context, task: TaskEntity) {
        val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

        val targetTime = when {
            task.isFixedEvent -> {
                if (!prefs.getBoolean("pre_class_alerts_enabled", true)) return
                val leadTimeMinutes = prefs.getInt("pre_class_lead_time_min", 15)
                val classMinutes = TaskViewModel.parseTimeToMinutes(task.startTime)
                if (classMinutes == Int.MAX_VALUE) return
                val alertMinutes = classMinutes - leadTimeMinutes
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, (alertMinutes / 60).coerceAtLeast(0))
                    set(Calendar.MINUTE, (alertMinutes % 60).coerceAtLeast(0))
                    set(Calendar.SECOND, 0)
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                cal.timeInMillis
            }
            task.isFutureTask && task.targetDateTime != null -> {
                task.targetDateTime
            }
            task.needsConsistency -> {
                val customHour = prefs.getInt("reminder_hour", 20)
                val customMinute = prefs.getInt("reminder_minute", 0)

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, customHour)
                    set(Calendar.MINUTE, customMinute)
                    set(Calendar.SECOND, 0)
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                cal.timeInMillis
            }
            else -> return
        }

        if (targetTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, task.id)
            val notifTitle = if (task.isFixedEvent) {
                "Upcoming Class in ${prefs.getInt("pre_class_lead_time_min", 15)}m: ${task.title} at ${task.startTime} ${if (task.location.isNotBlank()) "@ " + task.location else ""}"
            } else {
                task.title
            }
            putExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE, notifTitle)
            putExtra(ReminderBroadcastReceiver.EXTRA_IS_CONSISTENCY, task.needsConsistency)
            putExtra(ReminderBroadcastReceiver.EXTRA_CUSTOM_LABEL, if (task.isFixedEvent) "CLASS ALERT" else task.customLabel)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    targetTime,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                targetTime,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun triggerImmediateNotification(
        context: Context,
        taskId: Long,
        title: String,
        isConsistency: Boolean,
        customLabel: String = ""
    ) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE, title)
            putExtra(ReminderBroadcastReceiver.EXTRA_IS_CONSISTENCY, isConsistency)
            putExtra(ReminderBroadcastReceiver.EXTRA_CUSTOM_LABEL, customLabel)
        }
        context.sendBroadcast(intent)
    }
}
