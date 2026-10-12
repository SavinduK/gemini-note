package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.AppDatabase
import com.example.data.TaskEntity
import java.util.Calendar

class TodayTasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayTasksRemoteViewsFactory(applicationContext)
    }
}

class TodayTasksRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val tasksList = mutableListOf<TaskEntity>()

    override fun onCreate() {
        // Initial setup
    }

    override fun onDataSetChanged() {
        tasksList.clear()
        try {
            val database = AppDatabase.getDatabase(context)
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis

            cal.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = cal.timeInMillis

            // Fetch tasks for today
            val todayTasks = database.taskDao().getTasksForDaySync(startOfDay, endOfDay)
            if (todayTasks.isNotEmpty()) {
                tasksList.addAll(todayTasks)
            } else {
                // If no specific tasks are assigned strictly to today, show pending upcoming tasks
                val allTasks = database.taskDao().getAllTasksSync()
                val pending = allTasks.filter { !it.isCompleted }
                tasksList.addAll(pending.take(10))
            }
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        tasksList.clear()
    }

    override fun getCount(): Int = tasksList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= tasksList.size) return null

        val task = tasksList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        // Title
        views.setTextViewText(R.id.widget_item_title, task.title)

        // Status Icon
        val iconRes = if (task.isCompleted) R.drawable.ic_widget_check else R.drawable.ic_widget_uncheck
        views.setImageViewResource(R.id.widget_item_icon, iconRes)

        // Subtitle (Time & Category)
        val timeText = if (!task.dueTime.isNullOrBlank()) "Due ${task.dueTime}" else "Today"
        val categoryText = if (task.category.isNotBlank()) " • ${task.category}" else ""
        views.setTextViewText(R.id.widget_item_subtitle, "$timeText$categoryText")

        // Priority Badge
        views.setTextViewText(R.id.widget_item_priority_badge, task.priority.take(3))
        val badgeColor = when (task.priority) {
            "HIGH" -> Color.parseColor("#E53935")
            "MEDIUM" -> Color.parseColor("#FB8C00")
            else -> Color.parseColor("#43A047")
        }
        views.setInt(R.id.widget_item_priority_badge, "setBackgroundColor", badgeColor)

        // Fill-in Intent for item click
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in tasksList.indices) tasksList[position].id else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
