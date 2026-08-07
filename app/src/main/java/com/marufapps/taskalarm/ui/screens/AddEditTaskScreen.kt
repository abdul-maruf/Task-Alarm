package com.marufapps.taskalarm.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marufapps.taskalarm.data.Priority
import com.marufapps.taskalarm.viewmodel.TaskViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

/** Quick-fill templates for the 4 reminder stages, selectable via tone chips in the editor. */
private object ReminderTonePresets {
    val GENTLE = listOf(
        "Just a gentle nudge — {task} is due now 🙂",
        "Hey, {task} is still open — no rush, just a heads up",
        "Friendly reminder: {task} could use your attention",
        "Last nudge on {task} — whenever you get a chance"
    )
    val ANNOYED = listOf(
        "😤 {task} is overdue already",
        "Seriously, {task} is still not done",
        "This is reminder 3 — {task} needs to happen",
        "😤 Final warning — {task} is way overdue"
    )
    val URGENT = listOf(
        "🚨 {task} is overdue — act now",
        "🚨 Still not done: {task}",
        "🚨 URGENT: {task} is significantly overdue",
        "🚨 LAST CHANCE: {task} — do this now"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    taskId: Long?,
    onDone: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var deadlineMillis by remember {
        mutableStateOf(System.currentTimeMillis() + 60 * 60 * 1000L)
    }
    var msg1 by remember { mutableStateOf("") }
    var msg2 by remember { mutableStateOf("") }
    var msg3 by remember { mutableStateOf("") }
    var msg4 by remember { mutableStateOf("") }

    val globalDefaults by viewModel.settingsRepository.defaultReminderMessages
        .collectAsState(initial = listOf("", "", "", ""))

    if (taskId != null) {
        val existing by viewModel.taskById(taskId).collectAsState(initial = null)
        LaunchedEffect(existing) {
            existing?.let {
                title = it.title
                description = it.description
                category = it.category
                priority = it.priority
                deadlineMillis = it.deadlineEpochMillis
                msg1 = it.reminderMessage1 ?: ""
                msg2 = it.reminderMessage2 ?: ""
                msg3 = it.reminderMessage3 ?: ""
                msg4 = it.reminderMessage4 ?: ""
            }
        }
    }

    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a") }
    val deadlineLabel = remember(deadlineMillis) {
        Instant.ofEpochMilli(deadlineMillis).atZone(ZoneId.systemDefault()).format(formatter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "New Task" else "Edit Task") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Text("Deadline")
            Button(onClick = {
                showDateTimePicker(context, deadlineMillis) { newMillis ->
                    deadlineMillis = newMillis
                }
            }) {
                Text(deadlineLabel)
            }

            HorizontalDivider()

            Text("Custom reminder messages", style = MaterialTheme.typography.titleMedium)
            Text(
                "Optional. Leave blank to use your default message from Settings for that stage. " +
                    "{task} is replaced with the title, {hours} with hours overdue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Quick-fill a tone (you can still edit after):")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = false,
                    onClick = {
                        msg1 = ReminderTonePresets.GENTLE[0]
                        msg2 = ReminderTonePresets.GENTLE[1]
                        msg3 = ReminderTonePresets.GENTLE[2]
                        msg4 = ReminderTonePresets.GENTLE[3]
                    },
                    label = { Text("🙏 Gentle") }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        msg1 = ReminderTonePresets.ANNOYED[0]
                        msg2 = ReminderTonePresets.ANNOYED[1]
                        msg3 = ReminderTonePresets.ANNOYED[2]
                        msg4 = ReminderTonePresets.ANNOYED[3]
                    },
                    label = { Text("😤 Annoyed") }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        msg1 = ReminderTonePresets.URGENT[0]
                        msg2 = ReminderTonePresets.URGENT[1]
                        msg3 = ReminderTonePresets.URGENT[2]
                        msg4 = ReminderTonePresets.URGENT[3]
                    },
                    label = { Text("🚨 Urgent") }
                )
            }

            ReminderMessageField(
                stageLabel = "Reminder 1 (soon after deadline)",
                value = msg1,
                onValueChange = { msg1 = it },
                placeholder = globalDefaults.getOrNull(0).orEmpty(),
                previewTitle = title
            )
            ReminderMessageField(
                stageLabel = "Reminder 2",
                value = msg2,
                onValueChange = { msg2 = it },
                placeholder = globalDefaults.getOrNull(1).orEmpty(),
                previewTitle = title
            )
            ReminderMessageField(
                stageLabel = "Reminder 3",
                value = msg3,
                onValueChange = { msg3 = it },
                placeholder = globalDefaults.getOrNull(2).orEmpty(),
                previewTitle = title
            )
            ReminderMessageField(
                stageLabel = "Reminder 4 (final)",
                value = msg4,
                onValueChange = { msg4 = it },
                placeholder = globalDefaults.getOrNull(3).orEmpty(),
                previewTitle = title
            )

            Button(
                onClick = {
                    viewModel.saveTask(
                        id = taskId,
                        title = title.ifBlank { "Untitled task" },
                        description = description,
                        category = category.ifBlank { "General" },
                        priority = priority,
                        deadlineEpochMillis = deadlineMillis,
                        reminderMessage1 = msg1.takeIf { it.isNotBlank() },
                        reminderMessage2 = msg2.takeIf { it.isNotBlank() },
                        reminderMessage3 = msg3.takeIf { it.isNotBlank() },
                        reminderMessage4 = msg4.takeIf { it.isNotBlank() }
                    )
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Task & Set Alarm")
            }
        }
    }
}

/** A single reminder-stage message field with a live preview of the resolved text. */
@Composable
private fun ReminderMessageField(
    stageLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    previewTitle: String
) {
    Column {
        Text(stageLabel, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder.ifBlank { "Uses built-in default" }) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        val previewSource = value.ifBlank { placeholder }
        if (previewSource.isNotBlank()) {
            val resolvedPreview = previewSource
                .replace("{task}", previewTitle.ifBlank { "Untitled task" })
                .replace("{hours}", "2")
            Text(
                "Preview: \"$resolvedPreview\"",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

private fun showDateTimePicker(
    context: android.content.Context,
    initialMillis: Long,
    onPicked: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)
                    onPicked(cal.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
