package com.marufapps.taskalarm.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(private val dao: TaskDao) {

    fun observeAllTasks(): Flow<List<Task>> = dao.observeAllTasks()

    fun observeTaskById(id: Long): Flow<Task?> = dao.observeTaskById(id)

    suspend fun getTaskById(id: Long): Task? = dao.getTaskById(id)

    suspend fun saveTask(task: Task): Long = dao.upsert(task)

    suspend fun updateTask(task: Task) = dao.update(task)

    suspend fun deleteTask(task: Task) = dao.delete(task)

    suspend fun markCompleted(id: Long) = dao.markCompleted(id)

    suspend fun getOverdueTasksNeedingNotification(maxMissed: Int = 4) =
        dao.getOverdueTasksNeedingNotification(maxMissedNotifications = maxMissed)

    suspend fun incrementMissedNotificationCount(id: Long) = dao.incrementMissedNotificationCount(id)

    suspend fun muteNotifications(id: Long) = dao.muteNotifications(id)

    suspend fun snooze(id: Long, newDeadlineMillis: Long) = dao.snooze(id, newDeadlineMillis)
}
