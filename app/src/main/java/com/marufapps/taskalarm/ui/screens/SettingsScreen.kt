package com.marufapps.taskalarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marufapps.taskalarm.data.DefaultIntervals
import com.marufapps.taskalarm.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val intervals by viewModel.settingsRepository.reminderIntervalsMinutes
        .collectAsState(initial = DefaultIntervals.MINUTES)
    val snooze by viewModel.settingsRepository.snoozeDurationMinutes.collectAsState(initial = 30L)
    val defaultMessages by viewModel.settingsRepository.defaultReminderMessages
        .collectAsState(initial = listOf("", "", "", ""))

    var i1 by remember { mutableStateOf("") }
    var i2 by remember { mutableStateOf("") }
    var i3 by remember { mutableStateOf("") }
    var i4 by remember { mutableStateOf("") }
    var snoozeText by remember { mutableStateOf("") }
    var m1 by remember { mutableStateOf("") }
    var m2 by remember { mutableStateOf("") }
    var m3 by remember { mutableStateOf("") }
    var m4 by remember { mutableStateOf("") }

    LaunchedEffect(intervals) {
        if (intervals.size >= 4) {
            i1 = intervals[0].toString()
            i2 = intervals[1].toString()
            i3 = intervals[2].toString()
            i4 = intervals[3].toString()
        }
    }
    LaunchedEffect(snooze) { snoozeText = snooze.toString() }
    LaunchedEffect(defaultMessages) {
        if (defaultMessages.size >= 4) {
            m1 = defaultMessages[0]
            m2 = defaultMessages[1]
            m3 = defaultMessages[2]
            m4 = defaultMessages[3]
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Missed-deadline reminders",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "You'll get notified up to 4 times after a deadline passes. Set when each fires " +
                    "(minutes after the deadline) and what it says by default. Any task can still " +
                    "override its own message from the Add/Edit screen. {task} and {hours} are " +
                    "replaced automatically.",
                style = MaterialTheme.typography.bodyMedium
            )

            ReminderStageSettings(1, i1, { i1 = it }, m1, { m1 = it })
            ReminderStageSettings(2, i2, { i2 = it }, m2, { m2 = it })
            ReminderStageSettings(3, i3, { i3 = it }, m3, { m3 = it })
            ReminderStageSettings(4, i4, { i4 = it }, m4, { m4 = it })

            Text("Snooze duration", style = MaterialTheme.typography.titleLarge)
            IntervalField("Minutes to snooze when tapped from a notification", snoozeText) {
                snoozeText = it
            }

            Button(
                onClick = {
                    val parsed = listOfNotNull(
                        i1.toLongOrNull(), i2.toLongOrNull(), i3.toLongOrNull(), i4.toLongOrNull()
                    )
                    scope.launch {
                        if (parsed.size == 4) {
                            viewModel.settingsRepository.setReminderIntervals(parsed.sorted())
                        }
                        snoozeText.toLongOrNull()?.let {
                            viewModel.settingsRepository.setSnoozeDuration(it)
                        }
                        viewModel.settingsRepository.setDefaultReminderMessages(listOf(m1, m2, m3, m4))

                        snackbarHostState.showSnackbar(
                            if (parsed.size == 4) "Settings saved" else "Saved (check reminder minute fields — one wasn't a valid number)"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun ReminderStageSettings(
    stage: Int,
    intervalValue: String,
    onIntervalChange: (String) -> Unit,
    messageValue: String,
    onMessageChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Reminder $stage", style = MaterialTheme.typography.titleMedium)
        IntervalField("Minutes after deadline", intervalValue, onIntervalChange)
        OutlinedTextField(
            value = messageValue,
            onValueChange = onMessageChange,
            label = { Text("Default message") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
    }
}

@androidx.compose.runtime.Composable
private fun IntervalField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
