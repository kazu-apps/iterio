package com.zenith.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.app.domain.model.ScheduleType
import com.zenith.app.domain.model.SubjectGroup
import com.zenith.app.domain.model.Task
import com.zenith.app.domain.repository.SettingsRepository
import com.zenith.app.domain.repository.SubjectGroupRepository
import com.zenith.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TasksUiState(
    val groups: List<SubjectGroup> = emptyList(),
    val selectedGroup: SubjectGroup? = null,
    val tasksForSelectedGroup: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val showAddGroupDialog: Boolean = false,
    val showAddTaskDialog: Boolean = false,
    val showEditGroupDialog: Boolean = false,
    val showEditTaskDialog: Boolean = false,
    val editingGroup: SubjectGroup? = null,
    val editingTask: Task? = null,
    val defaultWorkDurationMinutes: Int = 25,
    // Schedule editing state
    val editingScheduleType: ScheduleType = ScheduleType.NONE,
    val editingRepeatDays: Set<Int> = emptySet(),
    val editingDeadlineDate: LocalDate? = null,
    val editingSpecificDate: LocalDate? = null
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val subjectGroupRepository: SubjectGroupRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
        loadDefaultWorkDuration()
    }

    private fun loadDefaultWorkDuration() {
        viewModelScope.launch {
            val settings = settingsRepository.getPomodoroSettings()
            _uiState.update { it.copy(defaultWorkDurationMinutes = settings.workDurationMinutes) }
        }
    }

    private fun loadGroups() {
        viewModelScope.launch {
            subjectGroupRepository.getAllGroups()
                .collect { groups ->
                    _uiState.update { state ->
                        state.copy(
                            groups = groups,
                            isLoading = false,
                            // 自動的に最初のグループを選択
                            selectedGroup = state.selectedGroup ?: groups.firstOrNull()
                        )
                    }
                    // 選択されているグループのタスクをロード
                    _uiState.value.selectedGroup?.let { group ->
                        loadTasksForGroup(group.id)
                    }
                }
        }
    }

    private fun loadTasksForGroup(groupId: Long) {
        viewModelScope.launch {
            taskRepository.getTasksByGroup(groupId)
                .collect { tasks ->
                    _uiState.update { it.copy(tasksForSelectedGroup = tasks) }
                }
        }
    }

    fun selectGroup(group: SubjectGroup) {
        _uiState.update { it.copy(selectedGroup = group) }
        loadTasksForGroup(group.id)
    }

    fun showAddGroupDialog() {
        _uiState.update { it.copy(showAddGroupDialog = true) }
    }

    fun hideAddGroupDialog() {
        _uiState.update { it.copy(showAddGroupDialog = false) }
    }

    fun showEditGroupDialog(group: SubjectGroup) {
        _uiState.update { it.copy(showEditGroupDialog = true, editingGroup = group) }
    }

    fun hideEditGroupDialog() {
        _uiState.update { it.copy(showEditGroupDialog = false, editingGroup = null) }
    }

    fun showAddTaskDialog() {
        _uiState.update { it.copy(showAddTaskDialog = true) }
    }

    fun hideAddTaskDialog() {
        _uiState.update { it.copy(showAddTaskDialog = false) }
    }

    fun showEditTaskDialog(task: Task) {
        _uiState.update {
            it.copy(
                showEditTaskDialog = true,
                editingTask = task,
                editingScheduleType = task.scheduleType,
                editingRepeatDays = task.repeatDays,
                editingDeadlineDate = task.deadlineDate,
                editingSpecificDate = task.specificDate
            )
        }
    }

    fun hideEditTaskDialog() {
        _uiState.update {
            it.copy(
                showEditTaskDialog = false,
                editingTask = null,
                editingScheduleType = ScheduleType.NONE,
                editingRepeatDays = emptySet(),
                editingDeadlineDate = null,
                editingSpecificDate = null
            )
        }
    }

    fun updateEditingScheduleType(type: ScheduleType) {
        _uiState.update { it.copy(editingScheduleType = type) }
    }

    fun updateEditingRepeatDays(days: Set<Int>) {
        _uiState.update { it.copy(editingRepeatDays = days) }
    }

    fun updateEditingDeadlineDate(date: LocalDate?) {
        _uiState.update { it.copy(editingDeadlineDate = date) }
    }

    fun updateEditingSpecificDate(date: LocalDate?) {
        _uiState.update { it.copy(editingSpecificDate = date) }
    }

    fun addGroup(name: String, colorHex: String) {
        viewModelScope.launch {
            val group = SubjectGroup(name = name, colorHex = colorHex)
            val id = subjectGroupRepository.insertGroup(group)
            hideAddGroupDialog()
            // 新しく作成したグループを選択
            subjectGroupRepository.getGroupById(id)?.let { newGroup ->
                selectGroup(newGroup)
            }
        }
    }

    fun updateGroup(group: SubjectGroup) {
        viewModelScope.launch {
            subjectGroupRepository.updateGroup(group)
            hideEditGroupDialog()
        }
    }

    fun deleteGroup(group: SubjectGroup) {
        viewModelScope.launch {
            subjectGroupRepository.deleteGroup(group)
            hideEditGroupDialog()
            // 削除後に最初のグループを選択
            _uiState.update { state ->
                val remainingGroups = state.groups.filter { it.id != group.id }
                state.copy(
                    selectedGroup = remainingGroups.firstOrNull(),
                    tasksForSelectedGroup = emptyList()
                )
            }
        }
    }

    fun addTask(
        name: String,
        workDurationMinutes: Int?,
        scheduleType: ScheduleType = ScheduleType.NONE,
        repeatDays: Set<Int> = emptySet(),
        deadlineDate: LocalDate? = null,
        specificDate: LocalDate? = null
    ) {
        val selectedGroup = _uiState.value.selectedGroup ?: return
        viewModelScope.launch {
            val task = Task(
                groupId = selectedGroup.id,
                name = name,
                workDurationMinutes = workDurationMinutes,
                scheduleType = scheduleType,
                repeatDays = repeatDays,
                deadlineDate = deadlineDate,
                specificDate = specificDate
            )
            taskRepository.insertTask(task)
            hideAddTaskDialog()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
            hideEditTaskDialog()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            hideEditTaskDialog()
        }
    }

    fun updateTaskProgress(taskId: Long, note: String?, percent: Int?, goal: String?) {
        viewModelScope.launch {
            taskRepository.updateProgress(taskId, note, percent, goal)
        }
    }
}
