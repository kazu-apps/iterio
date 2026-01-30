package com.iterio.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iterio.app.domain.model.DeadlineItem
import com.iterio.app.domain.model.ReviewTask
import com.iterio.app.domain.model.Task
import com.iterio.app.domain.repository.DailyStatsRepository
import com.iterio.app.domain.repository.DayStats
import com.iterio.app.domain.repository.ReviewTaskRepository
import com.iterio.app.domain.repository.StudySessionRepository
import com.iterio.app.domain.repository.SubjectGroupRepository
import com.iterio.app.domain.repository.TaskRepository
import com.iterio.app.domain.usecase.GetTodayTasksUseCase
import com.iterio.app.service.TimerService
import com.iterio.app.service.TimerState
import com.iterio.app.widget.IterioWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import android.content.Context
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val todayMinutes: Int = 0,
    val todayCycles: Int = 0,
    val currentStreak: Int = 0,
    val todayScheduledTasks: List<Task> = emptyList(),
    val todayReviewTasks: List<ReviewTask> = emptyList(),
    val weeklyData: List<DayStats> = emptyList(),
    val upcomingTaskDeadlines: List<DeadlineItem.TaskDeadline> = emptyList(),
    val upcomingGroupDeadlines: List<DeadlineItem.GroupDeadline> = emptyList(),
    val totalTaskDeadlineCount: Int = 0,
    val totalGroupDeadlineCount: Int = 0,
    val totalDeadlineCount: Int = 0,
    val activeTimerState: TimerState? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val studySessionRepository: StudySessionRepository,
    private val dailyStatsRepository: DailyStatsRepository,
    private val reviewTaskRepository: ReviewTaskRepository,
    private val subjectGroupRepository: SubjectGroupRepository,
    private val getTodayTasksUseCase: GetTodayTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadHomeData()
        collectActiveTimerState()
    }

    private fun collectActiveTimerState() {
        viewModelScope.launch {
            TimerService.activeTimerState.collect { timerState ->
                _uiState.update { it.copy(activeTimerState = timerState) }
            }
        }
    }

    fun loadHomeData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val today = LocalDate.now()

            // Observe today's stats reactively
            launch {
                combine(
                    studySessionRepository.observeTotalMinutesForDay(today),
                    studySessionRepository.observeTotalCyclesForDay(today)
                ) { minutes, cycles ->
                    Pair(minutes, cycles)
                }.collect { (minutes, cycles) ->
                    _uiState.update {
                        it.copy(
                            todayMinutes = minutes,
                            todayCycles = cycles
                        )
                    }
                }
            }

            // Load current streak
            launch {
                dailyStatsRepository.getCurrentStreak()
                    .onSuccess { streak ->
                        _uiState.update { it.copy(currentStreak = streak) }
                    }
            }

            // Load today's scheduled tasks and review tasks using UseCase
            launch {
                getTodayTasksUseCase(today).collect { result ->
                    _uiState.update {
                        it.copy(
                            todayScheduledTasks = result.scheduledTasks,
                            todayReviewTasks = result.reviewTasks,
                            isLoading = false
                        )
                    }
                }
            }

            // Load weekly data
            launch {
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                dailyStatsRepository.getWeeklyData(weekStart)
                    .onSuccess { weeklyData ->
                        _uiState.update { it.copy(weeklyData = weeklyData) }
                    }
            }

            // Load upcoming deadline items (tasks + groups, next 30 days)
            launch {
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
                    }.sortedBy { it.deadlineDate }
                    val groupItems = groups.map { group ->
                        DeadlineItem.GroupDeadline(
                            id = group.id,
                            name = group.name,
                            deadlineDate = group.deadlineDate!!,
                            colorHex = group.colorHex
                        )
                    }.sortedBy { it.deadlineDate }
                    Pair(taskItems, groupItems)
                }.collect { (taskItems, groupItems) ->
                    _uiState.update {
                        it.copy(
                            upcomingTaskDeadlines = taskItems.take(3),
                            upcomingGroupDeadlines = groupItems.take(3),
                            totalTaskDeadlineCount = taskItems.size,
                            totalGroupDeadlineCount = groupItems.size,
                            totalDeadlineCount = taskItems.size + groupItems.size
                        )
                    }
                }
            }
        }
    }

    fun toggleReviewTaskComplete(taskId: Long, complete: Boolean) {
        viewModelScope.launch {
            if (complete) {
                reviewTaskRepository.markAsCompleted(taskId)
            } else {
                reviewTaskRepository.markAsIncomplete(taskId)
            }
            IterioWidgetReceiver.sendDataChangedBroadcast(context)
        }
    }
}
