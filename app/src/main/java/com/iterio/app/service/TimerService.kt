package com.iterio.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.iterio.app.R
import com.iterio.app.domain.model.PomodoroSettings
import com.iterio.app.domain.repository.SettingsRepository
import com.iterio.app.domain.repository.TaskRepository
import com.iterio.app.domain.usecase.FinishTimerSessionUseCase
import com.iterio.app.domain.usecase.StartTimerSessionUseCase
import com.iterio.app.ui.MainActivity
import com.iterio.app.ui.premium.PremiumManager
import com.iterio.app.util.TimeConstants
import com.iterio.app.util.TimerPhaseUtils
import com.iterio.app.widget.IterioWidgetReceiver
import com.iterio.app.widget.IterioWidgetStateHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

enum class TimerPhase {
    WORK,
    SHORT_BREAK,
    LONG_BREAK,
    IDLE
}

/**
 * Timer default values and constraints
 */
object TimerDefaults {
    const val DEFAULT_WORK_MINUTES = 25
    const val DEFAULT_SHORT_BREAK_MINUTES = 5
    const val DEFAULT_LONG_BREAK_MINUTES = 15
    const val DEFAULT_CYCLES = 4
    const val MIN_CYCLES = 1
    const val MAX_CYCLES = 10
    const val MIN_WORK_MINUTES = 1
    const val MAX_WORK_MINUTES = 180
}

data class TimerState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val timeRemainingSeconds: Int = 0,
    val totalTimeSeconds: Int = 0,
    val currentCycle: Int = 1,
    val totalCycles: Int = TimerDefaults.DEFAULT_CYCLES,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val taskId: Long = 0,
    val taskName: String = "",
    val totalWorkMinutes: Int = 0,
    val workDurationMinutes: Int = TimerDefaults.DEFAULT_WORK_MINUTES,
    val shortBreakMinutes: Int = TimerDefaults.DEFAULT_SHORT_BREAK_MINUTES,
    val longBreakMinutes: Int = TimerDefaults.DEFAULT_LONG_BREAK_MINUTES,
    val sessionCompleted: Boolean = false,
    val autoLoopEnabled: Boolean = false,
    val sessionId: Long? = null
)

@AndroidEntryPoint
class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.iterio.app.action.START"
        const val ACTION_PAUSE = "com.iterio.app.action.PAUSE"
        const val ACTION_RESUME = "com.iterio.app.action.RESUME"
        const val ACTION_STOP = "com.iterio.app.action.STOP"
        const val ACTION_SKIP = "com.iterio.app.action.SKIP"

        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_WORK_DURATION = "work_duration"
        const val EXTRA_SHORT_BREAK = "short_break"
        const val EXTRA_LONG_BREAK = "long_break"
        const val EXTRA_CYCLES = "cycles"
        const val EXTRA_FOCUS_MODE_ENABLED = "focus_mode_enabled"
        const val EXTRA_FOCUS_MODE_STRICT = "focus_mode_strict"
        const val EXTRA_AUTO_LOOP_ENABLED = "auto_loop_enabled"
        const val EXTRA_ALLOWED_APPS = "allowed_apps"

        // Static timer state for cross-component observation (HomeScreen etc.)
        private val _activeTimerState = MutableStateFlow<TimerState?>(null)
        val activeTimerState: StateFlow<TimerState?> = _activeTimerState.asStateFlow()

        // Session completion event for navigation (observed by IterioNavHost)
        private val _sessionCompletedTaskId = MutableStateFlow<Long?>(null)
        val sessionCompletedTaskId: StateFlow<Long?> = _sessionCompletedTaskId.asStateFlow()

        fun consumeSessionCompletedEvent() {
            _sessionCompletedTaskId.value = null
        }

        @androidx.annotation.VisibleForTesting
        internal fun resetActiveTimerState() {
            _activeTimerState.value = null
        }

        fun startTimer(
            context: Context,
            taskId: Long,
            taskName: String,
            workDuration: Int,
            shortBreak: Int,
            longBreak: Int,
            cycles: Int,
            focusModeEnabled: Boolean = false,
            focusModeStrict: Boolean = false,
            autoLoopEnabled: Boolean = false,
            allowedApps: Set<String> = emptySet()
        ) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_NAME, taskName)
                putExtra(EXTRA_WORK_DURATION, workDuration)
                putExtra(EXTRA_SHORT_BREAK, shortBreak)
                putExtra(EXTRA_LONG_BREAK, longBreak)
                putExtra(EXTRA_CYCLES, cycles)
                putExtra(EXTRA_FOCUS_MODE_ENABLED, focusModeEnabled)
                putExtra(EXTRA_FOCUS_MODE_STRICT, focusModeStrict)
                putExtra(EXTRA_AUTO_LOOP_ENABLED, autoLoopEnabled)
                putExtra(EXTRA_ALLOWED_APPS, allowedApps.toTypedArray())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun skipPhase(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_SKIP
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var startTimerSessionUseCase: StartTimerSessionUseCase
    @Inject lateinit var finishTimerSessionUseCase: FinishTimerSessionUseCase
    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var premiumManager: PremiumManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sessionIdDeferred = CompletableDeferred<Long?>(null)

    private val binder = TimerBinder()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private var notificationManager: NotificationManager? = null
    private var lastNotificationText: String? = null

    // Sound effects for countdown
    private var soundPool: SoundPool? = null
    private var tickSoundId: Int = 0
    private var completeSoundId: Int = 0
    private var soundPoolLoaded: Boolean = false

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private fun updateTimerState(state: TimerState) {
        _timerState.value = state
        _activeTimerState.value = if (state.isRunning || state.isPaused) state else null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        createNotificationChannel()
        initSoundPool()
    }

    private fun initSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build()
                .apply {
                    setOnLoadCompleteListener { _, _, status ->
                        if (status == 0) {
                            soundPoolLoaded = true
                        }
                    }
                }

            // Load sound effects
            tickSoundId = soundPool?.load(this, R.raw.countdown_tick, 1) ?: 0
            completeSoundId = soundPool?.load(this, R.raw.timer_complete, 1) ?: 0
        } catch (e: Exception) {
            // Sound loading failed, continue without sounds
            soundPoolLoaded = false
        }
    }

    private fun playTickSound() {
        if (soundPoolLoaded && tickSoundId != 0) {
            soundPool?.play(tickSoundId, 0.7f, 0.7f, 1, 0, 1f)
        }
    }

    private fun playCompleteSound() {
        if (soundPoolLoaded && completeSoundId != 0) {
            soundPool?.play(completeSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0)
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: ""
                val workDuration = intent.getIntExtra(EXTRA_WORK_DURATION, TimerDefaults.DEFAULT_WORK_MINUTES)
                val shortBreak = intent.getIntExtra(EXTRA_SHORT_BREAK, TimerDefaults.DEFAULT_SHORT_BREAK_MINUTES)
                val longBreak = intent.getIntExtra(EXTRA_LONG_BREAK, TimerDefaults.DEFAULT_LONG_BREAK_MINUTES)
                val cycles = intent.getIntExtra(EXTRA_CYCLES, TimerDefaults.DEFAULT_CYCLES)
                val focusModeEnabled = intent.getBooleanExtra(EXTRA_FOCUS_MODE_ENABLED, false)
                val focusModeStrict = intent.getBooleanExtra(EXTRA_FOCUS_MODE_STRICT, false)
                val autoLoopEnabled = intent.getBooleanExtra(EXTRA_AUTO_LOOP_ENABLED, false)
                val allowedApps = intent.getStringArrayExtra(EXTRA_ALLOWED_APPS)?.toSet() ?: emptySet()

                startTimerInternal(taskId, taskName, workDuration, shortBreak, longBreak, cycles, focusModeEnabled, focusModeStrict, autoLoopEnabled, allowedApps)
            }
            ACTION_PAUSE -> pauseTimerInternal()
            ACTION_RESUME -> resumeTimerInternal()
            ACTION_STOP -> stopTimerInternal()
            ACTION_SKIP -> skipPhaseInternal()
        }

        return START_STICKY
    }

    private var focusModeEnabled = false
    private var focusModeStrict = false
    private var currentAllowedApps: Set<String> = emptySet()

    private fun startTimerInternal(
        taskId: Long,
        taskName: String,
        workDuration: Int,
        shortBreak: Int,
        longBreak: Int,
        cycles: Int,
        focusMode: Boolean,
        focusStrict: Boolean,
        autoLoop: Boolean = false,
        allowedApps: Set<String> = emptySet()
    ) {
        val totalSeconds = workDuration * TimeConstants.SECONDS_PER_MINUTE

        // Store focus mode settings
        focusModeEnabled = focusMode
        focusModeStrict = focusStrict
        currentAllowedApps = allowedApps

        updateTimerState(TimerState(
            phase = TimerPhase.WORK,
            timeRemainingSeconds = totalSeconds,
            totalTimeSeconds = totalSeconds,
            currentCycle = 1,
            totalCycles = cycles,
            isRunning = true,
            isPaused = false,
            taskId = taskId,
            taskName = taskName,
            totalWorkMinutes = 0,
            workDurationMinutes = workDuration,
            shortBreakMinutes = shortBreak,
            longBreakMinutes = longBreak,
            sessionCompleted = false,
            autoLoopEnabled = autoLoop
        ))

        // Create session in DB
        createSession(taskId, cycles)

        // Start focus mode if enabled and service is available
        if (focusModeEnabled && FocusModeService.isServiceRunning.value) {
            FocusModeService.startFocusMode(focusModeStrict, currentAllowedApps)
        } else if (focusModeEnabled && !FocusModeService.isServiceRunning.value) {
            Timber.w("Focus mode enabled but Accessibility Service is not running. Focus mode will be skipped.")
            Toast.makeText(
                this,
                getString(R.string.focus_mode_enable_accessibility),
                Toast.LENGTH_LONG
            ).show()
        }

        // Show lock overlay for complete lock mode
        if (focusModeEnabled && focusModeStrict && LockOverlayService.canDrawOverlays(this)) {
            LockOverlayService.showOverlay(this)
            updateOverlayTime()
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startCountDown()

        // Update widget
        updateWidgetState()
    }

    private fun pauseTimerInternal() {
        countDownTimer?.cancel()
        updateTimerState(_timerState.value.copy(isRunning = false, isPaused = true))
        updateNotification()
    }

    private fun resumeTimerInternal() {
        updateTimerState(_timerState.value.copy(isRunning = true, isPaused = false))
        startCountDown()
        updateNotification()
    }

    private fun stopTimerInternal() {
        countDownTimer?.cancel()

        // Save interrupted session if timer was active
        val state = _timerState.value
        if ((state.isRunning || state.isPaused) && state.sessionId != null) {
            saveInterruptedSession()
        }

        updateTimerState(TimerState())
        _sessionCompletedTaskId.value = null

        // Stop focus mode
        FocusModeService.stopFocusMode()

        // Hide lock overlay
        LockOverlayService.hideOverlay(this)

        // Update widget
        updateWidgetState()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun skipPhaseInternal() {
        countDownTimer?.cancel()
        onPhaseComplete()
    }

    private fun startCountDown() {
        countDownTimer?.cancel()

        val remainingMillis = _timerState.value.timeRemainingSeconds * TimeConstants.MILLIS_PER_SECOND

        countDownTimer = object : CountDownTimer(remainingMillis, TimeConstants.MILLIS_PER_SECOND) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / TimeConstants.MILLIS_PER_SECOND).toInt()
                updateTimerState(_timerState.value.copy(timeRemainingSeconds = seconds))

                // Only rebuild notification when display text changes or in final countdown
                val minutes = seconds / TimeConstants.SECONDS_PER_MINUTE
                val displayText = String.format("%02d:%02d", minutes, seconds % TimeConstants.SECONDS_PER_MINUTE)
                if (displayText != lastNotificationText || seconds <= 10) {
                    lastNotificationText = displayText
                    updateNotification()
                }

                updateOverlayTime()

                // Play countdown tick sound at 10 and 5 seconds
                if (seconds == 10 || seconds == 5) {
                    playTickSound()
                }
            }

            override fun onFinish() {
                onPhaseComplete()
            }
        }.start()
    }

    private fun updateOverlayTime() {
        if (!focusModeEnabled || !focusModeStrict) return

        val state = _timerState.value
        val minutes = state.timeRemainingSeconds / TimeConstants.SECONDS_PER_MINUTE
        val seconds = state.timeRemainingSeconds % TimeConstants.SECONDS_PER_MINUTE
        val timeText = String.format("%02d:%02d", minutes, seconds)

        val phaseText = TimerPhaseUtils.getPhaseDisplayText(this, state.phase, state.sessionCompleted)

        LockOverlayService.updateOverlayTime(this, timeText, phaseText)
    }

    private fun onPhaseComplete() {
        // Play completion sound when phase ends
        playCompleteSound()

        val state = _timerState.value

        when (state.phase) {
            TimerPhase.WORK -> {
                val newWorkMinutes = state.totalWorkMinutes + state.workDurationMinutes

                if (state.currentCycle >= state.totalCycles) {
                    // 最後のサイクル完了 -> セッション完了
                    // Session complete
                    updateTimerState(state.copy(
                        phase = TimerPhase.IDLE,
                        isRunning = false,
                        sessionCompleted = true,
                        totalWorkMinutes = newWorkMinutes
                    ))
                    _sessionCompletedTaskId.value = state.taskId

                    // Save completed session to DB (service layer — UI-independent)
                    saveCompletedSession(newWorkMinutes)

                    // Stop focus mode and hide overlay
                    FocusModeService.stopFocusMode()
                    LockOverlayService.hideOverlay(this)

                    updateNotification()
                    updateWidgetState()
                    // Keep service running briefly to allow UI to read final state
                } else {
                    // Short break - 休憩中はフォーカスモード・ロックを解除
                    FocusModeService.stopFocusMode()
                    LockOverlayService.hideOverlay(this)

                    val shortBreakSeconds = state.shortBreakMinutes * TimeConstants.SECONDS_PER_MINUTE
                    updateTimerState(state.copy(
                        phase = TimerPhase.SHORT_BREAK,
                        timeRemainingSeconds = shortBreakSeconds,
                        totalTimeSeconds = shortBreakSeconds,
                        totalWorkMinutes = newWorkMinutes
                    ))
                    updateNotification()
                    updateWidgetState()
                    startCountDown()
                }
            }

            TimerPhase.SHORT_BREAK -> {
                // Next work cycle - 作業復帰時にフォーカスモード・ロックを再有効化
                if (focusModeEnabled && FocusModeService.isServiceRunning.value) {
                    FocusModeService.startFocusMode(focusModeStrict, currentAllowedApps)
                }
                if (focusModeEnabled && focusModeStrict && LockOverlayService.canDrawOverlays(this)) {
                    LockOverlayService.showOverlay(this)
                }

                val workSeconds = state.workDurationMinutes * TimeConstants.SECONDS_PER_MINUTE
                updateTimerState(state.copy(
                    phase = TimerPhase.WORK,
                    timeRemainingSeconds = workSeconds,
                    totalTimeSeconds = workSeconds,
                    currentCycle = state.currentCycle + 1
                ))
                updateNotification()
                updateWidgetState()
                updateOverlayTime()
                startCountDown()
            }

            TimerPhase.LONG_BREAK -> {
                // 長休憩完了後 - 作業復帰時にフォーカスモード・ロックを再有効化
                if (focusModeEnabled && FocusModeService.isServiceRunning.value) {
                    FocusModeService.startFocusMode(focusModeStrict, currentAllowedApps)
                }
                if (focusModeEnabled && focusModeStrict && LockOverlayService.canDrawOverlays(this)) {
                    LockOverlayService.showOverlay(this)
                }

                val workSeconds = state.workDurationMinutes * TimeConstants.SECONDS_PER_MINUTE
                updateTimerState(state.copy(
                    phase = TimerPhase.WORK,
                    timeRemainingSeconds = workSeconds,
                    totalTimeSeconds = workSeconds,
                    currentCycle = 1,
                    totalWorkMinutes = 0
                ))
                updateNotification()
                updateWidgetState()
                updateOverlayTime()
                startCountDown()
            }

            TimerPhase.IDLE -> {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val state = _timerState.value

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, TimerService::class.java).apply {
            action = if (state.isRunning) ACTION_PAUSE else ACTION_RESUME
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = state.timeRemainingSeconds / TimeConstants.SECONDS_PER_MINUTE
        val seconds = state.timeRemainingSeconds % TimeConstants.SECONDS_PER_MINUTE
        val timeText = String.format("%02d:%02d", minutes, seconds)

        val phaseText = TimerPhaseUtils.getPhaseDisplayText(this, state.phase, state.sessionCompleted)

        val title = if (state.taskName.isNotEmpty()) {
            "${state.taskName} - $phaseText"
        } else {
            phaseText
        }

        val contentText = getString(
            R.string.notification_cycle_format,
            timeText,
            state.currentCycle,
            state.totalCycles
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (state.phase != TimerPhase.IDLE) {
            val pauseResumeText = if (state.isRunning) {
                getString(R.string.notification_action_pause)
            } else {
                getString(R.string.notification_action_resume)
            }
            builder.addAction(
                if (state.isRunning) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground,
                pauseResumeText,
                pauseResumePendingIntent
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.notification_action_stop),
                stopPendingIntent
            )
        }

        return builder.build()
    }

    private fun updateNotification() {
        notificationManager?.notify(NOTIFICATION_ID, createNotification())
    }

    private var lastWidgetUpdateSeconds = -1

    private fun updateWidgetState() {
        val state = _timerState.value

        // Save timer state to SharedPreferences for widget access
        IterioWidgetStateHelper.saveTimerStateToPrefs(
            this,
            state.phase,
            state.timeRemainingSeconds,
            state.isRunning
        )

        // Throttle widget updates to every 10 seconds during countdown
        val shouldUpdate = state.timeRemainingSeconds != lastWidgetUpdateSeconds &&
                (state.timeRemainingSeconds % 10 == 0 ||
                        state.phase == TimerPhase.IDLE ||
                        !state.isRunning)

        if (shouldUpdate) {
            lastWidgetUpdateSeconds = state.timeRemainingSeconds
            IterioWidgetStateHelper.updateWidget(this)
        }
    }

    // Note: sessionIdDeferred is reassigned on each call. The UI layer enforces that only
    // one timer session is active at a time, so concurrent calls to createSession() cannot occur.
    private fun createSession(taskId: Long, cycles: Int) {
        sessionIdDeferred = CompletableDeferred()
        serviceScope.launch {
            val task = taskRepository.getTaskById(taskId).getOrNull()
            if (task == null) {
                Timber.w("Session creation skipped: task $taskId not found. Timer is running without a session record.")
                sessionIdDeferred.complete(null)
                return@launch
            }
            val settings = settingsRepository.getPomodoroSettings().getOrDefault(PomodoroSettings())
            startTimerSessionUseCase(task, settings, cycles, LocalDateTime.now())
                .onSuccess { sessionId ->
                    updateTimerState(_timerState.value.copy(sessionId = sessionId))
                    sessionIdDeferred.complete(sessionId)
                }
                .onFailure { error ->
                    Timber.w("Session creation failed: $error. Timer is running without a session record.")
                    sessionIdDeferred.complete(null)
                }
        }
    }

    private fun saveCompletedSession(totalWorkMinutes: Int) {
        val state = _timerState.value
        saveSession(
            state = state,
            totalWorkMinutes = totalWorkMinutes,
            isInterrupted = false,
            isSessionCompleted = true
        )
    }

    private fun saveInterruptedSession() {
        val state = _timerState.value
        saveSession(
            state = state,
            totalWorkMinutes = state.totalWorkMinutes,
            isInterrupted = true,
            isSessionCompleted = false
        )
    }

    private fun saveSession(
        state: TimerState,
        totalWorkMinutes: Int,
        isInterrupted: Boolean,
        isSessionCompleted: Boolean
    ) {
        serviceScope.launch {
            withContext(NonCancellable) {
                val sessionId = sessionIdDeferred.await() ?: return@withContext
                val task = taskRepository.getTaskById(state.taskId).getOrNull() ?: return@withContext
                val isPremium = premiumManager.isPremium()
                val settings = settingsRepository.getPomodoroSettings().getOrDefault(PomodoroSettings())
                val params = FinishTimerSessionUseCase.Params(
                    sessionId = sessionId,
                    task = task,
                    settings = settings,
                    totalWorkMinutes = totalWorkMinutes,
                    currentCycle = state.currentCycle,
                    totalCycles = state.totalCycles,
                    isInterrupted = isInterrupted,
                    isSessionCompleted = isSessionCompleted,
                    isPremium = isPremium,
                    reviewIntervals = premiumManager.getReviewIntervals(isPremium, task.reviewCount)
                )
                finishTimerSessionUseCase(params)
                    .onSuccess { IterioWidgetReceiver.sendDataChangedBroadcast(this@TimerService) }
                    .onFailure { Timber.e("Failed to save session: $it") }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()

        try {
            countDownTimer?.cancel()
        } finally {
            countDownTimer = null
        }

        // Release SoundPool resources
        try {
            soundPool?.release()
        } finally {
            soundPool = null
            soundPoolLoaded = false
        }

        _activeTimerState.value = null
        _sessionCompletedTaskId.value = null

        super.onDestroy()
    }
}
