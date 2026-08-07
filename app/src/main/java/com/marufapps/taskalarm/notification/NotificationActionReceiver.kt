package com.marufapps.taskalarm.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.marufapps.taskalarm.alarm.AlarmScheduler
import com.marufapps.taskalarm.data.SettingsRepository
import com.marufapps.taskalarm.data.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return
        val pendingResult = goAsync()

        scope.launch {
            try {
                when (intent.action) {
                    ACTION_MARK_DONE -> {
                        repository.markCompleted(taskId)
                        alarmScheduler.cancelDeadlineAlarm(taskId)
                        WorkManager.getInstance(context).cancelUniqueWork("missed_check_$taskId")
                        clearNotifications(context, taskId)
                    }
                    ACTION_SNOOZE -> {
                        val snoozeMinutes = settingsRepository.snoozeDurationMinutes.first()
                        val newDeadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(snoozeMinutes)
                        repository.snooze(taskId, newDeadline)
                        val updated = repository.getTaskById(taskId)
                        if (updated != null) alarmScheduler.scheduleDeadlineAlarm(updated)
                        WorkManager.getInstance(context).cancelUniqueWork("missed_check_$taskId")
                        clearNotifications(context, taskId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun clearNotifications(context: Context, taskId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel((taskId + 1_000_000).toInt())
        for (attempt in 0..4) {
            nm.cancel((taskId * 10 + attempt + 2_000_000).toInt())
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.marufapps.taskalarm.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.marufapps.taskalarm.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
