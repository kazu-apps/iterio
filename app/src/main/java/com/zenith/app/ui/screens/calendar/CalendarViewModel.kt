package com.zenith.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.app.domain.model.DailyStats
import com.zenith.app.domain.model.SubscriptionStatus
import com.zenith.app.domain.repository.DailyStatsRepository
import com.zenith.app.ui.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = true,
    val currentMonth: YearMonth = YearMonth.now(),
    val dailyStats: Map<LocalDate, DailyStats> = emptyMap(),
    val selectedDate: LocalDate? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val dailyStatsRepository: DailyStatsRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentMonth = yearMonth) }

            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            dailyStatsRepository.getStatsBetweenDates(startDate, endDate).collect { statsList ->
                val statsMap = statsList.associateBy { it.date }
                _uiState.update { it.copy(isLoading = false, dailyStats = statsMap) }
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
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedDate = null) }
    }

    fun startTrial() {
        viewModelScope.launch {
            premiumManager.startTrial()
        }
    }
}
