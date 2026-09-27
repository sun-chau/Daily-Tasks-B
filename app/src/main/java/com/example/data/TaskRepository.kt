package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getDailyTasks(date: String): Flow<List<TaskEntity>> = taskDao.getDailyTasks(date)

    fun getTimetableForDay(dayOfWeekCode: String): Flow<List<TaskEntity>> =
        taskDao.getTimetableForDay(dayOfWeekCode)

    fun getAllTimetableEvents(): Flow<List<TaskEntity>> = taskDao.getAllTimetableEvents()

    fun getFutureTasks(): Flow<List<TaskEntity>> = taskDao.getFutureTasks()

    fun getAllCompletedTasks(): Flow<List<TaskEntity>> = taskDao.getAllCompletedTasks()

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getConsistencyTasks(): Flow<List<TaskEntity>> = taskDao.getConsistencyTasks()

    fun getPendingReminderTasks(): Flow<List<TaskEntity>> = taskDao.getPendingReminderTasks()

    suspend fun getAllRawTasks(): List<TaskEntity> = taskDao.getAllRawTasks()

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)

    suspend fun toggleTaskCompleted(task: TaskEntity) {
        val updated = task.copy(
            isCompleted = !task.isCompleted,
            completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)
    }

    suspend fun reseedSampleData() {
        taskDao.clearAllTasks()
        AppDatabase.seedInitialTasks(taskDao)
    }

    suspend fun clearAll() {
        taskDao.clearAllTasks()
    }
}
