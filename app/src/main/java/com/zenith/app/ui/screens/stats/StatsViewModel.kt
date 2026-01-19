package com.zenith.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.app.domain.model.SubscriptionStatus
import com.zenith.app.domain.repository.DailyStatsRepository
import com.zenith.app.domain.repository.StudySessionRepository
import com.zenith.app.ui.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class DayStats(
    val dayOfWeek: String,
    val minutes: Int
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val todayMinutes: Int = 0,
    val todaySessions: Int = 0,
    val totalSessions: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val thisWeekMinutes: Int = 0,
    val thisMonthMinutes: Int = 0,
    val averageDailyMinutes: Int = 0,
    val weeklyData: List<DayStats> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val dailyStatsRepository: DailyStatsRepository,
    private val studySessionRepository: StudySessionRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

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
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val today = LocalDate.now()
            val weekStart = today.with(DayOfWeek.MONDAY)
            val monthStart = today.withDayOfMonth(1)

            // Today's stats (for free users)
            val todayMinutes = studySessionRepository.getTotalMinutesForDay(today)
            val todaySessions = studySessionRepository.getTotalCyclesForDay(today)

            val currentStreak = dailyStatsRepository.getCurrentStreak()
            val maxStreak = dailyStatsRepository.getMaxStreak()
            val thisWeekMinutes = dailyStatsRepository.getTotalMinutesBetweenDates(weekStart, today)
            val thisMonthMinutes = dailyStatsRepository.getTotalMinutesBetweenDates(monthStart, today)
            val totalSessions = studySessionRepository.getAllSessions().first().size

            // Calculate average (last 30 days)
            val thirtyDaysAgo = today.minusDays(30)
            val last30DaysMinutes = dailyStatsRepository.getTotalMinutesBetweenDates(thirtyDaysAgo, today)
            val averageDaily = last30DaysMinutes / 30

            // Get weekly data for chart
            val dayLabels = listOf("月", "火", "水", "木", "金", "土", "日")
            val weeklyData = (0 until 7).map { dayOffset ->
                val date = weekStart.plusDays(dayOffset.toLong())
                val minutes = if (date <= today) {
                    dailyStatsRepository.getTotalMinutesBetweenDates(date, date)
                } else {
                    0
                }
                DayStats(
                    dayOfWeek = dayLabels[dayOffset],
                    minutes = minutes
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    todayMinutes = todayMinutes,
                    todaySessions = todaySessions,
                    totalSessions = totalSessions,
                    currentStreak = currentStreak,
                    maxStreak = maxStreak,
                    thisWeekMinutes = thisWeekMinutes,
                    thisMonthMinutes = thisMonthMinutes,
                    averageDailyMinutes = averageDaily,
                    weeklyData = weeklyData
                )
            }
        }
    }

    fun refresh() {
        loadStats()
    }

    fun startTrial() {
        viewModelScope.launch {
            premiumManager.startTrial()
        }
    }
}
