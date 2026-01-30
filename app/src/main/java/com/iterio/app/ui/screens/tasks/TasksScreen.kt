package com.iterio.app.ui.screens.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iterio.app.R
import com.iterio.app.domain.model.Task
import com.iterio.app.ui.screens.tasks.components.AddGroupDialog
import com.iterio.app.ui.screens.tasks.components.AddTaskDialog
import com.iterio.app.ui.screens.tasks.components.EditGroupDialog
import com.iterio.app.ui.screens.tasks.components.EditTaskDialog
import com.iterio.app.ui.screens.tasks.components.EmptyGroupsMessage
import com.iterio.app.ui.screens.tasks.components.GroupSelector
import com.iterio.app.ui.screens.tasks.components.TaskList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onStartTimer: (Task) -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tasks_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (uiState.selectedGroup != null) {
                FloatingActionButton(
                    onClick = { viewModel.showAddTaskDialog() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tasks_add_task))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Group selector area
            GroupSelector(
                groups = uiState.groups,
                selectedGroup = uiState.selectedGroup,
                onGroupSelected = { viewModel.selectGroup(it) },
                onAddGroup = { viewModel.showAddGroupDialog() },
                onEditGroup = { viewModel.showEditGroupDialog(it) }
            )

            HorizontalDivider()

            // Task list
            if (uiState.selectedGroup != null) {
                TaskList(
                    tasks = uiState.tasksForSelectedGroup,
                    onTaskClick = { viewModel.showEditTaskDialog(it) },
                    onStartTimer = onStartTimer
                )
            } else {
                EmptyGroupsMessage(onAddGroup = { viewModel.showAddGroupDialog() })
            }
        }
    }

    // Dialogs
    if (uiState.showAddGroupDialog) {
        AddGroupDialog(
            onDismiss = { viewModel.hideAddGroupDialog() },
            onConfirm = { name, color, hasDeadline, deadlineDate ->
                viewModel.addGroup(name, color, hasDeadline, deadlineDate)
            }
        )
    }

    if (uiState.showEditGroupDialog) {
        uiState.editingGroup?.let { group ->
            EditGroupDialog(
                group = group,
                onDismiss = { viewModel.hideEditGroupDialog() },
                onConfirm = { viewModel.updateGroup(it) },
                onDelete = { viewModel.deleteGroup(it) }
            )
        }
    }

    if (uiState.showAddTaskDialog) {
        AddTaskDialog(
            defaultWorkDurationMinutes = uiState.defaultWorkDurationMinutes,
            scheduleType = uiState.addingScheduleType,
            repeatDays = uiState.addingRepeatDays,
            deadlineDate = uiState.addingDeadlineDate,
            specificDate = uiState.addingSpecificDate,
            isPremium = uiState.isPremium,
            reviewCountOptions = uiState.reviewCountOptions,
            reviewCount = uiState.addingReviewCount,
            reviewEnabled = uiState.addingReviewEnabled,
            onScheduleTypeChange = viewModel::updateAddingScheduleType,
            onRepeatDaysChange = viewModel::updateAddingRepeatDays,
            onDeadlineDateChange = viewModel::updateAddingDeadlineDate,
            onSpecificDateChange = viewModel::updateAddingSpecificDate,
            onReviewCountChange = viewModel::updateAddingReviewCount,
            onReviewEnabledChange = viewModel::updateAddingReviewEnabled,
            onDismiss = { viewModel.hideAddTaskDialog() },
            onConfirm = { name, duration, scheduleType, repeatDays, deadlineDate, specificDate, reviewCount, reviewEnabled ->
                viewModel.addTask(name, duration, scheduleType, repeatDays, deadlineDate, specificDate, reviewCount, reviewEnabled)
            }
        )
    }

    if (uiState.showEditTaskDialog) {
        uiState.editingTask?.let { task ->
            EditTaskDialog(
                task = task,
                scheduleType = uiState.editingScheduleType,
                repeatDays = uiState.editingRepeatDays,
                deadlineDate = uiState.editingDeadlineDate,
                specificDate = uiState.editingSpecificDate,
                isPremium = uiState.isPremium,
                reviewCountOptions = uiState.reviewCountOptions,
                reviewCount = uiState.editingReviewCount,
                reviewEnabled = uiState.editingReviewEnabled,
                onScheduleTypeChange = viewModel::updateEditingScheduleType,
                onRepeatDaysChange = viewModel::updateEditingRepeatDays,
                onDeadlineDateChange = viewModel::updateEditingDeadlineDate,
                onSpecificDateChange = viewModel::updateEditingSpecificDate,
                onReviewCountChange = viewModel::updateEditingReviewCount,
                onReviewEnabledChange = viewModel::updateEditingReviewEnabled,
                onDismiss = { viewModel.hideEditTaskDialog() },
                onConfirm = { viewModel.updateTask(it) },
                onDelete = { viewModel.deleteTask(it) },
                onStartTimer = {
                    viewModel.hideEditTaskDialog()
                    onStartTimer(it)
                }
            )
        }
    }
}
