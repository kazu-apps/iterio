package com.iterio.app.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iterio.app.R
import com.iterio.app.domain.model.DeadlineItem
import com.iterio.app.ui.components.EmptySectionMessage
import com.iterio.app.ui.components.IterioCard
import com.iterio.app.ui.theme.AccentError
import com.iterio.app.ui.theme.AccentTeal
import com.iterio.app.ui.theme.AccentWarning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 期限が近いタスク・グループセクション（タスク期限とグループ期限を分離表示）
 */
@Composable
fun UpcomingDeadlinesSection(
    taskDeadlines: List<DeadlineItem.TaskDeadline>,
    groupDeadlines: List<DeadlineItem.GroupDeadline>,
    totalDeadlineCount: Int,
    onStartTimer: (Long) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEmpty = taskDeadlines.isEmpty() && groupDeadlines.isEmpty()
    val totalDisplayed = taskDeadlines.size + groupDeadlines.size

    IterioCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = AccentWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.deadline_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (totalDeadlineCount > totalDisplayed) {
                    TextButton(onClick = onViewAll) {
                        Text(
                            text = stringResource(R.string.deadline_view_all, totalDeadlineCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentTeal
                        )
                    }
                } else if (!isEmpty) {
                    Text(
                        text = "${totalDeadlineCount}件",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEmpty) {
                EmptySectionMessage(
                    icon = Icons.Default.DateRange,
                    message = stringResource(R.string.deadline_empty)
                )
            } else {
                // Task deadlines sub-section
                if (taskDeadlines.isNotEmpty()) {
                    DeadlineSubSectionHeader(
                        icon = Icons.Default.DateRange,
                        title = stringResource(R.string.deadline_task_subtitle),
                        count = taskDeadlines.size
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        taskDeadlines.forEach { item ->
                            TaskDeadlineItem(
                                item = item,
                                onStartTimer = { onStartTimer(item.taskId) }
                            )
                        }
                    }
                }

                // Divider between sub-sections
                if (taskDeadlines.isNotEmpty() && groupDeadlines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Group deadlines sub-section
                if (groupDeadlines.isNotEmpty()) {
                    DeadlineSubSectionHeader(
                        icon = Icons.Default.Folder,
                        title = stringResource(R.string.deadline_group_subtitle),
                        count = groupDeadlines.size
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        groupDeadlines.forEach { item ->
                            GroupDeadlineItem(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlineSubSectionHeader(
    icon: ImageVector,
    title: String,
    count: Int
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${count}件",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TaskDeadlineItem(
    item: DeadlineItem.TaskDeadline,
    onStartTimer: () -> Unit
) {
    val today = LocalDate.now()
    val daysUntilDeadline = ChronoUnit.DAYS.between(today, item.deadlineDate).toInt()
    val urgencyColor = getUrgencyColor(daysUntilDeadline)
    val dateFormatter = DateTimeFormatter.ofPattern("M/d")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartTimer() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(urgencyColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = urgencyColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(
                            R.string.deadline_date_format,
                            item.deadlineDate.format(dateFormatter),
                            daysUntilDeadline
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = urgencyColor
                    )
                }
            }

            FilledIconButton(
                onClick = onStartTimer,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.deadline_start_timer),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GroupDeadlineItem(
    item: DeadlineItem.GroupDeadline
) {
    val today = LocalDate.now()
    val daysUntilDeadline = ChronoUnit.DAYS.between(today, item.deadlineDate).toInt()
    val urgencyColor = getUrgencyColor(daysUntilDeadline)
    val dateFormatter = DateTimeFormatter.ofPattern("M/d")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(urgencyColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = urgencyColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.deadline_group_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.deadline_date_format,
                            item.deadlineDate.format(dateFormatter),
                            daysUntilDeadline
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = urgencyColor
                    )
                }
            }
        }
    }
}

/**
 * 期限までの日数に応じた緊急度の色を返す
 * - 1日以内: AccentError（赤）
 * - 3日以内: AccentWarning（オレンジ）
 * - それ以外: AccentTeal（通常）
 */
private fun getUrgencyColor(daysUntilDeadline: Int): Color {
    return when {
        daysUntilDeadline <= 1 -> AccentError
        daysUntilDeadline <= 3 -> AccentWarning
        else -> AccentTeal
    }
}
