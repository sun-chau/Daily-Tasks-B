package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ImportanceLevel {
    VERY_IMPORTANT,
    MEDIUM,
    LOW
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val date: String, // Format: YYYY-MM-DD (e.g. 2026-09-20)
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isExtraTask: Boolean = false, // "add a task if you did it in extra that day"
    val isFutureTask: Boolean = false, // "enter a future task to do"
    val targetDateTime: Long? = null, // epoch millis for future task scheduled time
    val importance: String = ImportanceLevel.MEDIUM.name,
    val customLabel: String = "", // custom label for tags
    val needsConsistency: Boolean = false, // "Reminder will only be for those tasks that needs constant consistency"
    val hasReminder: Boolean = false, // reminder enabled
    val createdAt: Long = System.currentTimeMillis(),
    val isFixedEvent: Boolean = false, // Timetable slot / college lecture / fixed event that happens automatically
    val startTime: String = "", // e.g. "10:00 AM" or "10:00"
    val endTime: String = "", // e.g. "11:00 AM" or "11:00"
    val daysOfWeek: String = "", // e.g. "MON,WED,FRI" or empty if single-day event
    val location: String = "", // e.g. "Hall B", "Lab 4", "Room 101"
    val professor: String = "", // e.g. "Dr. Alan Turing"
    val isHolidayCancelled: Boolean = false, // Temporary Holiday / No class toggle for this lecture
    val attendedCount: Int = 0, // Total times lecture was attended
    val missedCount: Int = 0, // Total times lecture was missed
    val isStreakFreeze: Boolean = false, // Streak freeze / grace day record
    val freezeReason: String = "" // Reason/description required for streak freeze/grace
)
