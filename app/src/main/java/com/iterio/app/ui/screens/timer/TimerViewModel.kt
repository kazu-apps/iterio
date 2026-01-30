package com.iterio.app.ui.screens.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iterio.app.domain.model.AllowedApp
import com.iterio.app.domain.model.BgmTrack
import com.iterio.app.domain.model.PomodoroSettings
import com.iterio.app.domain.model.SubscriptionStatus
import com.iterio.app.domain.model.Task
import com.iterio.app.domain.repository.TaskRepository
import com.iterio.app.domain.usecase.FinishTimerSessionUseCase
import com.iterio.app.domain.usecase.GetTimerInitialStateUseCase
import com.iterio.app.domain.usecase.StartTimerSessionUseCase
import com.iterio.app.util.InstalledAppsHelper
import java.time.LocalDate
import com.iterio.app.service.BgmState
import timber.log.Timber
import com.iterio.app.service.TimerPhase
import com.iterio.app.service.TimerService
import com.iterio.app.service.TimerState
import com.iterio.app.ui.bgm.BgmManager
import com.iterio.app.ui.premium.PremiumManager
import com.iterio.app.widget.IterioWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val isSessionLockModeEnabled: Boolean = false,
    // 許可アプリ関連
    val installedApps: List<AllowedApp> = emptyList(),
    val defaultAllowedApps: Set<String> = emptySet(),
    val isLoadingApps: Boolean = true,
    // 次のタスク（ループモード用）
    val nextTaskId: Long? = null,
    val nextTaskName: String? = null,
    val isAutoLoopSession: Boolean = false,
    val allTasksCompleted: Boolean = false
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val getTimerInitialStateUseCase: GetTimerInitialStateUseCase,
    private val startTimerSessionUseCase: StartTimerSessionUseCase,
    private val finishTimerSessionUseCase: FinishTimerSessionUseCase,
    private val taskRepository: TaskRepository,
    private val premiumManager: PremiumManager,
    private val bgmManager: BgmManager,
    private val installedAppsHelper: InstalledAppsHelper
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
        val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (!bound) {
            Timber.w("Failed to bind to TimerService")
            _uiState.update { it.copy(isServiceBound = false) }
        }
    }

    private fun loadTaskAndSettings() {
        viewModelScope.launch {
            getTimerInitialStateUseCase(taskId)
                .onSuccess { state ->
                    _uiState.update {
                        it.copy(
                            task = state.task,
                            settings = state.settings,
                            totalCycles = state.settings.cyclesBeforeLongBreak,
                            timeRemainingSeconds = state.totalTimeSeconds,
                            totalTimeSeconds = state.totalTimeSeconds,
                            defaultAllowedApps = state.defaultAllowedApps
                        )
                    }
                    // インストール済みアプリを非同期で読み込み
                    loadInstalledApps()
                }
                .onFailure { error ->
                    Timber.e("Failed to load timer initial state: $error")
                }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true) }
            val apps = installedAppsHelper.getInstalledUserApps()
            _uiState.update {
                it.copy(
                    installedApps = apps,
                    isLoadingApps = false
                )
            }
        }
    }

    private fun updateUiFromServiceState(timerState: TimerState) {
        val previousPhase = _uiState.value.phase
        val previousShowFinishDialog = _uiState.value.showFinishDialog

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
        // Detect transition to session completed state (showFinishDialog becomes true)
        val currentShowFinishDialog = timerState.sessionCompleted && timerState.phase == TimerPhase.IDLE
        if (currentShowFinishDialog && !previousShowFinishDialog) {
            Timber.d("finishSession called: interrupted=false, sessionId=$currentSessionId")
            finishSession(false)
            loadNextTask()
        }
    }

    /**
     * セッション完了時に今日の未完了タスクから次のタスクを探す
     */
    internal fun loadNextTask() {
        viewModelScope.launch {
            val currentTaskId = _uiState.value.task?.id ?: return@launch
            taskRepository.getTodayScheduledTasks(LocalDate.now())
                .firstOrNull()
                ?.let { todayTasks ->
                    val nextTask = todayTasks.firstOrNull { task ->
                        task.id != currentTaskId && !task.isCompletedToday
                    }
                    _uiState.update {
                        it.copy(
                            nextTaskId = nextTask?.id,
                            nextTaskName = nextTask?.name,
                            allTasksCompleted = nextTask == null && it.isAutoLoopSession
                        )
                    }
                }
        }
    }

    fun startTimer(
        lockModeEnabled: Boolean = false,
        cycleCount: Int? = null,
        autoLoopEnabled: Boolean = false,
        allowedApps: Set<String> = emptySet()
    ) {
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
                    totalCycles = cycles,
                    isAutoLoopSession = autoLoopEnabled
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
                focusModeStrict = lockModeEnabled,
                autoLoopEnabled = autoLoopEnabled,
                allowedApps = allowedApps
            )
            bgmManager.onTimerStart()
        } else if (state.isPaused) {
            TimerService.resumeTimer(context)
            bgmManager.onTimerResume()
        }
    }

    private fun createSession(cycles: Int) {
        viewModelScope.launch {
            val task = _uiState.value.task ?: return@launch
            val settings = _uiState.value.settings
            val startTime = sessionStartTime ?: LocalDateTime.now()

            startTimerSessionUseCase(task, settings, cycles, startTime)
                .onSuccess { sessionId ->
                    currentSessionId = sessionId
                    _uiState.update { it.copy(sessionId = sessionId) }
                }
                .onFailure { error ->
                    Timber.e("Failed to create session: $error")
                }
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
            val premium = isPremium.value
            val isSessionCompleted = state.phase == TimerPhase.IDLE

            val params = FinishTimerSessionUseCase.Params(
                sessionId = sessionId,
                task = task,
                settings = settings,
                totalWorkMinutes = state.totalWorkMinutes,
                currentCycle = state.currentCycle,
                totalCycles = state.totalCycles,
                isInterrupted = interrupted,
                isSessionCompleted = isSessionCompleted,
                isPremium = premium,
                reviewIntervals = premiumManager.getReviewIntervals(premium, task.reviewCount)
            )

            finishTimerSessionUseCase(params)
                .onSuccess {
                    IterioWidgetReceiver.sendDataChangedBroadcast(context)
                }
                .onFailure { error ->
                    Timber.e("Failed to finish session: $error")
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
        } catch (e: IllegalArgumentException) {
            // Service was not bound - this is expected if the service wasn't connected
            Timber.d("Service was not bound when attempting to unbind")
        }
    }
}
