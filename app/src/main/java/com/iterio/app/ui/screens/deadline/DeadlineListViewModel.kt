package com.iterio.app.ui.screens.deadline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iterio.app.domain.model.DeadlineItem
import com.iterio.app.domain.repository.SubjectGroupRepository
import com.iterio.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DeadlineListUiState(
    val allItems: List<DeadlineItem> = emptyList(),
    val filteredItems: List<DeadlineItem> = emptyList(),
    val selectedFilter: DeadlineFilter = DeadlineFilter.ALL,
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val taskCount: Int = 0,
    val groupCount: Int = 0
)

enum class DeadlineFilter {
    ALL, TASKS, GROUPS
}

@HiltViewModel
class DeadlineListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val subjectGroupRepository: SubjectGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeadlineListUiState())
    val uiState: StateFlow<DeadlineListUiState> = _uiState.asStateFlow()

    init {
        loadDeadlineItems()
    }

    private fun loadDeadlineItems() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val endDate = today.plusDays(30)

            combine(
                taskRepository.getUpcomingDeadlineTasks(today, endDate),
                subjectGroupRepository.getUpcomingDeadlineGroups(today, endDate)
            ) { tasks, groups ->
                val taskItems = tasks.map { task ->
                    DeadlineItem.TaskDeadline(
                        id = task.id,
                        name = task.name,
                        deadlineDate = task.deadlineDate!!,
                        colorHex = task.groupColor ?: "#6C63FF",
                        groupName = task.groupName,
                        taskId = task.id
                    )
                }
                val groupItems = groups.map { group ->
                    DeadlineItem.GroupDeadline(
                        id = group.id,
                        name = group.name,
                        deadlineDate = group.deadlineDate!!,
                        colorHex = group.colorHex
                    )
                }
                (taskItems + groupItems).sortedBy { it.deadlineDate }
            }.collect { allItems ->
                val currentFilter = _uiState.value.selectedFilter
                val filtered = applyFilter(allItems, currentFilter)

                _uiState.update {
                    it.copy(
                        allItems = allItems,
                        filteredItems = filtered,
                        isLoading = false,
                        totalCount = allItems.size,
                        taskCount = allItems.count { item -> item is DeadlineItem.TaskDeadline },
                        groupCount = allItems.count { item -> item is DeadlineItem.GroupDeadline }
                    )
                }
            }
        }
    }

    fun updateFilter(filter: DeadlineFilter) {
        val allItems = _uiState.value.allItems
        val filtered = applyFilter(allItems, filter)

        _uiState.update {
            it.copy(
                selectedFilter = filter,
                filteredItems = filtered
            )
        }
    }

    private fun applyFilter(
        items: List<DeadlineItem>,
        filter: DeadlineFilter
    ): List<DeadlineItem> = when (filter) {
        DeadlineFilter.ALL -> items
        DeadlineFilter.TASKS -> items.filterIsInstance<DeadlineItem.TaskDeadline>()
        DeadlineFilter.GROUPS -> items.filterIsInstance<DeadlineItem.GroupDeadline>()
    }
}
