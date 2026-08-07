package com.marufapps.taskalarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marufapps.taskalarm.data.TaskStatus
import com.marufapps.taskalarm.ui.components.statusColor
import com.marufapps.taskalarm.viewmodel.TaskViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val task by viewModel.taskById(taskId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(taskId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        task?.let { viewModel.deleteTask(it) }
                        onBack()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        val current = task
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                Text("Loading…")
            }
            return@Scaffold
        }

        val status = current.computedStatus()
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(current.title, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            if (current.description.isNotBlank()) {
                Text(current.description, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = statusColor(status).copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Deadline: ${current.deadlineInstant.atZone(ZoneId.systemDefault()).format(formatter)}",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                    Text("Category: ${current.category} • Priority: ${current.priority.name.lowercase()}")
                    if (status == TaskStatus.MISSED) {
                        Text(
                            "Missed reminders sent: ${current.missedNotificationCount} / 4",
                            color = statusColor(status)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!current.isCompleted) {
                    androidx.compose.material3.Button(onClick = {
                        viewModel.markCompleted(current)
                    }) {
                        Text("Mark Done")
                    }
                }
                if (status == TaskStatus.MISSED && current.missedNotificationCount > 0) {
                    OutlinedButton(onClick = { viewModel.muteNotifications(current) }) {
                        Text("Stop Reminders")
                    }
                }
            }

            TextButton(onClick = { onEdit(taskId) }) {
                Text("Edit deadline / details")
            }
        }
    }
}
