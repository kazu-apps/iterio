package com.iterio.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iterio.app.domain.model.BgmTrack
import com.iterio.app.domain.model.PomodoroSettings
import com.iterio.app.domain.model.ReviewTask
import com.iterio.app.domain.model.SubscriptionStatus
import com.iterio.app.domain.repository.ReviewTaskRepository
import com.iterio.app.domain.repository.SettingsRepository
import com.iterio.app.domain.usecase.UpdatePomodoroSettingsUseCase
import com.iterio.app.ui.bgm.BgmManager
import com.iterio.app.ui.premium.PremiumManager
import com.iterio.app.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val reviewIntervalsEnabled: Boolean = true,
    val defaultReviewCount: Int = 2,
    val workDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val cyclesBeforeLongBreak: Int = 4,
    val focusModeEnabled: Boolean = true,
    val focusModeStrict: Boolean = false,
    val autoLoopEnabled: Boolean = false,
    val allowedAppsCount: Int = 0,
    val language: String = "ja",
    // Review task management
    val reviewTaskTotalCount: Int = 0,
    val reviewTaskIncompleteCount: Int = 0,
    val reviewTasks: List<ReviewTask> = emptyList(),
    val selectedReviewTaskIds: Set<Long> = emptySet(),
    val showReviewTasksDialog: Boolean = false,
    val showDeleteAllReviewTasksDialog: Boolean = false,
    val showDeleteSelectedReviewTasksDialog: Boolean = false,
    // BGM settings
    val bgmTrackId: String? = null,
    val bgmVolume: Float = 0.5f,
    val bgmAutoPlayEnabled: Boolean = true,
    val showBgmSelectorDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val updatePomodoroSettingsUseCase: UpdatePomodoroSettingsUseCase,
    private val premiumManager: PremiumManager,
    private val localeManager: LocaleManager,
    private val reviewTaskRepository: ReviewTaskRepository,
    private val bgmManager: BgmManager
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
        loadReviewTaskCounts()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settingsResult = settingsRepository.getPomodoroSettings()
            val allowedAppsResult = settingsRepository.getAllowedApps()
            val language = localeManager.getCurrentLanguage()

            // BGM設定（BgmManagerから取得）
            val bgmTrackId = bgmManager.selectedTrack.value?.id
            val bgmVolume = bgmManager.volume.value
            val bgmAutoPlayEnabled = bgmManager.autoPlayEnabled.value

            settingsResult.onSuccess { settings ->
                val allowedApps = allowedAppsResult.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notificationsEnabled = settings.notificationsEnabled,
                        reviewIntervalsEnabled = settings.reviewEnabled,
                        defaultReviewCount = settings.defaultReviewCount,
                        workDurationMinutes = settings.workDurationMinutes,
                        shortBreakMinutes = settings.shortBreakMinutes,
                        longBreakMinutes = settings.longBreakMinutes,
                        cyclesBeforeLongBreak = settings.cyclesBeforeLongBreak,
                        focusModeEnabled = settings.focusModeEnabled,
                        focusModeStrict = settings.focusModeStrict,
                        autoLoopEnabled = settings.autoLoopEnabled,
                        allowedAppsCount = allowedApps.size,
                        language = language,
                        bgmTrackId = bgmTrackId,
                        bgmVolume = bgmVolume,
                        bgmAutoPlayEnabled = bgmAutoPlayEnabled
                    )
                }
            }
        }
    }

    /**
     * イベントを処理
     */
    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleNotifications -> toggleNotifications(event.enabled)
            is SettingsEvent.ToggleReviewIntervals -> toggleReviewIntervals(event.enabled)
            is SettingsEvent.UpdateWorkDuration -> updateWorkDuration(event.minutes)
            is SettingsEvent.UpdateShortBreak -> updateShortBreak(event.minutes)
            is SettingsEvent.UpdateLongBreak -> updateLongBreak(event.minutes)
            is SettingsEvent.UpdateCycles -> updateCycles(event.cycles)
            is SettingsEvent.ToggleFocusMode -> toggleFocusMode(event.enabled)
            is SettingsEvent.ToggleFocusModeStrict -> toggleFocusModeStrict(event.strict)
            is SettingsEvent.ToggleAutoLoop -> toggleAutoLoop(event.enabled)
            is SettingsEvent.StartTrial -> startTrial()
            is SettingsEvent.UpdateLanguage -> updateLanguage(event.languageCode)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        saveSettings()
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

    fun toggleFocusModeStrict(strict: Boolean): Boolean {
        // Premium機能なので確認（呼び出し元でPremium確認することを期待）
        _uiState.update { it.copy(focusModeStrict = strict) }
        saveSettings()
        return true
    }

    fun toggleAutoLoop(enabled: Boolean): Boolean {
        // Premium機能なので確認（呼び出し元でPremium確認することを期待）
        _uiState.update { it.copy(autoLoopEnabled = enabled) }
        saveSettings()
        return true
    }

    fun updateDefaultReviewCount(count: Int) {
        _uiState.update { it.copy(defaultReviewCount = count) }
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
                autoLoopEnabled = state.autoLoopEnabled,
                reviewEnabled = state.reviewIntervalsEnabled,
                defaultReviewCount = state.defaultReviewCount,
                notificationsEnabled = state.notificationsEnabled
            )
            updatePomodoroSettingsUseCase.updateSettings(settings)
                .onFailure { error ->
                    Timber.e("Failed to save settings: $error")
                }
        }
    }

    fun startTrial() {
        viewModelScope.launch {
            premiumManager.startTrial()
        }
    }

    /**
     * Update the app language.
     *
     * @param languageCode The language code to switch to (e.g., "ja", "en")
     * @param onLanguageChanged Optional callback invoked after language is changed.
     *                          Typically used to call Activity.recreate() for immediate effect.
     */
    fun updateLanguage(languageCode: String, onLanguageChanged: (() -> Unit)? = null) {
        viewModelScope.launch {
            localeManager.setLanguage(languageCode)
            _uiState.update { it.copy(language = languageCode) }
            onLanguageChanged?.invoke()
        }
    }

    // ========== Review Task Management ==========

    private fun loadReviewTaskCounts() {
        viewModelScope.launch {
            val totalCountResult = reviewTaskRepository.getTotalCount()
            val incompleteCountResult = reviewTaskRepository.getIncompleteCount()
            _uiState.update {
                it.copy(
                    reviewTaskTotalCount = totalCountResult.getOrDefault(0),
                    reviewTaskIncompleteCount = incompleteCountResult.getOrDefault(0)
                )
            }
        }
    }

    fun showReviewTasksDialog() {
        viewModelScope.launch {
            reviewTaskRepository.getAllWithDetails().collect { tasks ->
                _uiState.update {
                    it.copy(
                        showReviewTasksDialog = true,
                        reviewTasks = tasks
                    )
                }
            }
        }
    }

    fun hideReviewTasksDialog() {
        _uiState.update {
            it.copy(
                showReviewTasksDialog = false,
                selectedReviewTaskIds = emptySet()
            )
        }
    }

    fun showDeleteAllReviewTasksDialog() {
        _uiState.update {
            it.copy(showDeleteAllReviewTasksDialog = true)
        }
    }

    fun hideDeleteAllReviewTasksDialog() {
        _uiState.update {
            it.copy(showDeleteAllReviewTasksDialog = false)
        }
    }

    fun deleteAllReviewTasks() {
        viewModelScope.launch {
            reviewTaskRepository.deleteAll()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            reviewTaskTotalCount = 0,
                            reviewTaskIncompleteCount = 0,
                            showDeleteAllReviewTasksDialog = false,
                            reviewTasks = emptyList(),
                            selectedReviewTaskIds = emptySet()
                        )
                    }
                }
        }
    }

    fun toggleReviewTaskSelection(taskId: Long) {
        _uiState.update { state ->
            val newSelection = if (taskId in state.selectedReviewTaskIds) {
                state.selectedReviewTaskIds - taskId
            } else {
                state.selectedReviewTaskIds + taskId
            }
            state.copy(selectedReviewTaskIds = newSelection)
        }
    }

    fun selectAllReviewTasks() {
        _uiState.update { state ->
            state.copy(
                selectedReviewTaskIds = state.reviewTasks.map { it.id }.toSet()
            )
        }
    }

    fun clearReviewTaskSelection() {
        _uiState.update { it.copy(selectedReviewTaskIds = emptySet()) }
    }

    fun showDeleteSelectedReviewTasksDialog() {
        _uiState.update { it.copy(showDeleteSelectedReviewTasksDialog = true) }
    }

    fun hideDeleteSelectedReviewTasksDialog() {
        _uiState.update { it.copy(showDeleteSelectedReviewTasksDialog = false) }
    }

    fun deleteSelectedReviewTasks() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedReviewTaskIds.toList()
            if (selectedIds.isEmpty()) return@launch
            reviewTaskRepository.deleteByIds(selectedIds)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            selectedReviewTaskIds = emptySet(),
                            showDeleteSelectedReviewTasksDialog = false
                        )
                    }
                    loadReviewTaskCounts()
                }
        }
    }

    // ========== BGM Settings ==========

    fun updateBgmVolume(volume: Float) {
        bgmManager.setVolume(volume)
        _uiState.update { it.copy(bgmVolume = volume) }
    }

    fun toggleBgmAutoPlay(enabled: Boolean) {
        bgmManager.setAutoPlay(enabled)
        _uiState.update { it.copy(bgmAutoPlayEnabled = enabled) }
    }

    fun showBgmSelector() {
        _uiState.update { it.copy(showBgmSelectorDialog = true) }
    }

    fun hideBgmSelector() {
        _uiState.update { it.copy(showBgmSelectorDialog = false) }
    }

    fun selectBgmTrack(track: BgmTrack?) {
        bgmManager.selectTrack(track)
        _uiState.update {
            it.copy(
                bgmTrackId = track?.id,
                showBgmSelectorDialog = false
            )
        }
    }
}
