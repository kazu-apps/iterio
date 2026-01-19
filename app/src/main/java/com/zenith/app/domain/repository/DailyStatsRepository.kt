package com.zenith.app.domain.repository

import com.zenith.app.domain.model.DailyStats
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class DayStats(
    val dayOfWeek: String,  // "月", "火", ...
    val date: LocalDate,
    val minutes: Int
)

interface DailyStatsRepository {
    suspend fun updateStats(date: LocalDate, studyMinutes: Int, subjectName: String)
    suspend fun getByDate(date: LocalDate): DailyStats?
    fun getByDateFlow(date: LocalDate): Flow<DailyStats?>
    fun getStatsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyStats>>
    suspend fun getTotalMinutesBetweenDates(startDate: LocalDate, endDate: LocalDate): Int
    suspend fun getCurrentStreak(): Int
    suspend fun getMaxStreak(): Int

    /**
     * 週間データ（月曜〜日曜）を取得
     */
    suspend fun getWeeklyData(weekStart: LocalDate): List<DayStats>
}
