package com.marufapps.taskalarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marufapps.taskalarm.data.Task
import com.marufapps.taskalarm.data.TaskStatus
import com.marufapps.taskalarm.ui.components.TaskItem
import com.marufapps.taskalarm.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()

    val grouped = remember(tasks) {
        tasks.groupBy { it.computedStatus() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Tasks") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Task") }
            )
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            EmptyState(padding)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                section("Missed", grouped[TaskStatus.MISSED], viewModel, onOpenTask)
                section("Due soon", grouped[TaskStatus.DUE_SOON], viewModel, onOpenTask)
                section("Upcoming", grouped[TaskStatus.UPCOMING], viewModel, onOpenTask)
                section("Completed", grouped[TaskStatus.COMPLETED], viewModel, onOpenTask)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    label: String,
    tasks: List<Task>?,
    viewModel: TaskViewModel,
    onOpenTask: (Long) -> Unit
) {
    if (tasks.isNullOrEmpty()) return
    item(key = "header_$label") {
        Text(
            text = "$label (${tasks.size})",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
    items(tasks, key = { it.id }) { task ->
        TaskItem(
            task = task,
            onClick = { onOpenTask(task.id) },
            onToggleComplete = {
                if (!task.isCompleted) viewModel.markCompleted(task)
            }
        )
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("No tasks yet. Tap \"New Task\" to add your first one.")
    }
}
