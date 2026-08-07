package com.marufapps.taskalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marufapps.taskalarm.data.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Android clears all AlarmManager alarms on reboot, so we must re-register
 * every pending task's deadline alarm here.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                rescheduleAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAll(context: Context) {
        val allTasks = repository.observeAllTasks()
        // Take the latest snapshot once (first emission) rather than collecting forever.
        val tasks = allTasks.first()
        tasks.filter { !it.isCompleted }.forEach { task ->
            if (task.deadlineEpochMillis > System.currentTimeMillis()) {
                alarmScheduler.scheduleDeadlineAlarm(task)
            }
            // Missed tasks that still owe notifications will be picked up by
            // MissedDeadlineWorker's next natural check, or immediately re-triggered here:
            else if (task.missedNotificationCount < MissedDeadlineWorker.MAX_ATTEMPTS && !task.notificationsMuted) {
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "missed_check_${task.id}",
                    androidx.work.ExistingWorkPolicy.KEEP,
                    androidx.work.OneTimeWorkRequestBuilder<MissedDeadlineWorker>()
                        .setInputData(
                            androidx.work.Data.Builder()
                                .putLong(MissedDeadlineWorker.KEY_TASK_ID, task.id)
                                .putInt(MissedDeadlineWorker.KEY_ATTEMPT, task.missedNotificationCount + 1)
                                .build()
                        )
                        .build()
                )
            }
        }
    }
}
