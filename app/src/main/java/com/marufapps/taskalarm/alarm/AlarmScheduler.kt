package com.marufapps.taskalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.marufapps.taskalarm.data.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps AlarmManager to schedule/cancel the exact-time alarm that fires when a
 * task's deadline is reached. The escalation chain of 4 "missed" notifications
 * is handled separately by MissedDeadlineWorker, which this alarm kicks off.
 */
@Singleton
class AlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    fun scheduleDeadlineAlarm(task: Task) {
        if (task.isCompleted) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DEADLINE_REACHED
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForTask(task.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.deadlineEpochMillis,
                pendingIntent
            )
        } else {
            // Fallback: inexact alarm if user hasn't granted exact-alarm permission
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.deadlineEpochMillis,
                pendingIntent
            )
        }
    }

    fun cancelDeadlineAlarm(taskId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DEADLINE_REACHED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForTask(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val ACTION_DEADLINE_REACHED = "com.marufapps.taskalarm.ACTION_DEADLINE_REACHED"
        const val EXTRA_TASK_ID = "extra_task_id"

        fun requestCodeForTask(taskId: Long): Int = (1_000_000 + taskId).toInt()
    }
}
