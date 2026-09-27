package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Database(entities = [TaskEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_tasks_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialTasks(database.taskDao())
                    }
                }
            }
        }

        suspend fun seedInitialTasks(dao: TaskDao) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val todayStr = sdf.format(calendar.time)

            // Seed initial today tasks
            val initialTasks = listOf(
                TaskEntity(
                    title = "Morning Deep Focus Sprint",
                    description = "90 minutes distraction-free work on core project",
                    date = todayStr,
                    isCompleted = true,
                    completedAt = System.currentTimeMillis() - 7200000,
                    importance = ImportanceLevel.VERY_IMPORTANT.name,
                    customLabel = "DEEP WORK",
                    needsConsistency = true,
                    hasReminder = true
                ),
                TaskEntity(
                    title = "Review Weekly Performance Metrics",
                    description = "Analyze task completion rate and backlog items",
                    date = todayStr,
                    isCompleted = false,
                    importance = ImportanceLevel.MEDIUM.name,
                    customLabel = "REVIEW",
                    needsConsistency = false,
                    hasReminder = false
                ),
                TaskEntity(
                    title = "Daily 30-Minute Fitness Routine",
                    description = "Calisthenics & brisk walk",
                    date = todayStr,
                    isCompleted = false,
                    importance = ImportanceLevel.VERY_IMPORTANT.name,
                    customLabel = "HEALTH",
                    needsConsistency = true,
                    hasReminder = true
                ),
                TaskEntity(
                    title = "Inbox Zero & Team Sync",
                    description = "Clear urgent emails and respond to Slack threads",
                    date = todayStr,
                    isCompleted = false,
                    importance = ImportanceLevel.LOW.name,
                    customLabel = "OPS",
                    needsConsistency = false,
                    hasReminder = false
                )
            )
            dao.insertTasks(initialTasks)

            // Seed college timetable / recurring schedule items (as requested by user)
            val timetableLectures = listOf(
                TaskEntity(
                    title = "Statistics Lecture",
                    description = "Probability distributions & hypothesis testing",
                    date = todayStr,
                    isFixedEvent = true,
                    startTime = "10:00 AM",
                    endTime = "11:00 AM",
                    daysOfWeek = "MON,WED,FRI",
                    location = "Hall B - Room 204",
                    professor = "Dr. Robert Langlands",
                    customLabel = "ACADEMIC",
                    importance = ImportanceLevel.VERY_IMPORTANT.name,
                    hasReminder = true
                ),
                TaskEntity(
                    title = "Database Systems Lab",
                    description = "SQL indexing & query optimization practicals",
                    date = todayStr,
                    isFixedEvent = true,
                    startTime = "01:30 PM",
                    endTime = "03:00 PM",
                    daysOfWeek = "MON,THU",
                    location = "CS Computing Lab 3",
                    professor = "Prof. Grace Hopper",
                    customLabel = "PRACTICAL",
                    importance = ImportanceLevel.MEDIUM.name,
                    hasReminder = true
                )
            )
            dao.insertTasks(timetableLectures)

            // Seed a couple of future tasks with chronometers
            val futureCal = Calendar.getInstance()
            futureCal.add(Calendar.HOUR_OF_DAY, 6)
            futureCal.add(Calendar.MINUTE, 30)
            val futureCal2 = Calendar.getInstance()
            futureCal2.add(Calendar.DAY_OF_MONTH, 2)
            futureCal2.set(Calendar.HOUR_OF_DAY, 14)
            futureCal2.set(Calendar.MINUTE, 0)

            val futureTasks = listOf(
                TaskEntity(
                    title = "Client Project Presentation",
                    description = "Deliver Q3 roadmap demo and collect stakeholder feedback",
                    date = sdf.format(futureCal.time),
                    isCompleted = false,
                    isFutureTask = true,
                    targetDateTime = futureCal.timeInMillis,
                    importance = ImportanceLevel.VERY_IMPORTANT.name,
                    customLabel = "DEADLINE",
                    needsConsistency = false,
                    hasReminder = true
                ),
                TaskEntity(
                    title = "Quarterly Infrastructure Maintenance",
                    description = "Database index optimizations and cache flushing",
                    date = sdf.format(futureCal2.time),
                    isCompleted = false,
                    isFutureTask = true,
                    targetDateTime = futureCal2.timeInMillis,
                    importance = ImportanceLevel.MEDIUM.name,
                    customLabel = "SYSTEM",
                    needsConsistency = false,
                    hasReminder = true
                )
            )
            dao.insertTasks(futureTasks)

            // Seed historical completed tasks over the past 30 days to immediately showcase the GitHub heatmap
            val historyTasks = mutableListOf<TaskEntity>()
            val histCal = Calendar.getInstance()
            for (daysAgo in 1..45) {
                histCal.time = calendar.time
                histCal.add(Calendar.DAY_OF_MONTH, -daysAgo)
                val dayStr = sdf.format(histCal.time)
                // Vary counts based on pseudo-pattern (e.g. higher on weekdays)
                val dayOfWeek = histCal.get(Calendar.DAY_OF_WEEK)
                val count = when {
                    dayOfWeek == Calendar.SUNDAY -> if (daysAgo % 2 == 0) 1 else 0
                    dayOfWeek == Calendar.SATURDAY -> if (daysAgo % 3 == 0) 3 else 2
                    daysAgo % 7 == 0 -> 5
                    daysAgo % 5 == 0 -> 4
                    daysAgo % 3 == 0 -> 3
                    else -> 2
                }

                for (i in 1..count) {
                    val habit = if (i == 1) "Consistency: Daily Habit #$i" else "Task #$i for $dayStr"
                    historyTasks.add(
                        TaskEntity(
                            title = habit,
                            date = dayStr,
                            isCompleted = true,
                            completedAt = histCal.timeInMillis + (i * 3600000L),
                            importance = if (i == 1) ImportanceLevel.VERY_IMPORTANT.name else ImportanceLevel.MEDIUM.name,
                            customLabel = if (i == 1) "HABIT" else "WORK",
                            needsConsistency = (i == 1)
                        )
                    )
                }
            }
            dao.insertTasks(historyTasks)
        }
    }
}
