package com.zenith.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.app.domain.model.PomodoroSettings
import com.zenith.app.domain.model.SubscriptionStatus
import com.zenith.app.domain.repository.SettingsRepository
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
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val dailyGoalMinutes: Int = 60,
    val reviewIntervalsEnabled: Boolean = true,
    val workDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val cyclesBeforeLongBreak: Int = 4,
    val focusModeEnabled: Boolean = true,
    val focusModeStrict: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.getPomodoroSettings()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    notificationsEnabled = settings.notificationsEnabled,
                    reviewIntervalsEnabled = settings.reviewEnabled,
                    workDurationMinutes = settings.workDurationMinutes,
                    shortBreakMinutes = settings.shortBreakMinutes,
                    longBreakMinutes = settings.longBreakMinutes,
                    cyclesBeforeLongBreak = settings.cyclesBeforeLongBreak,
                    focusModeEnabled = settings.focusModeEnabled,
                    focusModeStrict = settings.focusModeStrict
                )
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        saveSettings()
    }

    fun updateDailyGoal(minutes: Int) {
        _uiState.update { it.copy(dailyGoalMinutes = minutes) }
    }

    fun toggleReviewIntervals(enabled: Boolean) {
        _uiState.update { it.copy(reviewIntervalsEnabled = enabled) }
        saveSettings()
    }

    fun updateWorkDuration(minutes: Int) {
        _uiState.update { it.copy(workDurationMinutes = minutes) }
        saveSettings()
    }

    fun updateShortBreak(minutes: Int) {
        _uiState.update { it.copy(shortBreakMinutes = minutes) }
        saveSettings()
    }

    fun updateLongBreak(minutes: Int) {
        _uiState.update { it.copy(longBreakMinutes = minutes) }
        saveSettings()
    }

    fun updateCycles(cycles: Int) {
        _uiState.update { it.copy(cyclesBeforeLongBreak = cycles) }
        saveSettings()
    }

    fun toggleFocusMode(enabled: Boolean) {
        _uiState.update { it.copy(focusModeEnabled = enabled) }
        saveSettings()
    }

    fun toggleFocusModeStrict(strict: Boolean) {
        _uiState.update { it.copy(focusModeStrict = strict) }
        saveSettings()
    }

    private fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            val settings = PomodoroSettings(
                workDurationMinutes = state.workDurationMinutes,
                shortBreakMinutes = state.shortBreakMinutes,
                longBreakMinutes = state.longBreakMinutes,
                cyclesBeforeLongBreak = state.cyclesBeforeLongBreak,
                focusModeEnabled = state.focusModeEnabled,
                focusModeStrict = state.focusModeStrict,
                reviewEnabled = state.reviewIntervalsEnabled,
                notificationsEnabled = state.notificationsEnabled
            )
            settingsRepository.updatePomodoroSettings(settings)
        }
    }

    fun startTrial() {
        viewModelScope.launch {
            premiumManager.startTrial()
        }
    }
}
