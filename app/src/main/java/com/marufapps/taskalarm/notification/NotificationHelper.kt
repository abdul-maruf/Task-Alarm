package com.marufapps.taskalarm.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.marufapps.taskalarm.MainActivity
import com.marufapps.taskalarm.R
import com.marufapps.taskalarm.data.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val dueChannel = NotificationChannel(
            CHANNEL_DUE,
            "Task Deadlines",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies you the moment a task's deadline arrives"
            enableVibration(true)
        }

        val missedChannel = NotificationChannel(
            CHANNEL_MISSED,
            "Missed Deadlines",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Escalating reminders for tasks you missed the deadline on"
            enableVibration(true)
            enableLights(true)
            lightColor = Color.RED
        }

        notificationManager.createNotificationChannel(dueChannel)
        notificationManager.createNotificationChannel(missedChannel)
    }

    fun showDeadlineReachedNotification(task: Task) {
        val contentIntent = openTaskPendingIntent(task.id)

        val notification = NotificationCompat.Builder(context, CHANNEL_DUE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Deadline reached: ${task.title}")
            .setContentText(task.description.ifBlank { "This task is now due." })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(markDoneAction(task.id, 0))
            .addAction(snoozeAction(task.id, 0))
            .build()

        notificationManager.notify(notificationIdForDue(task.id), notification)
    }

    fun showMissedDeadlineNotification(task: Task, attempt: Int, totalAttempts: Int, message: String) {
        val contentIntent = openTaskPendingIntent(task.id)

        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ ${task.title}")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSubText("Reminder $attempt of $totalAttempts")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setColor(Color.RED)
            // Increase perceived urgency slightly on later attempts
            .setVibrate(vibrationPatternFor(attempt))
            .addAction(markDoneAction(task.id, attempt))
            .addAction(snoozeAction(task.id, attempt))
            .build()

        notificationManager.notify(notificationIdForMissed(task.id, attempt), notification)
    }

    private fun vibrationPatternFor(attempt: Int): LongArray = when (attempt) {
        1 -> longArrayOf(0, 250, 250, 250)
        2 -> longArrayOf(0, 400, 200, 400)
        3 -> longArrayOf(0, 500, 150, 500, 150, 500)
        else -> longArrayOf(0, 700, 100, 700, 100, 700, 100, 700)
    }

    private fun openTaskPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_task_id", taskId)
        }
        return PendingIntent.getActivity(
            context, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun markDoneAction(taskId: Long, attempt: Int): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_DONE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, (taskId * 10 + attempt).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, "Mark Done", pendingIntent).build()
    }

    private fun snoozeAction(taskId: Long, attempt: Int): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, (taskId * 100 + attempt).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, "Snooze", pendingIntent).build()
    }

    private fun notificationIdForDue(taskId: Long) = (taskId + 1_000_000).toInt()
    private fun notificationIdForMissed(taskId: Long, attempt: Int) = (taskId * 10 + attempt + 2_000_000).toInt()

    /** Clears any currently-shown notifications (due + missed 1-4) for a task. */
    fun cancelAllForTask(taskId: Long) {
        notificationManager.cancel(notificationIdForDue(taskId))
        for (attempt in 0..4) {
            notificationManager.cancel(notificationIdForMissed(taskId, attempt))
        }
    }

    companion object {
        const val CHANNEL_DUE = "channel_due"
        const val CHANNEL_MISSED = "channel_missed"
    }
}
