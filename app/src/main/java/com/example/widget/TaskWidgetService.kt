package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.AppDatabase
import com.example.data.TaskEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskRemoteViewsFactory(this.applicationContext)
    }
}

class TaskRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var taskList: List<TaskEntity> = emptyList()

    override fun onCreate() {
        fetchData()
    }

    override fun onDataSetChanged() {
        fetchData()
    }

    private fun fetchData() {
        try {
            val db = AppDatabase.getDatabase(context)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            runBlocking {
                val list = db.taskDao().getDailyTasks(todayStr).firstOrNull() ?: emptyList()
                taskList = list
            }
        } catch (_: Exception) {
            taskList = emptyList()
        }
    }

    override fun onDestroy() {
        taskList = emptyList()
    }

    override fun getCount(): Int = taskList.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= taskList.size) return RemoteViews(context.packageName, R.layout.widget_task_item)
        val task = taskList[position]

        val views = RemoteViews(context.packageName, R.layout.widget_task_item)
        views.setTextViewText(R.id.widget_item_title, task.title)

        if (task.isCompleted) {
            views.setTextViewText(R.id.widget_item_checkbox, "☑")
            views.setTextColor(R.id.widget_item_checkbox, 0xFF00E676.toInt()) // Green
            views.setTextColor(R.id.widget_item_title, 0xFF888888.toInt())
        } else {
            views.setTextViewText(R.id.widget_item_checkbox, "☐")
            views.setTextColor(R.id.widget_item_checkbox, 0xFF00FFFF.toInt()) // Cyan
            views.setTextColor(R.id.widget_item_title, 0xFFFFFFFF.toInt())
        }

        if (task.customLabel.isNotBlank()) {
            views.setViewVisibility(R.id.widget_item_badge, View.VISIBLE)
            views.setTextViewText(R.id.widget_item_badge, task.customLabel)
        } else if (task.isExtraTask) {
            views.setViewVisibility(R.id.widget_item_badge, View.VISIBLE)
            views.setTextViewText(R.id.widget_item_badge, "BONUS")
        } else {
            views.setViewVisibility(R.id.widget_item_badge, View.GONE)
        }

        // Fill-in Intent for interactive clicking on task item / checkbox
        val fillInIntent = Intent().apply {
            putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_checkbox, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_title, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = taskList.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
