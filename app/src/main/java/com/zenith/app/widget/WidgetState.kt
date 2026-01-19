package com.zenith.app.widget

import com.zenith.app.service.TimerPhase

data class WidgetState(
    val todayStudyMinutes: Int = 0,
    val currentStreak: Int = 0,
    val timerPhase: TimerPhase = TimerPhase.IDLE,
    val timeRemainingSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val isPremium: Boolean = false
)
