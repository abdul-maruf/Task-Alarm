package com.marufapps.taskalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class Priority { LOW, MEDIUM, HIGH }

enum class TaskStatus { UPCOMING, DUE_SOON, MISSED, COMPLETED }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val priority: Priority = Priority.MEDIUM,
    val deadlineEpochMillis: Long,
    val isCompleted: Boolean = false,
    val completedAtEpochMillis: Long? = null,
    // How many of the 4 post-deadline "missed" notifications have fired so far.
    val missedNotificationCount: Int = 0,
    // Timestamp of the most recently fired missed-notification, used to schedule the next one.
    val lastMissedNotificationEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    // If true, alarms/notifications are silenced for this task even if overdue.
    val notificationsMuted: Boolean = false,
    // Optional per-task override for each of the 4 missed-deadline reminder messages.
    // Null/blank means "fall back to the global default template for that stage" (see SettingsRepository).
    // Supports {task} and {hours} placeholders, resolved at notification time.
    val reminderMessage1: String? = null,
    val reminderMessage2: String? = null,
    val reminderMessage3: String? = null,
    val reminderMessage4: String? = null
) {
    val deadlineInstant: Instant get() = Instant.ofEpochMilli(deadlineEpochMillis)

    /** Returns this task's custom message for a given stage (1-4), or null if not set. */
    fun customMessageForStage(stage: Int): String? = when (stage) {
        1 -> reminderMessage1
        2 -> reminderMessage2
        3 -> reminderMessage3
        4 -> reminderMessage4
        else -> null
    }?.takeIf { it.isNotBlank() }

    fun computedStatus(now: Long = System.currentTimeMillis()): TaskStatus {
        if (isCompleted) return TaskStatus.COMPLETED
        return when {
            now > deadlineEpochMillis -> TaskStatus.MISSED
            deadlineEpochMillis - now <= 60 * 60 * 1000L -> TaskStatus.DUE_SOON
            else -> TaskStatus.UPCOMING
        }
    }
}
