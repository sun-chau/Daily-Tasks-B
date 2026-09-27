package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        val isConsistency = intent.getBooleanExtra(EXTRA_IS_CONSISTENCY, false)
        val customLabel = intent.getStringExtra(EXTRA_CUSTOM_LABEL) ?: ""

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "tasks_reminders_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task & Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Utilitarian reminders for consistent habits and upcoming planned tasks"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val headerText = if (isConsistency) "[ CONSISTENCY HABIT ]" else "[ DISPATCH TARGET ]"
        val tagText = if (customLabel.isNotBlank()) customLabel else if (isConsistency) "STREAK" else "TIMED"

        // Build Neo-Brutalist Custom RemoteViews
        val collapsedView = RemoteViews(context.packageName, R.layout.notification_brutalist_collapsed).apply {
            setTextViewText(R.id.notif_badge, headerText)
            setTextViewText(R.id.notif_tag, tagText)
            setTextViewText(R.id.notif_title, taskTitle)
        }

        val expandedView = RemoteViews(context.packageName, R.layout.notification_brutalist_expanded).apply {
            setTextViewText(R.id.notif_expanded_badge, headerText)
            setTextViewText(R.id.notif_expanded_tag, tagText)
            setTextViewText(R.id.notif_expanded_title, taskTitle)
            val desc = if (isConsistency) {
                "Maintain relentless daily momentum. Complete this objective today to preserve your streak."
            } else {
                "Scheduled future target is now active. Open your workspace and execute."
            }
            setTextViewText(R.id.notif_expanded_desc, desc)
            setOnClickPendingIntent(R.id.notif_cta_btn, pendingIntent)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId.toInt().coerceAtLeast(1001), notification)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_IS_CONSISTENCY = "extra_is_consistency"
        const val EXTRA_CUSTOM_LABEL = "extra_custom_label"
    }
}
