package com.zenith.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.app.domain.model.DailyStats
import com.zenith.app.domain.model.ReviewTask
import com.zenith.app.domain.model.Task
import com.zenith.app.domain.repository.DailyStatsRepository
import com.zenith.app.domain.repository.DayStats
import com.zenith.app.domain.repository.ReviewTaskRepository
import com.zenith.app.domain.repository.StudySessionRepository
import com.zenith.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
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
    val weeklyData: List<DayStats> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val studySessionRepository: StudySessionRepository,
    private val dailyStatsRepository: DailyStatsRepository,
    private val reviewTaskRepository: ReviewTaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val today = LocalDate.now()

            // Load today's stats
            launch {
                val minutes = studySessionRepository.getTotalMinutesForDay(today)
                val cycles = studySessionRepository.getTotalCyclesForDay(today)
                _uiState.update {
                    it.copy(
                        todayMinutes = minutes,
                        todayCycles = cycles
                    )
                }
            }

            // Load current streak
            launch {
                val streak = dailyStatsRepository.getCurrentStreak()
                _uiState.update { it.copy(currentStreak = streak) }
            }

            // Load today's scheduled tasks
            launch {
                taskRepository.getTodayScheduledTasks(today).collect { tasks ->
                    _uiState.update { it.copy(todayScheduledTasks = tasks) }
                }
            }

            // Load today's review tasks
            launch {
                reviewTaskRepository.getOverdueAndTodayTasks(today).collect { reviews ->
                    _uiState.update {
                        it.copy(
                            todayReviewTasks = reviews,
                            isLoading = false
                        )
                    }
                }
            }

            // Load weekly data
            launch {
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weeklyData = dailyStatsRepository.getWeeklyData(weekStart)
                _uiState.update { it.copy(weeklyData = weeklyData) }
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
        }
    }
}
