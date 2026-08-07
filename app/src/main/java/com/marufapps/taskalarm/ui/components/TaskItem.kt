package com.marufapps.taskalarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.marufapps.taskalarm.data.Priority
import com.marufapps.taskalarm.data.Task
import com.marufapps.taskalarm.data.TaskStatus
import com.marufapps.taskalarm.ui.theme.StatusCompleted
import com.marufapps.taskalarm.ui.theme.StatusDueSoon
import com.marufapps.taskalarm.ui.theme.StatusMissed
import com.marufapps.taskalarm.ui.theme.StatusUpcoming
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun statusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.UPCOMING -> StatusUpcoming
    TaskStatus.DUE_SOON -> StatusDueSoon
    TaskStatus.MISSED -> StatusMissed
    TaskStatus.COMPLETED -> StatusCompleted
}

private fun priorityLabel(priority: Priority) = when (priority) {
    Priority.LOW -> "Low"
    Priority.MEDIUM -> "Medium"
    Priority.HIGH -> "High"
}

@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = task.computedStatus()
    val color = statusColor(status)
    val formatter = remember(task.id) {
        DateTimeFormatter.ofPattern("MMM d, h:mm a")
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (status == TaskStatus.COMPLETED) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusLabel(status),
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = task.deadlineInstant.atZone(ZoneId.systemDefault()).format(formatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (status == TaskStatus.MISSED && task.missedNotificationCount > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${task.missedNotificationCount}/4 missed reminders sent",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusMissed
                    )
                }
            }

            IconButton(onClick = onToggleComplete) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                    contentDescription = "Toggle complete",
                    tint = if (task.isCompleted) StatusCompleted else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun statusLabel(status: TaskStatus) = when (status) {
    TaskStatus.UPCOMING -> "Upcoming"
    TaskStatus.DUE_SOON -> "Due soon"
    TaskStatus.MISSED -> "Missed"
    TaskStatus.COMPLETED -> "Done"
}
