package com.zenith.app.data.repository

import com.zenith.app.data.local.dao.SettingsDao
import com.zenith.app.data.local.entity.SettingsEntity
import com.zenith.app.domain.model.PomodoroSettings
import com.zenith.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getPomodoroSettingsFlow(): Flow<PomodoroSettings> {
        return settingsDao.getAllSettings().map { settings ->
            buildPomodoroSettings(settings)
        }
    }

    override suspend fun getPomodoroSettings(): PomodoroSettings {
        return PomodoroSettings(
            workDurationMinutes = getSetting(
                SettingsEntity.KEY_WORK_DURATION_MINUTES,
                SettingsEntity.DEFAULT_WORK_DURATION
            ).toIntOrNull() ?: 25,
            shortBreakMinutes = getSetting(
                SettingsEntity.KEY_SHORT_BREAK_MINUTES,
                SettingsEntity.DEFAULT_SHORT_BREAK
            ).toIntOrNull() ?: 5,
            longBreakMinutes = getSetting(
                SettingsEntity.KEY_LONG_BREAK_MINUTES,
                SettingsEntity.DEFAULT_LONG_BREAK
            ).toIntOrNull() ?: 15,
            cyclesBeforeLongBreak = getSetting(
                SettingsEntity.KEY_CYCLES_BEFORE_LONG_BREAK,
                SettingsEntity.DEFAULT_CYCLES
            ).toIntOrNull() ?: 4,
            focusModeEnabled = getSetting(
                SettingsEntity.KEY_FOCUS_MODE_ENABLED,
                "true"
            ).toBoolean(),
            focusModeStrict = getSetting(
                SettingsEntity.KEY_FOCUS_MODE_STRICT,
                "false"
            ).toBoolean(),
            reviewEnabled = getSetting(
                SettingsEntity.KEY_REVIEW_ENABLED,
                "true"
            ).toBoolean(),
            reviewIntervals = parseReviewIntervals(
                getSetting(
                    SettingsEntity.KEY_REVIEW_INTERVALS,
                    SettingsEntity.DEFAULT_REVIEW_INTERVALS
                )
            ),
            notificationsEnabled = getSetting(
                SettingsEntity.KEY_NOTIFICATIONS_ENABLED,
                "true"
            ).toBoolean()
        )
    }

    override suspend fun updatePomodoroSettings(settings: PomodoroSettings) {
        setSetting(SettingsEntity.KEY_WORK_DURATION_MINUTES, settings.workDurationMinutes.toString())
        setSetting(SettingsEntity.KEY_SHORT_BREAK_MINUTES, settings.shortBreakMinutes.toString())
        setSetting(SettingsEntity.KEY_LONG_BREAK_MINUTES, settings.longBreakMinutes.toString())
        setSetting(SettingsEntity.KEY_CYCLES_BEFORE_LONG_BREAK, settings.cyclesBeforeLongBreak.toString())
        setSetting(SettingsEntity.KEY_FOCUS_MODE_ENABLED, settings.focusModeEnabled.toString())
        setSetting(SettingsEntity.KEY_FOCUS_MODE_STRICT, settings.focusModeStrict.toString())
        setSetting(SettingsEntity.KEY_REVIEW_ENABLED, settings.reviewEnabled.toString())
        setSetting(SettingsEntity.KEY_REVIEW_INTERVALS, json.encodeToString(settings.reviewIntervals))
        setSetting(SettingsEntity.KEY_NOTIFICATIONS_ENABLED, settings.notificationsEnabled.toString())
    }

    override suspend fun getSetting(key: String, defaultValue: String): String {
        return settingsDao.getSettingValue(key) ?: defaultValue.also {
            settingsDao.setSetting(key, it)
        }
    }

    override suspend fun setSetting(key: String, value: String) {
        settingsDao.setSetting(key, value)
    }

    private fun buildPomodoroSettings(settings: List<SettingsEntity>): PomodoroSettings {
        val settingsMap = settings.associateBy { it.key }
        return PomodoroSettings(
            workDurationMinutes = settingsMap[SettingsEntity.KEY_WORK_DURATION_MINUTES]?.value?.toIntOrNull() ?: 25,
            shortBreakMinutes = settingsMap[SettingsEntity.KEY_SHORT_BREAK_MINUTES]?.value?.toIntOrNull() ?: 5,
            longBreakMinutes = settingsMap[SettingsEntity.KEY_LONG_BREAK_MINUTES]?.value?.toIntOrNull() ?: 15,
            cyclesBeforeLongBreak = settingsMap[SettingsEntity.KEY_CYCLES_BEFORE_LONG_BREAK]?.value?.toIntOrNull() ?: 4,
            focusModeEnabled = settingsMap[SettingsEntity.KEY_FOCUS_MODE_ENABLED]?.value?.toBoolean() ?: true,
            focusModeStrict = settingsMap[SettingsEntity.KEY_FOCUS_MODE_STRICT]?.value?.toBoolean() ?: false,
            reviewEnabled = settingsMap[SettingsEntity.KEY_REVIEW_ENABLED]?.value?.toBoolean() ?: true,
            reviewIntervals = parseReviewIntervals(
                settingsMap[SettingsEntity.KEY_REVIEW_INTERVALS]?.value ?: SettingsEntity.DEFAULT_REVIEW_INTERVALS
            ),
            notificationsEnabled = settingsMap[SettingsEntity.KEY_NOTIFICATIONS_ENABLED]?.value?.toBoolean() ?: true
        )
    }

    private fun parseReviewIntervals(jsonString: String): List<Int> {
        return try {
            json.decodeFromString<List<Int>>(jsonString)
        } catch (e: Exception) {
            listOf(1, 3, 7, 14, 30, 60)
        }
    }
}
