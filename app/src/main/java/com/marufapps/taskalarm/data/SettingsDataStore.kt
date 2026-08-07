package com.marufapps.taskalarm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "task_alarm_settings")

/**
 * Default escalation schedule for the 4 post-deadline "missed" notifications:
 * 15 minutes, 1 hour, 3 hours, 1 day after the deadline was missed.
 * User can customize these in Settings.
 */
object DefaultIntervals {
    val MINUTES = listOf(15L, 60L, 180L, 1440L)
}

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val intervalsKey = stringPreferencesKey("missed_reminder_intervals_minutes")
    private val snoozeKey = longPreferencesKey("snooze_duration_minutes")
    private val soundKey = stringPreferencesKey("notification_sound_uri")

    // Stored individually (not comma-joined) since message text can itself contain commas.
    private val defaultMessageKeys = listOf(
        stringPreferencesKey("default_reminder_message_1"),
        stringPreferencesKey("default_reminder_message_2"),
        stringPreferencesKey("default_reminder_message_3"),
        stringPreferencesKey("default_reminder_message_4")
    )

    val reminderIntervalsMinutes: Flow<List<Long>> = context.dataStore.data.map { prefs ->
        prefs[intervalsKey]?.split(",")?.mapNotNull { it.toLongOrNull() }
            ?: DefaultIntervals.MINUTES
    }

    val snoozeDurationMinutes: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[snoozeKey] ?: 30L
    }

    /** The 4 global default reminder messages, falling back to built-in defaults if unset. */
    val defaultReminderMessages: Flow<List<String>> = context.dataStore.data.map { prefs ->
        (1..4).map { stage ->
            prefs[defaultMessageKeys[stage - 1]] ?: MessageTemplateResolver.builtInFallback(stage)
        }
    }

    suspend fun setReminderIntervals(minutes: List<Long>) {
        context.dataStore.edit { it[intervalsKey] = minutes.joinToString(",") }
    }

    suspend fun setSnoozeDuration(minutes: Long) {
        context.dataStore.edit { it[snoozeKey] = minutes }
    }

    suspend fun setDefaultReminderMessages(messages: List<String>) {
        context.dataStore.edit { prefs ->
            messages.forEachIndexed { index, message ->
                if (index < defaultMessageKeys.size) prefs[defaultMessageKeys[index]] = message
            }
        }
    }
}
