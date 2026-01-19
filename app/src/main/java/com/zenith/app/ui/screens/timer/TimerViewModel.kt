package com.zenith.app.ui.screens.timer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.app.domain.model.BgmTrack
import com.zenith.app.domain.model.PomodoroSettings
import com.zenith.app.domain.model.ReviewTask
import com.zenith.app.domain.model.StudySession
import com.zenith.app.domain.model.SubscriptionStatus
import com.zenith.app.domain.model.Task
import com.zenith.app.domain.repository.ReviewTaskRepository
import com.zenith.app.domain.repository.SettingsRepository
import com.zenith.app.domain.repository.StudySessionRepository
import com.zenith.app.domain.repository.TaskRepository
import com.zenith.app.service.BgmState
import com.zenith.app.service.TimerPhase
import com.zenith.app.service.TimerService
import com.zenith.app.service.TimerState
import com.zenith.app.ui.bgm.BgmManager
import com.zenith.app.ui.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class TimerUiState(
    val task: Task? = null,
    val settings: PomodoroSettings = PomodoroSettings(),
    val phase: TimerPhase = TimerPhase.IDLE,
    val timeRemainingSeconds: Int = 0,
    val totalTimeSeconds: Int = 0,
    val currentCycle: Int = 1,
    val totalCycles: Int = 4,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val sessionId: Long? = null,
    val totalWorkMinutes: Int = 0,
    val showFinishDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val isServiceBound: Boolean = false,
    val isSessionLockModeEnabled: Boolean = false
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val studySessionRepository: StudySessionRepository,
    private val reviewTaskRepository: ReviewTaskRepository,
    private val premiumManager: PremiumManager,
    private val bgmManager: BgmManager
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

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

    // BGM関連
    val bgmState: StateFlow<BgmState> = bgmManager.bgmState
    val selectedBgmTrack: StateFlow<BgmTrack?> = bgmManager.selectedTrack
    val bgmVolume: StateFlow<Float> = bgmManager.volume

    fun getAvailableBgmTracks(): List<BgmTrack> = bgmManager.getAvailableTracks()

    fun selectBgmTrack(track: BgmTrack?) {
        if (track != null) {
            bgmManager.play(track)
        } else {
            bgmManager.stop()
        }
    }

    fun setBgmVolume(volume: Float) {
        bgmManager.setVolume(volume)
    }

    fun toggleBgm() {
        if (bgmManager.isPlaying()) {
            bgmManager.pause()
        } else {
            bgmManager.resume()
        }
    }

    private var timerService: TimerService? = null
    private var sessionStartTime: LocalDateTime? = null
    private var currentSessionId: Long? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? TimerService.TimerBinder ?: return
            timerService = binder.getService()
            _uiState.update { it.copy(isServiceBound = true) }

            // Observe timer state from service
            viewModelScope.launch {
                timerService?.timerState?.collect { timerState ->
                    updateUiFromServiceState(timerState)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            _uiState.update { it.copy(isServiceBound = false) }
        }
    }

    init {
        loadTaskAndSettings()
        bindToService()
    }

    private fun bindToService() {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun loadTaskAndSettings() {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            val settings = settingsRepository.getPomodoroSettings()

            // タスク固有の作業時間があればそれを使用、なければ設定のデフォルト値
            val workDurationMinutes = task?.workDurationMinutes ?: settings.workDurationMinutes

            _uiState.update {
                it.copy(
                    task = task,
                    settings = settings,
                    totalCycles = settings.cyclesBeforeLongBreak,
                    timeRemainingSeconds = workDurationMinutes * 60,
                    totalTimeSeconds = workDurationMinutes * 60
                )
            }
        }
    }

    private fun updateUiFromServiceState(timerState: TimerState) {
        val previousPhase = _uiState.value.phase
        val previousSessionCompleted = timerState.sessionCompleted

        _uiState.update {
            it.copy(
                phase = timerState.phase,
                timeRemainingSeconds = timerState.timeRemainingSeconds,
                totalTimeSeconds = timerState.totalTimeSeconds,
                currentCycle = timerState.currentCycle,
                totalCycles = timerState.totalCycles,
                isRunning = timerState.isRunning,
                isPaused = timerState.isPaused,
                totalWorkMinutes = timerState.totalWorkMinutes,
                showFinishDialog = timerState.sessionCompleted && timerState.phase == TimerPhase.IDLE
            )
        }

        // Session completed - save and create review tasks
        if (timerState.sessionCompleted && timerState.phase == TimerPhase.IDLE && !previousSessionCompleted) {
            finishSession(false)
        }
    }

    fun startTimer(lockModeEnabled: Boolean = false, cycleCount: Int? = null) {
        val state = _uiState.value
        val settings = state.settings
        val task = state.task ?: return

        // タスク固有の作業時間があればそれを使用、なければ設定のデフォルト値
        val workDurationMinutes = task.workDurationMinutes ?: settings.workDurationMinutes
        // セッションごとのサイクル数、指定がなければ設定のデフォルト値
        val cycles = cycleCount ?: settings.cyclesBeforeLongBreak

        if (state.phase == TimerPhase.IDLE) {
            sessionStartTime = LocalDateTime.now()
            createSession(cycles)

            // セッションごとのロックモード状態とサイクル数を保存
            _uiState.update {
                it.copy(
                    isSessionLockModeEnabled = lockModeEnabled,
                    totalCycles = cycles
                )
            }

            TimerService.startTimer(
                context = context,
                taskId = task.id,
                taskName = task.name,
                workDuration = workDurationMinutes,
                shortBreak = settings.shortBreakMinutes,
                longBreak = settings.longBreakMinutes,
                cycles = cycles,
                focusModeEnabled = settings.focusModeEnabled,
                focusModeStrict = lockModeEnabled
            )
        } else if (state.isPaused) {
            TimerService.resumeTimer(context)
        }
    }

    private fun createSession(cycles: Int) {
        viewModelScope.launch {
            val task = _uiState.value.task ?: return@launch
            val settings = _uiState.value.settings
            // タスク固有の作業時間があればそれを使用
            val workDurationMinutes = task.workDurationMinutes ?: settings.workDurationMinutes
            val session = StudySession(
                taskId = task.id,
                startedAt = sessionStartTime ?: LocalDateTime.now(),
                plannedDurationMinutes = workDurationMinutes * cycles
            )
            currentSessionId = studySessionRepository.insertSession(session)
            _uiState.update { it.copy(sessionId = currentSessionId) }
        }
    }

    fun pauseTimer() {
        TimerService.pauseTimer(context)
        bgmManager.onTimerPause()
    }

    fun resumeTimer() {
        TimerService.resumeTimer(context)
        bgmManager.onTimerResume()
    }

    fun showCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = true) }
    }

    fun hideCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false) }
    }

    fun cancelTimer(interrupted: Boolean = true) {
        finishSession(interrupted)
        TimerService.stopTimer(context)
        bgmManager.onTimerStop()
        _uiState.update {
            it.copy(
                phase = TimerPhase.IDLE,
                isRunning = false,
                isPaused = false,
                showCancelDialog = false
            )
        }
    }

    fun hideFinishDialog() {
        _uiState.update { it.copy(showFinishDialog = false) }
        TimerService.stopTimer(context)
        bgmManager.onTimerStop()
    }

    private fun finishSession(interrupted: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val sessionId = currentSessionId ?: state.sessionId ?: return@launch
            val task = state.task ?: return@launch
            val settings = state.settings

            // Finish session
            studySessionRepository.finishSession(
                id = sessionId,
                durationMinutes = state.totalWorkMinutes,
                cycles = if (state.phase == TimerPhase.IDLE) state.totalCycles else state.currentCycle,
                interrupted = interrupted
            )

            // Create review tasks if enabled and not interrupted
            if (!interrupted && settings.reviewEnabled) {
                // Premium状態に応じた復習間隔を使用
                val premium = isPremium.value
                val intervals = premiumManager.getReviewIntervals(premium)
                val reviewTasks = ReviewTask.generateReviewTasks(
                    studySessionId = sessionId,
                    taskId = task.id,
                    studyDate = LocalDate.now(),
                    intervals = intervals
                )
                reviewTaskRepository.insertAll(reviewTasks)
            }
        }
    }

    fun skipPhase() {
        TimerService.skipPhase(context)
    }

    fun startTrial() {
        viewModelScope.launch {
            premiumManager.startTrial()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            // Service not bound
        }
    }
}
