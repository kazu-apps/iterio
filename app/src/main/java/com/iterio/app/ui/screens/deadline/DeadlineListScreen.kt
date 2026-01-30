package com.iterio.app.ui.screens.deadline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iterio.app.R
import com.iterio.app.domain.model.DeadlineItem
import com.iterio.app.ui.components.EmptySectionMessage
import com.iterio.app.ui.components.IterioCard
import com.iterio.app.ui.components.LoadingIndicator
import com.iterio.app.ui.theme.AccentError
import com.iterio.app.ui.theme.AccentTeal
import com.iterio.app.ui.theme.AccentWarning
import com.iterio.app.ui.theme.BackgroundDark
import com.iterio.app.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlineListScreen(
    onNavigateBack: () -> Unit,
    onStartTimer: (Long) -> Unit,
    viewModel: DeadlineListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.deadline_list_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Summary Stats
                DeadlineSummaryRow(uiState = uiState)

                // Filter Chips
                DeadlineFilterRow(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::updateFilter
                )

                // Item List
                if (uiState.filteredItems.isEmpty()) {
                    EmptySectionMessage(
                        icon = Icons.Default.DateRange,
                        message = stringResource(R.string.deadline_list_empty),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DeadlineItemList(
                        items = uiState.filteredItems,
                        onStartTimer = onStartTimer,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun DeadlineSummaryRow(uiState: DeadlineListUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(
            label = stringResource(R.string.deadline_list_total, uiState.totalCount),
            color = AccentTeal,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = stringResource(R.string.deadline_list_task_count, uiState.taskCount),
            color = AccentWarning,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = stringResource(R.string.deadline_list_group_count, uiState.groupCount),
            color = AccentError,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    IterioCard(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlineFilterRow(
    selectedFilter: DeadlineFilter,
    onFilterSelected: (DeadlineFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filters = listOf(
            DeadlineFilter.ALL to R.string.deadline_list_filter_all,
            DeadlineFilter.TASKS to R.string.deadline_list_filter_tasks,
            DeadlineFilter.GROUPS to R.string.deadline_list_filter_groups
        )

        filters.forEach { (filter, labelResId) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = stringResource(labelResId),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                    selectedLabelColor = AccentTeal
                )
            )
        }
    }
}

@Composable
private fun DeadlineItemList(
    items: List<DeadlineItem>,
    onStartTimer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        items(
            items = items,
            key = { item ->
                when (item) {
                    is DeadlineItem.TaskDeadline -> "task_${item.taskId}"
                    is DeadlineItem.GroupDeadline -> "group_${item.id}"
                }
            }
        ) { item ->
            when (item) {
                is DeadlineItem.TaskDeadline -> DeadlineTaskListItem(
                    item = item,
                    onStartTimer = { onStartTimer(item.taskId) }
                )
                is DeadlineItem.GroupDeadline -> DeadlineGroupListItem(
                    item = item
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DeadlineTaskListItem(
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.deadline_date_format,
                                item.deadlineDate.format(dateFormatter),
                                daysUntilDeadline
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = urgencyColor
                        )
                        item.groupName?.let { groupName ->
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
private fun DeadlineGroupListItem(
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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

private fun getUrgencyColor(daysUntilDeadline: Int): Color {
    return when {
        daysUntilDeadline <= 1 -> AccentError
        daysUntilDeadline <= 3 -> AccentWarning
        else -> AccentTeal
    }
}
