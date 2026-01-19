package com.zenith.app.ui.screens.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenith.app.domain.model.ScheduleType
import com.zenith.app.domain.model.SubjectGroup
import com.zenith.app.domain.model.Task
import com.zenith.app.ui.screens.tasks.components.ScheduleSection
import com.zenith.app.ui.theme.AccentTeal
import com.zenith.app.ui.theme.AccentWarning

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
                title = { Text("タスク管理") },
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
                    Icon(Icons.Default.Add, contentDescription = "タスク追加")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 科目グループ選択エリア
            GroupSelector(
                groups = uiState.groups,
                selectedGroup = uiState.selectedGroup,
                onGroupSelected = { viewModel.selectGroup(it) },
                onAddGroup = { viewModel.showAddGroupDialog() },
                onEditGroup = { viewModel.showEditGroupDialog(it) }
            )

            HorizontalDivider()

            // タスク一覧
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

    // ダイアログ
    if (uiState.showAddGroupDialog) {
        AddGroupDialog(
            onDismiss = { viewModel.hideAddGroupDialog() },
            onConfirm = { name, color -> viewModel.addGroup(name, color) }
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
            onDismiss = { viewModel.hideAddTaskDialog() },
            onConfirm = { name, duration -> viewModel.addTask(name, duration) }
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
                onScheduleTypeChange = viewModel::updateEditingScheduleType,
                onRepeatDaysChange = viewModel::updateEditingRepeatDays,
                onDeadlineDateChange = viewModel::updateEditingDeadlineDate,
                onSpecificDateChange = viewModel::updateEditingSpecificDate,
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

@Composable
private fun GroupSelector(
    groups: List<SubjectGroup>,
    selectedGroup: SubjectGroup?,
    onGroupSelected: (SubjectGroup) -> Unit,
    onAddGroup: () -> Unit,
    onEditGroup: (SubjectGroup) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(groups) { group ->
            GroupChip(
                group = group,
                isSelected = group.id == selectedGroup?.id,
                onClick = { onGroupSelected(group) },
                onLongClick = { onEditGroup(group) }
            )
        }
        item {
            AddGroupChip(onClick = onAddGroup)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupChip(
    group: SubjectGroup,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        try {
            Color(android.graphics.Color.parseColor(group.colorHex))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(group.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                    )
            )
            Text(
                text = group.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddGroupChip(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "追加",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskList(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onStartTimer: (Task) -> Unit
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    "タスクがありません",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    "右下の＋ボタンで追加してください",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks) { task ->
                TaskCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onStartTimer = { onStartTimer(task) }
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onStartTimer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    // Schedule badge
                    if (task.scheduleType != ScheduleType.NONE) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (task.isOverdue) AccentWarning.copy(alpha = 0.2f) else AccentTeal.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (task.scheduleType) {
                                        ScheduleType.REPEAT -> Icons.Default.Repeat
                                        ScheduleType.DEADLINE -> Icons.Default.Schedule
                                        ScheduleType.SPECIFIC -> Icons.Default.DateRange
                                        else -> Icons.Default.Schedule
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (task.isOverdue) AccentWarning else AccentTeal
                                )
                                Text(
                                    text = task.scheduleLabel ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (task.isOverdue) AccentWarning else AccentTeal
                                )
                            }
                        }
                    }
                }
                if (task.progressNote != null || task.progressPercent != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        task.progressPercent?.let { percent ->
                            LinearProgressIndicator(
                                progress = { percent / 100f },
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        task.progressNote?.let { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
                task.nextGoal?.let { goal ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "次回: $goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Last studied info
                task.lastStudiedLabel?.let { label ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onStartTimer) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "タイマー開始",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyGroupsMessage(onAddGroup: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                "科目グループがありません",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Button(onClick = onAddGroup) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("科目グループを追加")
            }
        }
    }
}

@Composable
private fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#00838F") }

    val colors = listOf(
        "#00838F", "#1976D2", "#388E3C", "#F57C00",
        "#D32F2F", "#7B1FA2", "#5D4037", "#455A64"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("科目グループを追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("グループ名") },
                    placeholder = { Text("例: 数学") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("色を選択", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .clickable { selectedColor = color }
                                .then(
                                    if (color == selectedColor) {
                                        Modifier.padding(2.dp)
                                    } else Modifier
                                )
                        ) {
                            if (color == selectedColor) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
private fun EditGroupDialog(
    group: SubjectGroup,
    onDismiss: () -> Unit,
    onConfirm: (SubjectGroup) -> Unit,
    onDelete: (SubjectGroup) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var selectedColor by remember { mutableStateOf(group.colorHex) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val colors = listOf(
        "#00838F", "#1976D2", "#388E3C", "#F57C00",
        "#D32F2F", "#7B1FA2", "#5D4037", "#455A64"
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("削除確認") },
            text = { Text("「${group.name}」を削除しますか？\nこのグループ内のタスクも全て削除されます。") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(group) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("キャンセル")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("グループを編集") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("グループ名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("色を選択", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .clickable { selectedColor = color }
                            ) {
                                if (color == selectedColor) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("このグループを削除")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(group.copy(name = name, colorHex = selectedColor)) },
                    enabled = name.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun AddTaskDialog(
    defaultWorkDurationMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var useCustomDuration by remember { mutableStateOf(false) }
    var workDuration by remember { mutableFloatStateOf(defaultWorkDurationMinutes.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("タスクを追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("タスク名") },
                    placeholder = { Text("例: 微分積分") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "作業時間を指定",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = useCustomDuration,
                        onCheckedChange = { useCustomDuration = it }
                    )
                }

                if (useCustomDuration) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (workDuration > 1f) workDuration -= 1f }
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "1分減らす",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${workDuration.toInt()}分",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(
                                onClick = { if (workDuration < 180f) workDuration += 1f }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "1分増やす",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Slider(
                            value = workDuration,
                            onValueChange = { workDuration = it },
                            valueRange = 1f..180f,
                            steps = 178
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1分", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("180分", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Text(
                        text = "設定のデフォルト値（${defaultWorkDurationMinutes}分）を使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val duration = if (useCustomDuration) workDuration.toInt() else null
                    onConfirm(name, duration)
                },
                enabled = name.isNotBlank()
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
private fun EditTaskDialog(
    task: Task,
    scheduleType: ScheduleType,
    repeatDays: Set<Int>,
    deadlineDate: java.time.LocalDate?,
    specificDate: java.time.LocalDate?,
    onScheduleTypeChange: (ScheduleType) -> Unit,
    onRepeatDaysChange: (Set<Int>) -> Unit,
    onDeadlineDateChange: (java.time.LocalDate?) -> Unit,
    onSpecificDateChange: (java.time.LocalDate?) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onStartTimer: (Task) -> Unit
) {
    var name by remember { mutableStateOf(task.name) }
    var progressNote by remember { mutableStateOf(task.progressNote ?: "") }
    var progressPercent by remember { mutableStateOf(task.progressPercent?.toString() ?: "") }
    var nextGoal by remember { mutableStateOf(task.nextGoal ?: "") }
    var useCustomDuration by remember { mutableStateOf(task.workDurationMinutes != null) }
    var workDuration by remember { mutableFloatStateOf((task.workDurationMinutes ?: 25).toFloat()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("削除確認") },
            text = { Text("「${task.name}」を削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(task) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("キャンセル")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("タスクを編集") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("タスク名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = progressNote,
                        onValueChange = { progressNote = it },
                        label = { Text("進捗メモ") },
                        placeholder = { Text("例: 第3章まで完了") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = progressPercent,
                        onValueChange = { progressPercent = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("進捗率 (%)") },
                        placeholder = { Text("例: 60") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nextGoal,
                        onValueChange = { nextGoal = it },
                        label = { Text("次回の目標") },
                        placeholder = { Text("例: 第4章 p.80-100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // Schedule Section
                    ScheduleSection(
                        scheduleType = scheduleType,
                        repeatDays = repeatDays,
                        deadlineDate = deadlineDate,
                        specificDate = specificDate,
                        onScheduleTypeChange = onScheduleTypeChange,
                        onRepeatDaysChange = onRepeatDaysChange,
                        onDeadlineDateChange = onDeadlineDateChange,
                        onSpecificDateChange = onSpecificDateChange
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "作業時間を指定",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = useCustomDuration,
                            onCheckedChange = { useCustomDuration = it }
                        )
                    }

                    if (useCustomDuration) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (workDuration > 1f) workDuration -= 1f }
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "1分減らす",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "${workDuration.toInt()}分",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                IconButton(
                                    onClick = { if (workDuration < 180f) workDuration += 1f }
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "1分増やす",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Slider(
                                value = workDuration,
                                onValueChange = { workDuration = it },
                                valueRange = 1f..180f,
                                steps = 178
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("1分", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("180分", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Text(
                            text = "設定のデフォルト値を使用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onStartTimer(task) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("このタスクでタイマー開始")
                    }

                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("このタスクを削除")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(
                            task.copy(
                                name = name,
                                progressNote = progressNote.takeIf { it.isNotBlank() },
                                progressPercent = progressPercent.toIntOrNull()?.coerceIn(0, 100),
                                nextGoal = nextGoal.takeIf { it.isNotBlank() },
                                workDurationMinutes = if (useCustomDuration) workDuration.toInt() else null,
                                scheduleType = scheduleType,
                                repeatDays = repeatDays,
                                deadlineDate = deadlineDate,
                                specificDate = specificDate
                            )
                        )
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("キャンセル")
                }
            }
        )
    }
}
