package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE (date = :date OR (isFutureTask = 1 AND date = :date)) AND isFixedEvent = 0 ORDER BY isCompleted ASC, CASE importance WHEN 'VERY_IMPORTANT' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END ASC, id DESC")
    fun getDailyTasks(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isFixedEvent = 1 AND (daysOfWeek LIKE '%' || :dayOfWeekCode || '%' OR daysOfWeek = 'ALL') ORDER BY startTime ASC, id ASC")
    fun getTimetableForDay(dayOfWeekCode: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isFixedEvent = 1 ORDER BY startTime ASC, id ASC")
    fun getAllTimetableEvents(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isFutureTask = 1 AND isFixedEvent = 0 ORDER BY isCompleted ASC, targetDateTime ASC, id ASC")
    fun getFutureTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND isFixedEvent = 0")
    fun getAllCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isFixedEvent = 0 ORDER BY date DESC, id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE needsConsistency = 1 AND isFutureTask = 0")
    fun getConsistencyTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (needsConsistency = 1 OR isFutureTask = 1) AND hasReminder = 1 AND isCompleted = 0")
    fun getPendingReminderTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllRawTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}
