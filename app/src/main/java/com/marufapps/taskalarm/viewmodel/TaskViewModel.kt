package com.marufapps.taskalarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.marufapps.taskalarm.alarm.AlarmScheduler
import com.marufapps.taskalarm.data.Priority
import com.marufapps.taskalarm.data.SettingsRepository
import com.marufapps.taskalarm.data.Task
import com.marufapps.taskalarm.data.TaskRepository
import com.marufapps.taskalarm.notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler,
    val settingsRepository: SettingsRepository,
    private val workManager: WorkManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun taskById(id: Long) = repository.observeTaskById(id)

    /** Creates or updates a task and (re)schedules its deadline alarm. */
    fun saveTask(
        id: Long? = null,
        title: String,
        description: String,
        category: String,
        priority: Priority,
        deadlineEpochMillis: Long,
        reminderMessage1: String? = null,
        reminderMessage2: String? = null,
        reminderMessage3: String? = null,
        reminderMessage4: String? = null
    ) {
        viewModelScope.launch {
            val task = if (id != null) {
                repository.getTaskById(id)?.copy(
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    deadlineEpochMillis = deadlineEpochMillis,
                    missedNotificationCount = 0,
                    notificationsMuted = false,
                    reminderMessage1 = reminderMessage1,
                    reminderMessage2 = reminderMessage2,
                    reminderMessage3 = reminderMessage3,
                    reminderMessage4 = reminderMessage4
                ) ?: Task(
                    title = title, description = description, category = category,
                    priority = priority, deadlineEpochMillis = deadlineEpochMillis,
                    reminderMessage1 = reminderMessage1, reminderMessage2 = reminderMessage2,
                    reminderMessage3 = reminderMessage3, reminderMessage4 = reminderMessage4
                )
            } else {
                Task(
                    title = title, description = description, category = category,
                    priority = priority, deadlineEpochMillis = deadlineEpochMillis,
                    reminderMessage1 = reminderMessage1, reminderMessage2 = reminderMessage2,
                    reminderMessage3 = reminderMessage3, reminderMessage4 = reminderMessage4
                )
            }
            val savedId = repository.saveTask(task)
            val finalTask = task.copy(id = if (task.id == 0L) savedId else task.id)
            alarmScheduler.scheduleDeadlineAlarm(finalTask)
        }
    }

    fun markCompleted(task: Task) {
        viewModelScope.launch {
            repository.markCompleted(task.id)
            alarmScheduler.cancelDeadlineAlarm(task.id)
            workManager.cancelUniqueWork("missed_check_${task.id}")
            notificationHelper.cancelAllForTask(task.id)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            alarmScheduler.cancelDeadlineAlarm(task.id)
            workManager.cancelUniqueWork("missed_check_${task.id}")
            notificationHelper.cancelAllForTask(task.id)
            repository.deleteTask(task)
        }
    }

    fun muteNotifications(task: Task) {
        viewModelScope.launch {
            repository.muteNotifications(task.id)
            workManager.cancelUniqueWork("missed_check_${task.id}")
            notificationHelper.cancelAllForTask(task.id)
        }
    }
}
