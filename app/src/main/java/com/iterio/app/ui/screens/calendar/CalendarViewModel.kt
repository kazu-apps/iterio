package com.iterio.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iterio.app.domain.model.DailyStats
import com.iterio.app.domain.model.ReviewTask
import com.iterio.app.domain.model.SubscriptionStatus
import com.iterio.app.domain.model.Task
import com.iterio.app.domain.repository.DailyStatsRepository
import com.iterio.app.domain.repository.ReviewTaskRepository
import com.iterio.app.domain.repository.TaskRepository
import com.iterio.app.ui.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = true,
    val currentMonth: YearMonth = YearMonth.now(),
    val dailyStats: Map<LocalDate, DailyStats> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val taskCountByDate: Map<LocalDate, Int> = emptyMap(),
    val groupColorsByDate: Map<LocalDate, List<String>> = emptyMap(),
    val selectedDateTasks: List<Task> = emptyList(),
    val selectedDateReviewTasks: List<ReviewTask> = emptyList()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val dailyStatsRepository: DailyStatsRepository,
    private val taskRepository: TaskRepository,
    private val reviewTaskRepository: ReviewTaskRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var monthLoadJob: Job? = null
    private var dateLoadJob: Job? = null

    val subscriptionStatus: StateFlow<SubscriptionStatus> = premiumManager.subscriptionStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubscriptionStatus()
        )

    val isPremium: StateFlow<Boolean> = subscriptionStatus
        .map { it.isPremium }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        loadMonthData(YearMonth.now())
    }

    fun loadMonthData(yearMonth: YearMonth) {
        monthLoadJob?.cancel()
        monthLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentMonth = yearMonth) }

            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            // Load daily stats
            launch {
                dailyStatsRepository.getStatsBetweenDates(startDate, endDate).collect { statsList ->
                    val statsMap = statsList.associateBy { it.date }
                    _uiState.update { it.copy(isLoading = false, dailyStats = statsMap) }
                }
            }

            // Load task counts for the month (通常タスク + 復習タスク) - Flow でリアクティブに監視
            launch {
                combine(
                    taskRepository.observeTaskCountByDateRange(startDate, endDate),
                    reviewTaskRepository.observeTaskCountByDateRange(startDate, endDate)
                ) { regularTaskCounts, reviewTaskCounts ->
                    (regularTaskCounts.keys + reviewTaskCounts.keys)
                        .associateWith { date ->
                            (regularTaskCounts[date] ?: 0) + (reviewTaskCounts[date] ?: 0)
                        }
                }.collect { combinedCounts ->
                    _uiState.update { it.copy(taskCountByDate = combinedCounts) }
                }
            }

            // Load group colors for calendar dots
            launch {
                combine(
                    taskRepository.observeGroupColorsByDateRange(startDate, endDate),
                    reviewTaskRepository.observeGroupColorsByDateRange(startDate, endDate)
                ) { regularColors, reviewColors ->
                    (regularColors.keys + reviewColors.keys).associateWith { date ->
                        regularColors[date].orEmpty() + reviewColors[date].orEmpty()
                    }
                }.collect { combinedColors ->
                    _uiState.update { it.copy(groupColorsByDate = combinedColors) }
                }
            }
        }
    }

    fun previousMonth() {
        val newMonth = _uiState.value.currentMonth.minusMonths(1)
        loadMonthData(newMonth)
    }

    fun nextMonth() {
        val newMonth = _uiState.value.currentMonth.plusMonths(1)
        loadMonthData(newMonth)
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadTasksForDate(date)
    }

    private fun loadTasksForDate(date: LocalDate) {
        dateLoadJob?.cancel()
        dateLoadJob = viewModelScope.launch {
            launch {
                taskRepository.observeTasksForDate(date).collect { tasks ->
                    _uiState.update { it.copy(selectedDateTasks = tasks) }
                }
            }

            launch {
                reviewTaskRepository.getAllTasksForDate(date).collect { reviewTasks ->
                    _uiState.update { it.copy(selectedDateReviewTasks = reviewTasks) }
                }
            }
        }
    }

    fun clearSelection() {
        dateLoadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedDate = null,
                selectedDateTasks = emptyList(),
                selectedDateReviewTasks = emptyList()
            )
        }
    }

    fun toggleReviewTaskComplete(taskId: Long, complete: Boolean) {
        viewModelScope.launch {
            if (complete) {
                reviewTaskRepository.markAsCompleted(taskId)
            } else {
                reviewTaskRepository.markAsIncomplete(taskId)
            }
        }
    }

    fun startTrial() {
        viewModelScope.launch {
            premiumManager.startTrial()
        }
    }
}
