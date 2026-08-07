package com.marufapps.taskalarm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY deadlineEpochMillis ASC")
    fun observeAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTaskById(id: Long): Flow<Task?>

    @Query(
        """SELECT * FROM tasks 
           WHERE isCompleted = 0 
           AND deadlineEpochMillis < :now 
           AND missedNotificationCount < :maxMissedNotifications
           AND notificationsMuted = 0"""
    )
    suspend fun getOverdueTasksNeedingNotification(
        now: Long = System.currentTimeMillis(),
        maxMissedNotifications: Int = 4
    ): List<Task>

    @Query(
        """SELECT * FROM tasks 
           WHERE isCompleted = 0 
           AND deadlineEpochMillis > :now
           ORDER BY deadlineEpochMillis ASC"""
    )
    suspend fun getPendingUpcomingTasks(now: Long = System.currentTimeMillis()): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET isCompleted = 1, completedAtEpochMillis = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, completedAt: Long = System.currentTimeMillis())

    @Query(
        """UPDATE tasks SET missedNotificationCount = missedNotificationCount + 1, 
           lastMissedNotificationEpochMillis = :firedAt WHERE id = :id"""
    )
    suspend fun incrementMissedNotificationCount(id: Long, firedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET notificationsMuted = 1 WHERE id = :id")
    suspend fun muteNotifications(id: Long)

    @Query("UPDATE tasks SET deadlineEpochMillis = :newDeadline, missedNotificationCount = 0 WHERE id = :id")
    suspend fun snooze(id: Long, newDeadline: Long)
}
