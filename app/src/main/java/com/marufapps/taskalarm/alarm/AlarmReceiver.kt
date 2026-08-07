package com.marufapps.taskalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.marufapps.taskalarm.data.SettingsRepository
import com.marufapps.taskalarm.data.TaskRepository
import com.marufapps.taskalarm.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Triggered by AlarmManager at the exact moment a task's deadline hits.
 * Shows the "deadline reached" notification, then hands off to WorkManager
 * (MissedDeadlineWorker) to check back later and fire up to 4 escalating
 * "you missed this" reminders if the task is still not completed.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val task = repository.getTaskById(taskId)
                if (task != null && !task.isCompleted) {
                    notificationHelper.showDeadlineReachedNotification(task)
                    scheduleFirstMissedCheck(context, taskId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun scheduleFirstMissedCheck(context: Context, taskId: Long) {
        // Reminder #1 should fire after the user's configured interval, not immediately.
        val intervals = settingsRepository.reminderIntervalsMinutes.first()
        val firstDelayMinutes = intervals.getOrElse(0) { 15L }

        val request = OneTimeWorkRequestBuilder<MissedDeadlineWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(MissedDeadlineWorker.KEY_TASK_ID, taskId)
                    .putInt(MissedDeadlineWorker.KEY_ATTEMPT, 1)
                    .build()
            )
            .setInitialDelay(firstDelayMinutes, java.util.concurrent.TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "missed_check_$taskId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
