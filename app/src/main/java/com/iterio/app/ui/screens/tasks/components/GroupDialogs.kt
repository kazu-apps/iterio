package com.iterio.app.ui.screens.tasks.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iterio.app.R
import com.iterio.app.domain.model.SubjectGroup
import com.iterio.app.ui.theme.AccentTeal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val GROUP_COLORS = listOf(
    "#00838F", "#1976D2", "#388E3C", "#F57C00",
    "#D32F2F", "#7B1FA2", "#5D4037", "#455A64"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean, LocalDate?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#00838F") }
    var hasDeadline by remember { mutableStateOf(false) }
    var deadlineDate by remember { mutableStateOf<LocalDate?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tasks_add_group)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tasks_group_name)) },
                    placeholder = { Text(stringResource(R.string.tasks_group_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.tasks_select_color), style = MaterialTheme.typography.labelMedium)
                ColorSelector(
                    colors = GROUP_COLORS,
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
                DeadlineToggleSection(
                    hasDeadline = hasDeadline,
                    deadlineDate = deadlineDate,
                    onHasDeadlineChange = { hasDeadline = it },
                    onDeadlineDateChange = { deadlineDate = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedColor, hasDeadline, deadlineDate) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditGroupDialog(
    group: SubjectGroup,
    onDismiss: () -> Unit,
    onConfirm: (SubjectGroup) -> Unit,
    onDelete: (SubjectGroup) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var selectedColor by remember { mutableStateOf(group.colorHex) }
    var hasDeadline by remember { mutableStateOf(group.hasDeadline) }
    var deadlineDate by remember { mutableStateOf(group.deadlineDate) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.tasks_delete_confirm)) },
            text = { Text(stringResource(R.string.tasks_delete_group_message, group.name)) },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(group) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.tasks_edit_group)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.tasks_group_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.tasks_select_color), style = MaterialTheme.typography.labelMedium)
                    ColorSelector(
                        colors = GROUP_COLORS,
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                    DeadlineToggleSection(
                        hasDeadline = hasDeadline,
                        deadlineDate = deadlineDate,
                        onHasDeadlineChange = { hasDeadline = it },
                        onDeadlineDateChange = { deadlineDate = it }
                    )
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.tasks_delete_group))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(
                            group.copy(
                                name = name,
                                colorHex = selectedColor,
                                hasDeadline = hasDeadline,
                                deadlineDate = if (hasDeadline) deadlineDate else null
                            )
                        )
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlineToggleSection(
    hasDeadline: Boolean,
    deadlineDate: LocalDate?,
    onHasDeadlineChange: (Boolean) -> Unit,
    onDeadlineDateChange: (LocalDate?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.group_deadline_toggle),
                style = MaterialTheme.typography.labelMedium
            )
            Switch(
                checked = hasDeadline,
                onCheckedChange = { checked ->
                    onHasDeadlineChange(checked)
                    if (!checked) {
                        onDeadlineDateChange(null)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = AccentTeal
                )
            )
        }

        if (hasDeadline) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = deadlineDate?.format(dateFormatter)
                        ?: stringResource(R.string.group_deadline_select_date)
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadlineDate?.let {
                it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDeadlineDateChange(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ColorSelector(
    colors: List<String>,
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(color)))
                    .clickable { onColorSelected(color) }
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
