package com.zenith.app.domain.model

data class PomodoroSettings(
    val workDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val cyclesBeforeLongBreak: Int = 4,
    val focusModeEnabled: Boolean = true,
    val focusModeStrict: Boolean = false, // true = complete lock, false = emergency unlock allowed
    val reviewEnabled: Boolean = true,
    val reviewIntervals: List<Int> = listOf(1, 3, 7, 14, 30, 60), // days
    val notificationsEnabled: Boolean = true
)
