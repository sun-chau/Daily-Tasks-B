package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ImportanceLevel
import com.example.data.TaskEntity
import com.example.data.TaskRepository
import com.example.receiver.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ProductivityMetrics(
    val totalCompletedAllTime: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val thisWeekCompleted: Int = 0,
    val lastWeekCompleted: Int = 0,
    val thisMonthCompleted: Int = 0,
    val consistencyRatePercent: Int = 0,
    val dailyCounts: Map<String, Int> = emptyMap(),
    val streakFreezeDates: Set<String> = emptySet()
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    private val prefs = application.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfFull = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val sdfDayOfWeek = SimpleDateFormat("EEE", Locale.US)

    // Dynamic date state observed from system or midnight rollover
    private val _currentDate = MutableStateFlow(Date())
    val currentDate: StateFlow<Date> = _currentDate.asStateFlow()

    val todayDateString: String
        get() = sdfDate.format(_currentDate.value)

    val todayFullHeading: String
        get() = sdfFull.format(_currentDate.value)

    val todayDayOfWeek: String
        get() = sdfDayOfWeek.format(_currentDate.value).uppercase(Locale.US)

    // Theme state: default Light (false) as requested
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // High contrast mode
    private val _highContrastEnabled = MutableStateFlow(prefs.getBoolean("high_contrast_enabled", false))
    val highContrastEnabled: StateFlow<Boolean> = _highContrastEnabled.asStateFlow()

    // Reminders state
    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean("reminders_enabled", true))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    // Daily consistency reminder hour (default 20 = 8 PM)
    private val _reminderHour = MutableStateFlow(prefs.getInt("reminder_hour", 20))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(prefs.getInt("reminder_minute", 0))
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    // Pre-class alerts (e.g. 15 minutes before)
    private val _preClassAlertsEnabled = MutableStateFlow(prefs.getBoolean("pre_class_alerts_enabled", true))
    val preClassAlertsEnabled: StateFlow<Boolean> = _preClassAlertsEnabled.asStateFlow()

    // Pre-class alert lead time (in minutes: 5, 10, 15, 30)
    private val _preClassLeadTimeMinutes = MutableStateFlow(prefs.getInt("pre_class_lead_time_min", 15))
    val preClassLeadTimeMinutes: StateFlow<Int> = _preClassLeadTimeMinutes.asStateFlow()

    // Weekly tasks goal target (default: 20)
    private val _weeklyGoal = MutableStateFlow(prefs.getInt("weekly_goal_target", 20))
    val weeklyGoal: StateFlow<Int> = _weeklyGoal.asStateFlow()

    // Haptics / Tactile state
    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("haptics_enabled", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    // Haptic Intensity (LOW, MEDIUM, HIGH)
    private val _hapticIntensity = MutableStateFlow(prefs.getString("haptic_intensity", "MEDIUM") ?: "MEDIUM")
    val hapticIntensity: StateFlow<String> = _hapticIntensity.asStateFlow()

    // Task Sort Order state (DEFAULT, IMPORTANCE, CHRONOLOGICAL, STATUS)
    private val _taskSortOrder = MutableStateFlow(prefs.getString("task_sort_order", "DEFAULT") ?: "DEFAULT")
    val taskSortOrder: StateFlow<String> = _taskSortOrder.asStateFlow()

    // College schedule configuration: College start/end hours (e.g. 08:30 to 17:00)
    private val _collegeStartTime = MutableStateFlow(prefs.getString("college_start_time", "08:30 AM") ?: "08:30 AM")
    val collegeStartTime: StateFlow<String> = _collegeStartTime.asStateFlow()

    private val _collegeEndTime = MutableStateFlow(prefs.getString("college_end_time", "05:00 PM") ?: "05:00 PM")
    val collegeEndTime: StateFlow<String> = _collegeEndTime.asStateFlow()

    // Attendance threshold warning percentage (e.g. 75%)
    private val _attendanceThresholdPercent = MutableStateFlow(prefs.getInt("attendance_threshold_pct", 75))
    val attendanceThresholdPercent: StateFlow<Int> = _attendanceThresholdPercent.asStateFlow()

    // College Weekend Days (e.g. SAT, SUN)
    private val _collegeWeekendDays = MutableStateFlow(
        prefs.getStringSet("college_weekend_days", setOf("SAT", "SUN"))?.toSet() ?: setOf("SAT", "SUN")
    )
    val collegeWeekendDays: StateFlow<Set<String>> = _collegeWeekendDays.asStateFlow()

    // Notification Quiet Hours / DND during class
    private val _quietHoursEnabled = MutableStateFlow(prefs.getBoolean("quiet_hours_enabled", false))
    val quietHoursEnabled: StateFlow<Boolean> = _quietHoursEnabled.asStateFlow()

    private val _quietHoursStart = MutableStateFlow(prefs.getString("quiet_hours_start", "22:00") ?: "22:00")
    val quietHoursStart: StateFlow<String> = _quietHoursStart.asStateFlow()

    private val _quietHoursEnd = MutableStateFlow(prefs.getString("quiet_hours_end", "07:00") ?: "07:00")
    val quietHoursEnd: StateFlow<String> = _quietHoursEnd.asStateFlow()

    // Semester Start & End Dates
    private val _semesterStartDate = MutableStateFlow(prefs.getString("semester_start_date", "2025-01-15") ?: "2025-01-15")
    val semesterStartDate: StateFlow<String> = _semesterStartDate.asStateFlow()

    private val _semesterEndDate = MutableStateFlow(prefs.getString("semester_end_date", "2025-05-30") ?: "2025-05-30")
    val semesterEndDate: StateFlow<String> = _semesterEndDate.asStateFlow()

    // Visual preferences: Timeline track, color-coded subjects, horizontal heatmap
    private val _timelineTrackEnabled = MutableStateFlow(prefs.getBoolean("timeline_track_enabled", true))
    val timelineTrackEnabled: StateFlow<Boolean> = _timelineTrackEnabled.asStateFlow()

    private val _colorCodedSubjectsEnabled = MutableStateFlow(prefs.getBoolean("color_coded_subjects_enabled", true))
    val colorCodedSubjectsEnabled: StateFlow<Boolean> = _colorCodedSubjectsEnabled.asStateFlow()

    private val _horizontalHeatMapEnabled = MutableStateFlow(prefs.getBoolean("horizontal_heat_map_enabled", true))
    val horizontalHeatMapEnabled: StateFlow<Boolean> = _horizontalHeatMapEnabled.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
    }

    fun refreshDateRollover() {
        _currentDate.value = Date()
    }

    val dailyTasks: StateFlow<List<TaskEntity>> = _currentDate
        .flatMapLatest { date ->
            repository.getDailyTasks(sdfDate.format(date))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTimetable: StateFlow<List<TaskEntity>> = _currentDate
        .flatMapLatest { date ->
            val dayOfWeekStr = sdfDayOfWeek.format(date).uppercase(Locale.US)
            repository.getTimetableForDay(dayOfWeekStr).map { list ->
                list.filter { event ->
                    event.daysOfWeek.split(",").map { it.trim().uppercase(Locale.US) }
                        .any { it == dayOfWeekStr || it == "ALL" } || event.daysOfWeek.isBlank()
                }.sortedBy { parseTimeToMinutes(it.startTime) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTimetableEvents: StateFlow<List<TaskEntity>> = repository.getAllTimetableEvents()
        .map { list -> list.sortedBy { parseTimeToMinutes(it.startTime) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val futureTasks: StateFlow<List<TaskEntity>> = repository.getFutureTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletedTasks: StateFlow<List<TaskEntity>> = repository.getAllCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productivityMetrics: StateFlow<ProductivityMetrics> = allCompletedTasks
        .combine(allTasks) { completed, all ->
            calculateMetrics(completed, all)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductivityMetrics())

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task)
            com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
            if (_hapticsEnabled.value) {
                if (!task.isCompleted) {
                    com.example.util.FeedbackManager.performTaskCompletedFeedback(getApplication())
                } else {
                    com.example.util.FeedbackManager.performClickFeedback(getApplication())
                }
            }
        }
    }

    fun moveFutureTaskToToday(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                date = todayDateString,
                isFutureTask = false,
                targetDateTime = null
            )
            repository.updateTask(updated)
            com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun addDailyTask(
        title: String,
        description: String = "",
        importance: String = ImportanceLevel.MEDIUM.name,
        customLabel: String = "",
        needsConsistency: Boolean = false,
        isExtraTask: Boolean = false
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                date = todayDateString,
                isCompleted = false,
                isExtraTask = isExtraTask,
                isFutureTask = false,
                importance = importance,
                customLabel = customLabel.trim().uppercase(Locale.getDefault()),
                needsConsistency = needsConsistency,
                hasReminder = needsConsistency && _remindersEnabled.value
            )
            val id = repository.insertTask(task)
            com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
            if (needsConsistency && _remindersEnabled.value) {
                ReminderScheduler.scheduleTaskReminder(getApplication(), task.copy(id = id))
            }
        }
    }

    fun addFutureTask(
        title: String,
        description: String = "",
        targetEpochMillis: Long,
        importance: String = ImportanceLevel.MEDIUM.name,
        customLabel: String = "",
        hasReminder: Boolean = true
    ) {
        viewModelScope.launch {
            val futureDate = sdfDate.format(Date(targetEpochMillis))
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                date = futureDate,
                isCompleted = false,
                isExtraTask = false,
                isFutureTask = true,
                targetDateTime = targetEpochMillis,
                importance = importance,
                customLabel = customLabel.trim().uppercase(Locale.getDefault()),
                needsConsistency = false,
                hasReminder = hasReminder && _remindersEnabled.value
            )
            val id = repository.insertTask(task)
            com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
            if (hasReminder && _remindersEnabled.value) {
                ReminderScheduler.scheduleTaskReminder(getApplication(), task.copy(id = id))
            }
        }
    }

    fun addTimetableEvent(
        title: String,
        description: String = "",
        startTime: String,
        endTime: String,
        daysOfWeek: String = "ALL",
        location: String = "",
        professor: String = "",
        customLabel: String = "LECTURE",
        hasReminder: Boolean = true
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                date = todayDateString,
                isCompleted = false,
                isExtraTask = false,
                isFutureTask = false,
                isFixedEvent = true,
                startTime = formatTimeToStandard(startTime),
                endTime = formatTimeToStandard(endTime),
                daysOfWeek = daysOfWeek.trim().uppercase(Locale.getDefault()),
                location = location.trim(),
                professor = professor.trim(),
                importance = ImportanceLevel.VERY_IMPORTANT.name,
                customLabel = customLabel.trim().uppercase(Locale.getDefault()),
                needsConsistency = false,
                hasReminder = hasReminder && _remindersEnabled.value
            )
            repository.insertTask(task)
            com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
            com.example.widget.TimetableWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun updateTimetableEvent(event: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(event.copy(
                startTime = formatTimeToStandard(event.startTime),
                endTime = formatTimeToStandard(event.endTime)
            ))
            com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
            com.example.widget.TimetableWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun markAttendance(event: TaskEntity, attended: Boolean) {
        viewModelScope.launch {
            val updated = if (attended) {
                event.copy(attendedCount = event.attendedCount + 1)
            } else {
                event.copy(missedCount = event.missedCount + 1)
            }
            repository.updateTask(updated)
            if (_hapticsEnabled.value) {
                com.example.util.FeedbackManager.performTaskCompletedFeedback(getApplication())
            }
        }
    }

    fun toggleHolidayCancellation(event: TaskEntity) {
        viewModelScope.launch {
            val updated = event.copy(isHolidayCancelled = !event.isHolidayCancelled)
            repository.updateTask(updated)
            com.example.widget.TimetableWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun copyTimetableDay(sourceDay: String, targetDay: String) {
        viewModelScope.launch {
            val currentEvents = allTimetableEvents.value
            val eventsToCopy = currentEvents.filter {
                it.daysOfWeek.contains(sourceDay, ignoreCase = true) || it.daysOfWeek == "ALL"
            }
            eventsToCopy.forEach { ev ->
                val newDays = if (ev.daysOfWeek.isBlank() || ev.daysOfWeek == "ALL") {
                    targetDay
                } else {
                    val daysList = ev.daysOfWeek.split(",").map { it.trim() }.toMutableSet()
                    daysList.add(targetDay)
                    daysList.joinToString(",")
                }
                repository.updateTask(ev.copy(daysOfWeek = newDays))
            }
            com.example.widget.TimetableWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun addStreakFreeze(dateStr: String, reason: String) {
        viewModelScope.launch {
            val freezeTask = TaskEntity(
                title = "Streak Freeze / Sick Grace Day: $reason",
                description = reason,
                date = dateStr,
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
                isStreakFreeze = true,
                freezeReason = reason,
                customLabel = "FREEZE",
                importance = ImportanceLevel.LOW.name
            )
            repository.insertTask(freezeTask)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
            if (task.isFixedEvent) {
                com.example.widget.TimetableWidgetProvider.triggerUpdate(getApplication())
            }
            ReminderScheduler.cancelReminder(getApplication(), task.id)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun setHighContrastEnabled(enabled: Boolean) {
        _highContrastEnabled.value = enabled
        prefs.edit().putBoolean("high_contrast_enabled", enabled).apply()
    }

    fun setRemindersEnabled(enabled: Boolean) {
        _remindersEnabled.value = enabled
        prefs.edit().putBoolean("reminders_enabled", enabled).apply()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
        prefs.edit().putInt("reminder_hour", hour).putInt("reminder_minute", minute).apply()
    }

    fun setPreClassAlertsEnabled(enabled: Boolean) {
        _preClassAlertsEnabled.value = enabled
        prefs.edit().putBoolean("pre_class_alerts_enabled", enabled).apply()
    }

    fun setPreClassLeadTimeMinutes(minutes: Int) {
        _preClassLeadTimeMinutes.value = minutes
        prefs.edit().putInt("pre_class_lead_time_min", minutes).apply()
    }

    fun setWeeklyGoal(goal: Int) {
        val safeGoal = if (goal > 0) goal else 10
        _weeklyGoal.value = safeGoal
        prefs.edit().putInt("weekly_goal_target", safeGoal).apply()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
    }

    fun setHapticIntensity(intensity: String) {
        _hapticIntensity.value = intensity
        prefs.edit().putString("haptic_intensity", intensity).apply()
    }

    fun setCollegeScheduleConfig(start: String, end: String) {
        _collegeStartTime.value = start
        _collegeEndTime.value = end
        prefs.edit().putString("college_start_time", start).putString("college_end_time", end).apply()
    }

    fun setAttendanceThreshold(threshold: Int) {
        _attendanceThresholdPercent.value = threshold
        prefs.edit().putInt("attendance_threshold_pct", threshold).apply()
    }

    fun toggleCollegeWeekendDay(day: String) {
        val current = _collegeWeekendDays.value.toMutableSet()
        if (current.contains(day)) {
            current.remove(day)
        } else {
            current.add(day)
        }
        _collegeWeekendDays.value = current
        prefs.edit().putStringSet("college_weekend_days", current).apply()
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        _quietHoursEnabled.value = enabled
        prefs.edit().putBoolean("quiet_hours_enabled", enabled).apply()
    }

    fun setQuietHoursRange(start: String, end: String) {
        _quietHoursStart.value = start
        _quietHoursEnd.value = end
        prefs.edit().putString("quiet_hours_start", start).putString("quiet_hours_end", end).apply()
    }

    fun setSemesterDates(start: String, end: String) {
        _semesterStartDate.value = start
        _semesterEndDate.value = end
        prefs.edit().putString("semester_start_date", start).putString("semester_end_date", end).apply()
    }

    fun setTimelineTrackEnabled(enabled: Boolean) {
        _timelineTrackEnabled.value = enabled
        prefs.edit().putBoolean("timeline_track_enabled", enabled).apply()
    }

    fun setColorCodedSubjectsEnabled(enabled: Boolean) {
        _colorCodedSubjectsEnabled.value = enabled
        prefs.edit().putBoolean("color_coded_subjects_enabled", enabled).apply()
    }

    fun setHorizontalHeatMapEnabled(enabled: Boolean) {
        _horizontalHeatMapEnabled.value = enabled
        prefs.edit().putBoolean("horizontal_heat_map_enabled", enabled).apply()
    }

    fun remindTomorrowEarly(task: TaskEntity) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowStr = sdfDate.format(cal.time)
            val updated = task.copy(
                date = tomorrowStr,
                customLabel = if (task.customLabel.isNotBlank()) "${task.customLabel} • RESCHEDULED" else "RESCHEDULED"
            )
            repository.updateTask(updated)
            com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun updateLectureNotes(event: TaskEntity, notes: String) {
        viewModelScope.launch {
            val updated = event.copy(description = notes)
            repository.updateTask(updated)
        }
    }

    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val allTasks = repository.getAllRawTasks()
        val jsonArray = JSONArray()
        for (t in allTasks) {
            val obj = JSONObject()
            obj.put("title", t.title)
            obj.put("description", t.description)
            obj.put("date", t.date)
            obj.put("isCompleted", t.isCompleted)
            obj.put("isExtraTask", t.isExtraTask)
            obj.put("isFutureTask", t.isFutureTask)
            obj.put("isFixedEvent", t.isFixedEvent)
            obj.put("startTime", t.startTime)
            obj.put("endTime", t.endTime)
            obj.put("daysOfWeek", t.daysOfWeek)
            obj.put("location", t.location)
            obj.put("professor", t.professor)
            obj.put("isHolidayCancelled", t.isHolidayCancelled)
            obj.put("attendedCount", t.attendedCount)
            obj.put("missedCount", t.missedCount)
            obj.put("isStreakFreeze", t.isStreakFreeze)
            obj.put("freezeReason", t.freezeReason)
            obj.put("targetDateTime", t.targetDateTime ?: 0L)
            obj.put("importance", t.importance)
            obj.put("customLabel", t.customLabel)
            obj.put("needsConsistency", t.needsConsistency)
            obj.put("hasReminder", t.hasReminder)
            obj.put("createdAt", t.createdAt)
            obj.put("completedAt", t.completedAt ?: 0L)
            jsonArray.put(obj)
        }
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("totalCount", allTasks.size)
        root.put("tasks", jsonArray)
        root.toString(2)
    }

    suspend fun importDataFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            val array = root.getJSONArray("tasks")
            val tasksList = mutableListOf<TaskEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val task = TaskEntity(
                    id = 0L,
                    title = obj.optString("title", ""),
                    description = obj.optString("description", ""),
                    date = obj.optString("date", todayDateString),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    isExtraTask = obj.optBoolean("isExtraTask", false),
                    isFutureTask = obj.optBoolean("isFutureTask", false),
                    isFixedEvent = obj.optBoolean("isFixedEvent", false),
                    startTime = obj.optString("startTime", ""),
                    endTime = obj.optString("endTime", ""),
                    daysOfWeek = obj.optString("daysOfWeek", "ALL"),
                    location = obj.optString("location", ""),
                    professor = obj.optString("professor", ""),
                    isHolidayCancelled = obj.optBoolean("isHolidayCancelled", false),
                    attendedCount = obj.optInt("attendedCount", 0),
                    missedCount = obj.optInt("missedCount", 0),
                    isStreakFreeze = obj.optBoolean("isStreakFreeze", false),
                    freezeReason = obj.optString("freezeReason", ""),
                    targetDateTime = if (obj.has("targetDateTime") && obj.getLong("targetDateTime") > 0L) obj.getLong("targetDateTime") else null,
                    importance = obj.optString("importance", "MEDIUM"),
                    customLabel = obj.optString("customLabel", ""),
                    needsConsistency = obj.optBoolean("needsConsistency", false),
                    hasReminder = obj.optBoolean("hasReminder", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    completedAt = if (obj.has("completedAt") && obj.getLong("completedAt") > 0L) obj.getLong("completedAt") else null
                )
                tasksList.add(task)
            }
            if (tasksList.isNotEmpty()) {
                tasksList.forEach { repository.insertTask(it) }
                com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
                com.example.widget.TimetableWidgetProvider.triggerUpdate(getApplication())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun setTaskSortOrder(order: String) {
        _taskSortOrder.value = order
        prefs.edit().putString("task_sort_order", order).apply()
    }

    fun reseedSampleData() {
        viewModelScope.launch {
            repository.reseedSampleData()
            com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
            com.example.widget.TimetableWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            com.example.widget.TaskWidgetProvider.triggerUpdate(getApplication())
            com.example.widget.TimetableWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun triggerTestNotification() {
        ReminderScheduler.triggerImmediateNotification(
            getApplication(),
            9999L,
            "Consistency Habit Reminder: Keep your daily streak unbroken!",
            isConsistency = true,
            customLabel = "TEST NOTIFICATION"
        )
    }

    private fun calculateMetrics(completed: List<TaskEntity>, all: List<TaskEntity>): ProductivityMetrics {
        val dailyMap = mutableMapOf<String, Int>()
        val freezeDates = mutableSetOf<String>()

        for (task in completed) {
            dailyMap[task.date] = (dailyMap[task.date] ?: 0) + 1
            if (task.isStreakFreeze) {
                freezeDates.add(task.date)
            }
        }

        var currentStreak = 0
        var bestStreak = 0
        var runningStreak = 0

        val cal = Calendar.getInstance()
        val checkDate = Calendar.getInstance()

        var isCurrentStreakAlive = true
        for (i in 0..365) {
            checkDate.time = cal.time
            checkDate.add(Calendar.DAY_OF_MONTH, -i)
            val dateStr = sdfDate.format(checkDate.time)
            val count = dailyMap[dateStr] ?: 0
            val isFrozen = freezeDates.contains(dateStr)

            if (count > 0 || isFrozen) {
                if (isCurrentStreakAlive) currentStreak++
                runningStreak++
                if (runningStreak > bestStreak) bestStreak = runningStreak
            } else {
                if (i > 0) isCurrentStreakAlive = false
                runningStreak = 0
            }
        }

        val calWeek = Calendar.getInstance()
        calWeek.firstDayOfWeek = Calendar.MONDAY
        calWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calWeek.set(Calendar.HOUR_OF_DAY, 0)
        calWeek.set(Calendar.MINUTE, 0)
        val startOfThisWeek = calWeek.timeInMillis

        calWeek.add(Calendar.WEEK_OF_YEAR, -1)
        val startOfLastWeek = calWeek.timeInMillis

        var thisWeekCount = 0
        var lastWeekCount = 0
        var thisMonthCount = 0

        val calMonth = Calendar.getInstance()
        calMonth.set(Calendar.DAY_OF_MONTH, 1)
        calMonth.set(Calendar.HOUR_OF_DAY, 0)
        calMonth.set(Calendar.MINUTE, 0)
        val startOfThisMonth = calMonth.timeInMillis

        for (task in completed) {
            val ts = task.completedAt ?: task.createdAt
            if (ts >= startOfThisWeek) {
                thisWeekCount++
            } else if (ts >= startOfLastWeek && ts < startOfThisWeek) {
                lastWeekCount++
            }
            if (ts >= startOfThisMonth) {
                thisMonthCount++
            }
        }

        val totalDailyTasks = all.filter { !it.isFutureTask }.size
        val consistencyRate = if (totalDailyTasks > 0) {
            ((completed.size.toFloat() / totalDailyTasks.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            100
        }

        return ProductivityMetrics(
            totalCompletedAllTime = completed.size,
            currentStreakDays = currentStreak,
            bestStreakDays = maxOf(bestStreak, currentStreak),
            thisWeekCompleted = thisWeekCount,
            lastWeekCompleted = lastWeekCount,
            thisMonthCompleted = thisMonthCount,
            consistencyRatePercent = consistencyRate,
            dailyCounts = dailyMap,
            streakFreezeDates = freezeDates
        )
    }

    companion object {
        fun formatTimeToStandard(timeStr: String): String {
            if (timeStr.isBlank()) return ""
            val clean = timeStr.trim().uppercase(Locale.getDefault())
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val digitsPart = clean.replace("AM", "").replace("PM", "").trim()
            val parts = digitsPart.split(":")
            var hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return timeStr
            val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val amPm = if (hour >= 12) "PM" else "AM"
            return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
        }

        fun parseTimeToMinutes(timeStr: String): Int {
            if (timeStr.isBlank()) return Int.MAX_VALUE
            val clean = timeStr.trim().uppercase(Locale.getDefault())
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val digitsPart = clean.replace("AM", "").replace("PM", "").trim()
            val parts = digitsPart.split(":")
            val rawHour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return Int.MAX_VALUE
            val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            var hour = rawHour
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            return hour * 60 + minute
        }
    }
}
