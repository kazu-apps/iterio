package com.zenith.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zenith.app.R
import com.zenith.app.ui.MainActivity
import com.zenith.app.widget.ZenithWidgetStateHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class TimerPhase {
    WORK,
    SHORT_BREAK,
    LONG_BREAK,
    IDLE
}

data class TimerState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val timeRemainingSeconds: Int = 0,
    val totalTimeSeconds: Int = 0,
    val currentCycle: Int = 1,
    val totalCycles: Int = 4,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val taskId: Long = 0,
    val taskName: String = "",
    val totalWorkMinutes: Int = 0,
    val workDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionCompleted: Boolean = false
)

@AndroidEntryPoint
class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.zenith.app.action.START"
        const val ACTION_PAUSE = "com.zenith.app.action.PAUSE"
        const val ACTION_RESUME = "com.zenith.app.action.RESUME"
        const val ACTION_STOP = "com.zenith.app.action.STOP"
        const val ACTION_SKIP = "com.zenith.app.action.SKIP"

        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_WORK_DURATION = "work_duration"
        const val EXTRA_SHORT_BREAK = "short_break"
        const val EXTRA_LONG_BREAK = "long_break"
        const val EXTRA_CYCLES = "cycles"
        const val EXTRA_FOCUS_MODE_ENABLED = "focus_mode_enabled"
        const val EXTRA_FOCUS_MODE_STRICT = "focus_mode_strict"

        fun startTimer(
            context: Context,
            taskId: Long,
            taskName: String,
            workDuration: Int,
            shortBreak: Int,
            longBreak: Int,
            cycles: Int,
            focusModeEnabled: Boolean = false,
            focusModeStrict: Boolean = false
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

    private val binder = TimerBinder()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private var notificationManager: NotificationManager? = null

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0)
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: ""
                val workDuration = intent.getIntExtra(EXTRA_WORK_DURATION, 25)
                val shortBreak = intent.getIntExtra(EXTRA_SHORT_BREAK, 5)
                val longBreak = intent.getIntExtra(EXTRA_LONG_BREAK, 15)
                val cycles = intent.getIntExtra(EXTRA_CYCLES, 4)
                val focusModeEnabled = intent.getBooleanExtra(EXTRA_FOCUS_MODE_ENABLED, false)
                val focusModeStrict = intent.getBooleanExtra(EXTRA_FOCUS_MODE_STRICT, false)

                startTimerInternal(taskId, taskName, workDuration, shortBreak, longBreak, cycles, focusModeEnabled, focusModeStrict)
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

    private fun startTimerInternal(
        taskId: Long,
        taskName: String,
        workDuration: Int,
        shortBreak: Int,
        longBreak: Int,
        cycles: Int,
        focusMode: Boolean,
        focusStrict: Boolean
    ) {
        val totalSeconds = workDuration * 60

        // Store focus mode settings
        focusModeEnabled = focusMode
        focusModeStrict = focusStrict

        _timerState.value = TimerState(
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
            sessionCompleted = false
        )

        // Start focus mode if enabled
        if (focusModeEnabled && FocusModeService.isServiceRunning.value) {
            FocusModeService.startFocusMode(focusModeStrict)
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
        _timerState.value = _timerState.value.copy(isRunning = false, isPaused = true)
        updateNotification()
    }

    private fun resumeTimerInternal() {
        _timerState.value = _timerState.value.copy(isRunning = true, isPaused = false)
        startCountDown()
        updateNotification()
    }

    private fun stopTimerInternal() {
        countDownTimer?.cancel()
        _timerState.value = TimerState()

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

        val remainingMillis = _timerState.value.timeRemainingSeconds * 1000L

        countDownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                _timerState.value = _timerState.value.copy(timeRemainingSeconds = seconds)
                updateNotification()
                updateOverlayTime()
            }

            override fun onFinish() {
                onPhaseComplete()
            }
        }.start()
    }

    private fun updateOverlayTime() {
        if (!focusModeEnabled || !focusModeStrict) return

        val state = _timerState.value
        val minutes = state.timeRemainingSeconds / 60
        val seconds = state.timeRemainingSeconds % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        val phaseText = when (state.phase) {
            TimerPhase.WORK -> "作業中"
            TimerPhase.SHORT_BREAK -> "休憩中"
            TimerPhase.LONG_BREAK -> "長休憩中"
            TimerPhase.IDLE -> "完了"
        }

        LockOverlayService.updateOverlayTime(this, timeText, phaseText)
    }

    private fun onPhaseComplete() {
        val state = _timerState.value

        when (state.phase) {
            TimerPhase.WORK -> {
                val newWorkMinutes = state.totalWorkMinutes + state.workDurationMinutes

                if (state.currentCycle >= state.totalCycles) {
                    // All cycles complete -> long break
                    _timerState.value = state.copy(
                        phase = TimerPhase.LONG_BREAK,
                        timeRemainingSeconds = state.longBreakMinutes * 60,
                        totalTimeSeconds = state.longBreakMinutes * 60,
                        totalWorkMinutes = newWorkMinutes
                    )
                } else {
                    // Short break
                    _timerState.value = state.copy(
                        phase = TimerPhase.SHORT_BREAK,
                        timeRemainingSeconds = state.shortBreakMinutes * 60,
                        totalTimeSeconds = state.shortBreakMinutes * 60,
                        totalWorkMinutes = newWorkMinutes
                    )
                }
                updateNotification()
                updateWidgetState()
                startCountDown()
            }

            TimerPhase.SHORT_BREAK -> {
                // Next work cycle
                _timerState.value = state.copy(
                    phase = TimerPhase.WORK,
                    timeRemainingSeconds = state.workDurationMinutes * 60,
                    totalTimeSeconds = state.workDurationMinutes * 60,
                    currentCycle = state.currentCycle + 1
                )
                updateNotification()
                updateWidgetState()
                startCountDown()
            }

            TimerPhase.LONG_BREAK -> {
                // Session complete
                _timerState.value = state.copy(
                    phase = TimerPhase.IDLE,
                    isRunning = false,
                    sessionCompleted = true
                )

                // Stop focus mode and hide overlay
                FocusModeService.stopFocusMode()
                LockOverlayService.hideOverlay(this)

                updateNotification()
                updateWidgetState()
                // Keep service running briefly to allow UI to read final state
            }

            TimerPhase.IDLE -> {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "タイマー",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ポモドーロタイマーの通知"
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

        val minutes = state.timeRemainingSeconds / 60
        val seconds = state.timeRemainingSeconds % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        val phaseText = when (state.phase) {
            TimerPhase.WORK -> "作業中"
            TimerPhase.SHORT_BREAK -> "休憩中"
            TimerPhase.LONG_BREAK -> "長休憩中"
            TimerPhase.IDLE -> if (state.sessionCompleted) "完了！" else "準備中"
        }

        val title = if (state.taskName.isNotEmpty()) {
            "${state.taskName} - $phaseText"
        } else {
            phaseText
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("$timeText (サイクル ${state.currentCycle}/${state.totalCycles})")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (state.phase != TimerPhase.IDLE) {
            builder.addAction(
                if (state.isRunning) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground,
                if (state.isRunning) "一時停止" else "再開",
                pauseResumePendingIntent
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                "停止",
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
        ZenithWidgetStateHelper.saveTimerStateToPrefs(
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
            ZenithWidgetStateHelper.updateWidget(this)
        }
    }

    override fun onDestroy() {
        try {
            countDownTimer?.cancel()
        } finally {
            countDownTimer = null
        }
        super.onDestroy()
    }
}
