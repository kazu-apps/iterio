package com.zenith.app.domain.repository

import com.zenith.app.domain.model.PomodoroSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getPomodoroSettingsFlow(): Flow<PomodoroSettings>
    suspend fun getPomodoroSettings(): PomodoroSettings
    suspend fun updatePomodoroSettings(settings: PomodoroSettings)
    suspend fun getSetting(key: String, defaultValue: String): String
    suspend fun setSetting(key: String, value: String)
}
