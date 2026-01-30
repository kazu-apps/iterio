package com.iterio.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iterio.app.domain.model.ScheduleType
import com.iterio.app.domain.model.SubjectGroup
import com.iterio.app.domain.model.Task
import com.iterio.app.domain.repository.SettingsRepository
import com.iterio.app.domain.repository.SubjectGroupRepository
import com.iterio.app.domain.repository.TaskRepository
import com.iterio.app.ui.premium.PremiumManager
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
    // Schedule editing state (for edit dialog)
    val editingScheduleType: ScheduleType = ScheduleType.NONE,
    val editingRepeatDays: Set<Int> = emptySet(),
    val editingDeadlineDate: LocalDate? = null,
    val editingSpecificDate: LocalDate? = null,
    // Schedule adding state (for add dialog)
    val addingScheduleType: ScheduleType = ScheduleType.NONE,
    val addingRepeatDays: Set<Int> = emptySet(),
    val addingDeadlineDate: LocalDate? = null,
    val addingSpecificDate: LocalDate? = null,
    // Review count state
    val isPremium: Boolean = false,
    val reviewCountOptions: List<Int> = listOf(2),
    val addingReviewCount: Int? = null,
    val editingReviewCount: Int? = null,
    // Review enabled state
    val addingReviewEnabled: Boolean = true,
    val editingReviewEnabled: Boolean = true
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val subjectGroupRepository: SubjectGroupRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
        loadDefaultWorkDuration()
        observePremiumStatus()
    }

    private fun loadDefaultWorkDuration() {
        viewModelScope.launch {
            settingsRepository.getPomodoroSettings()
                .onSuccess { settings ->
                    _uiState.update { it.copy(defaultWorkDurationMinutes = settings.workDurationMinutes) }
                }
        }
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            premiumManager.subscriptionStatus.collect { status ->
                val isPremium = status.isPremium
                _uiState.update {
                    it.copy(
                        isPremium = isPremium,
                        reviewCountOptions = premiumManager.getReviewCountOptions(isPremium)
                    )
                }
            }
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

    /**
     * イベントを処理
     */
    fun onEvent(event: TasksEvent) {
        when (event) {
            is TasksEvent.SelectGroup -> selectGroup(event.group)
            is TasksEvent.ShowAddGroupDialog -> showAddGroupDialog()
            is TasksEvent.HideAddGroupDialog -> hideAddGroupDialog()
            is TasksEvent.ShowEditGroupDialog -> showEditGroupDialog(event.group)
            is TasksEvent.HideEditGroupDialog -> hideEditGroupDialog()
            is TasksEvent.ShowAddTaskDialog -> showAddTaskDialog()
            is TasksEvent.HideAddTaskDialog -> hideAddTaskDialog()
            is TasksEvent.ShowEditTaskDialog -> showEditTaskDialog(event.task)
            is TasksEvent.HideEditTaskDialog -> hideEditTaskDialog()
            is TasksEvent.UpdateAddingScheduleType -> updateAddingScheduleType(event.type)
            is TasksEvent.UpdateAddingRepeatDays -> updateAddingRepeatDays(event.days)
            is TasksEvent.UpdateAddingDeadlineDate -> updateAddingDeadlineDate(event.date)
            is TasksEvent.UpdateAddingSpecificDate -> updateAddingSpecificDate(event.date)
            is TasksEvent.UpdateEditingScheduleType -> updateEditingScheduleType(event.type)
            is TasksEvent.UpdateEditingRepeatDays -> updateEditingRepeatDays(event.days)
            is TasksEvent.UpdateEditingDeadlineDate -> updateEditingDeadlineDate(event.date)
            is TasksEvent.UpdateEditingSpecificDate -> updateEditingSpecificDate(event.date)
            is TasksEvent.AddGroup -> addGroup(event.name, event.colorHex, event.hasDeadline, event.deadlineDate)
            is TasksEvent.UpdateGroup -> updateGroup(event.group)
            is TasksEvent.DeleteGroup -> deleteGroup(event.group)
            is TasksEvent.UpdateAddingReviewCount -> updateAddingReviewCount(event.reviewCount)
            is TasksEvent.UpdateEditingReviewCount -> updateEditingReviewCount(event.reviewCount)
            is TasksEvent.UpdateAddingReviewEnabled -> updateAddingReviewEnabled(event.enabled)
            is TasksEvent.UpdateEditingReviewEnabled -> updateEditingReviewEnabled(event.enabled)
            is TasksEvent.AddTask -> addTask(
                event.name,
                event.workDurationMinutes,
                event.scheduleType,
                event.repeatDays,
                event.deadlineDate,
                event.specificDate,
                event.reviewCount,
                event.reviewEnabled
            )
            is TasksEvent.UpdateTask -> updateTask(event.task)
            is TasksEvent.DeleteTask -> deleteTask(event.task)
            is TasksEvent.UpdateTaskProgress -> updateTaskProgress(
                event.taskId,
                event.note,
                event.percent,
                event.goal
            )
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
        _uiState.update {
            it.copy(
                showAddTaskDialog = false,
                addingScheduleType = ScheduleType.NONE,
                addingRepeatDays = emptySet(),
                addingDeadlineDate = null,
                addingSpecificDate = null,
                addingReviewCount = null,
                addingReviewEnabled = true
            )
        }
    }

    fun updateAddingReviewCount(reviewCount: Int?) {
        _uiState.update { it.copy(addingReviewCount = reviewCount) }
    }

    fun updateAddingScheduleType(type: ScheduleType) {
        _uiState.update { it.copy(addingScheduleType = type) }
    }

    fun updateAddingRepeatDays(days: Set<Int>) {
        _uiState.update { it.copy(addingRepeatDays = days) }
    }

    fun updateAddingDeadlineDate(date: LocalDate?) {
        _uiState.update { it.copy(addingDeadlineDate = date) }
    }

    fun updateAddingSpecificDate(date: LocalDate?) {
        _uiState.update { it.copy(addingSpecificDate = date) }
    }

    fun showEditTaskDialog(task: Task) {
        _uiState.update {
            it.copy(
                showEditTaskDialog = true,
                editingTask = task,
                editingScheduleType = task.scheduleType,
                editingRepeatDays = task.repeatDays,
                editingDeadlineDate = task.deadlineDate,
                editingSpecificDate = task.specificDate,
                editingReviewCount = task.reviewCount,
                editingReviewEnabled = task.reviewEnabled
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
                editingSpecificDate = null,
                editingReviewCount = null,
                editingReviewEnabled = true
            )
        }
    }

    fun updateEditingReviewCount(reviewCount: Int?) {
        _uiState.update { it.copy(editingReviewCount = reviewCount) }
    }

    fun updateAddingReviewEnabled(enabled: Boolean) {
        _uiState.update { it.copy(addingReviewEnabled = enabled) }
    }

    fun updateEditingReviewEnabled(enabled: Boolean) {
        _uiState.update { it.copy(editingReviewEnabled = enabled) }
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

    fun addGroup(name: String, colorHex: String, hasDeadline: Boolean = false, deadlineDate: LocalDate? = null) {
        viewModelScope.launch {
            val group = SubjectGroup(
                name = name,
                colorHex = colorHex,
                hasDeadline = hasDeadline,
                deadlineDate = if (hasDeadline) deadlineDate else null
            )
            subjectGroupRepository.insertGroup(group)
                .onSuccess { id ->
                    hideAddGroupDialog()
                    // 新しく作成したグループを選択
                    subjectGroupRepository.getGroupById(id)
                        .onSuccess { newGroup ->
                            newGroup?.let { selectGroup(it) }
                        }
                }
        }
    }

    fun updateGroup(group: SubjectGroup) {
        viewModelScope.launch {
            subjectGroupRepository.updateGroup(group)
                .onSuccess { hideEditGroupDialog() }
        }
    }

    fun deleteGroup(group: SubjectGroup) {
        viewModelScope.launch {
            subjectGroupRepository.deleteGroup(group)
                .onSuccess {
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
    }

    fun addTask(
        name: String,
        workDurationMinutes: Int?,
        scheduleType: ScheduleType = ScheduleType.NONE,
        repeatDays: Set<Int> = emptySet(),
        deadlineDate: LocalDate? = null,
        specificDate: LocalDate? = null,
        reviewCount: Int? = null,
        reviewEnabled: Boolean = true
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
                specificDate = specificDate,
                reviewCount = reviewCount,
                reviewEnabled = reviewEnabled
            )
            taskRepository.insertTask(task)
                .onSuccess { hideAddTaskDialog() }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
                .onSuccess { hideEditTaskDialog() }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
                .onSuccess { hideEditTaskDialog() }
        }
    }

    fun updateTaskProgress(taskId: Long, note: String?, percent: Int?, goal: String?) {
        viewModelScope.launch {
            taskRepository.updateProgress(taskId, note, percent, goal)
        }
    }
}
