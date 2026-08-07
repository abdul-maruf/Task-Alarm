package com.marufapps.taskalarm.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marufapps.taskalarm.data.MessageTemplateResolver
import com.marufapps.taskalarm.data.SettingsRepository
import com.marufapps.taskalarm.data.TaskRepository
import com.marufapps.taskalarm.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Handles the "you missed your deadline" escalation chain.
 * Fires up to 4 notifications total, spaced according to the user's configured
 * intervals (default: 15 min, 1 hr, 3 hr, 1 day after the deadline).
 * Each run checks whether the task has since been completed or muted before
 * firing again, and re-enqueues itself for the next attempt if there is one left.
 */
@HiltWorker
class MissedDeadlineWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        val attempt = inputData.getInt(KEY_ATTEMPT, 1)
        if (taskId == -1L) return Result.failure()

        val task = repository.getTaskById(taskId) ?: return Result.success()

        // Stop the chain if the task was completed or muted since scheduling.
        if (task.isCompleted || task.notificationsMuted) {
            return Result.success()
        }

        // Fire missed-deadline notification #attempt (1 through 4), with the resolved message.
        val globalDefaults = settingsRepository.defaultReminderMessages.first()
        val resolvedMessage = MessageTemplateResolver.resolve(
            task = task,
            stage = attempt,
            globalDefault = globalDefaults.getOrNull(attempt - 1)
        )
        notificationHelper.showMissedDeadlineNotification(
            task = task,
            attempt = attempt,
            totalAttempts = MAX_ATTEMPTS,
            message = resolvedMessage
        )
        repository.incrementMissedNotificationCount(taskId)

        if (attempt >= MAX_ATTEMPTS) {
            return Result.success()
        }

        // Schedule the next escalation using the user's configured intervals.
        val intervals = settingsRepository.reminderIntervalsMinutes.first()
        val currentIntervalMin = intervals.getOrElse(attempt - 1) { 15L }
        val nextIntervalMin = intervals.getOrElse(attempt) { currentIntervalMin * 4 }
        val delayMinutes = (nextIntervalMin - currentIntervalMin).coerceAtLeast(1L)

        val nextRequest = OneTimeWorkRequestBuilder<MissedDeadlineWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(KEY_TASK_ID, taskId)
                    .putInt(KEY_ATTEMPT, attempt + 1)
                    .build()
            )
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "missed_check_$taskId",
            ExistingWorkPolicy.REPLACE,
            nextRequest
        )

        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "key_task_id"
        const val KEY_ATTEMPT = "key_attempt"
        const val MAX_ATTEMPTS = 4
    }
}
