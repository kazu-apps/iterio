package com.zenith.app.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import androidx.room.Room
import com.zenith.app.data.local.ZenithDatabase
import com.zenith.app.service.TimerPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val Context.premiumDataStore by preferencesDataStore(name = "premium_prefs")

object ZenithWidgetStateHelper {

    @Volatile
    private var database: ZenithDatabase? = null

    private fun getDatabase(context: Context): ZenithDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                ZenithDatabase::class.java,
                ZenithDatabase.DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { database = it }
        }
    }

    suspend fun getWidgetState(context: Context): WidgetState {
        return try {
            // Premium状態をチェック
            val isPremium = checkPremiumStatus(context)

            val db = getDatabase(context)
            val dailyStatsDao = db.dailyStatsDao()

            val today = LocalDate.now()
            val todayStats = dailyStatsDao.getByDate(today)
            val streak = dailyStatsDao.getCurrentStreak(today)

            // TimerServiceから状態を取得（SharedPreferences経由）
            val timerState = getTimerStateFromPrefs(context)

            WidgetState(
                todayStudyMinutes = todayStats?.totalStudyMinutes ?: 0,
                currentStreak = streak,
                timerPhase = timerState.phase,
                timeRemainingSeconds = timerState.timeRemainingSeconds,
                isTimerRunning = timerState.isRunning,
                isPremium = isPremium
            )
        } catch (e: Exception) {
            WidgetState()
        }
    }

    private suspend fun checkPremiumStatus(context: Context): Boolean {
        return try {
            val prefs = context.premiumDataStore.data.first()
            val subscriptionType = prefs[stringPreferencesKey("subscription_type")]
            val trialExpiresAtStr = prefs[stringPreferencesKey("trial_expires_at")]

            // LIFETIME または有効なサブスクリプション
            if (subscriptionType == "LIFETIME") return true
            if (subscriptionType == "MONTHLY" || subscriptionType == "YEARLY") {
                val expiresAtStr = prefs[stringPreferencesKey("expires_at")]
                if (expiresAtStr != null) {
                    val expiresAt = LocalDateTime.parse(expiresAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    if (expiresAt.isAfter(LocalDateTime.now())) return true
                }
            }

            // トライアル期間中
            if (trialExpiresAtStr != null) {
                val trialExpiresAt = LocalDateTime.parse(trialExpiresAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                if (trialExpiresAt.isAfter(LocalDateTime.now())) return true
            }

            false
        } catch (e: Exception) {
            false
        }
    }

    private fun getTimerStateFromPrefs(context: Context): TimerStateData {
        val prefs = context.getSharedPreferences(TIMER_PREFS_NAME, Context.MODE_PRIVATE)
        val phaseOrdinal = prefs.getInt(KEY_TIMER_PHASE, TimerPhase.IDLE.ordinal)
        val timeRemaining = prefs.getInt(KEY_TIME_REMAINING, 0)
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)

        return TimerStateData(
            phase = TimerPhase.entries.getOrElse(phaseOrdinal) { TimerPhase.IDLE },
            timeRemainingSeconds = timeRemaining,
            isRunning = isRunning
        )
    }

    fun saveTimerStateToPrefs(context: Context, phase: TimerPhase, timeRemaining: Int, isRunning: Boolean) {
        val prefs = context.getSharedPreferences(TIMER_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_TIMER_PHASE, phase.ordinal)
            .putInt(KEY_TIME_REMAINING, timeRemaining)
            .putBoolean(KEY_IS_RUNNING, isRunning)
            .apply()
    }

    fun updateWidget(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ZenithWidget().updateAll(context)
            } catch (e: Exception) {
                // Widget update failed, ignore
            }
        }
    }

    private data class TimerStateData(
        val phase: TimerPhase,
        val timeRemainingSeconds: Int,
        val isRunning: Boolean
    )

    private const val TIMER_PREFS_NAME = "zenith_widget_timer_prefs"
    private const val KEY_TIMER_PHASE = "timer_phase"
    private const val KEY_TIME_REMAINING = "time_remaining"
    private const val KEY_IS_RUNNING = "is_running"

    /**
     * Close the database when no longer needed (e.g., during app shutdown)
     */
    fun closeDatabase() {
        synchronized(this) {
            try {
                database?.close()
            } catch (e: Exception) {
                android.util.Log.e("ZenithWidgetStateHelper", "Error closing database", e)
            } finally {
                database = null
            }
        }
    }
}
