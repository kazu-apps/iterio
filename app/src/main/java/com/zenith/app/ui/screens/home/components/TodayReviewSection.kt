package com.zenith.app.ui.screens.home.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.zenith.app.domain.model.ReviewTask
import com.zenith.app.ui.components.ZenithCard
import com.zenith.app.ui.theme.AccentSuccess
import com.zenith.app.ui.theme.AccentTeal

/**
 * 今日の復習タスクセクション
 */
@Composable
fun TodayReviewSection(
    reviewTasks: List<ReviewTask>,
    onToggleComplete: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reviewTasks.isEmpty()) return

    val completedCount = reviewTasks.count { it.isCompleted }
    val totalCount = reviewTasks.size

    ZenithCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "今日の復習",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "$completedCount / $totalCount 完了",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (completedCount == totalCount) AccentSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reviewTasks.forEach { reviewTask ->
                    ReviewTaskItem(
                        reviewTask = reviewTask,
                        onToggleComplete = { onToggleComplete(reviewTask.id, !reviewTask.isCompleted) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewTaskItem(
    reviewTask: ReviewTask,
    onToggleComplete: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (reviewTask.isCompleted) {
            AccentSuccess.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "backgroundColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleComplete() },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
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
                // チェックボックス風のアイコン
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (reviewTask.isCompleted) AccentSuccess
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (reviewTask.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "完了",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = reviewTask.taskName ?: "タスク",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (reviewTask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (reviewTask.isCompleted) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = reviewTask.reviewLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reviewTask.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            AccentTeal
                        }
                    )
                }
            }

            reviewTask.groupName?.let { groupName ->
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
