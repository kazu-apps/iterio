package com.zenith.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zenith.app.data.local.dao.DailyStatsDao
import com.zenith.app.data.local.entity.DailyStatsEntity
import com.zenith.app.domain.model.DailyStats
import com.zenith.app.domain.repository.DailyStatsRepository
import com.zenith.app.domain.repository.DayStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

class DailyStatsRepositoryImpl @Inject constructor(
    private val dailyStatsDao: DailyStatsDao,
    private val gson: Gson
) : DailyStatsRepository {

    override suspend fun updateStats(date: LocalDate, studyMinutes: Int, subjectName: String) {
        val existingStats = dailyStatsDao.getByDate(date)

        if (existingStats != null) {
            val breakdown = parseBreakdown(existingStats.subjectBreakdownJson).toMutableMap()
            breakdown[subjectName] = (breakdown[subjectName] ?: 0) + studyMinutes

            dailyStatsDao.update(
                existingStats.copy(
                    totalStudyMinutes = existingStats.totalStudyMinutes + studyMinutes,
                    sessionCount = existingStats.sessionCount + 1,
                    subjectBreakdownJson = gson.toJson(breakdown)
                )
            )
        } else {
            val breakdown = mapOf(subjectName to studyMinutes)
            dailyStatsDao.insert(
                DailyStatsEntity(
                    date = date,
                    totalStudyMinutes = studyMinutes,
                    sessionCount = 1,
                    subjectBreakdownJson = gson.toJson(breakdown)
                )
            )
        }
    }

    override suspend fun getByDate(date: LocalDate): DailyStats? {
        return dailyStatsDao.getByDate(date)?.toDomain()
    }

    override fun getByDateFlow(date: LocalDate): Flow<DailyStats?> {
        return dailyStatsDao.getByDateFlow(date).map { it?.toDomain() }
    }

    override fun getStatsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyStats>> {
        return dailyStatsDao.getStatsBetweenDates(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTotalMinutesBetweenDates(startDate: LocalDate, endDate: LocalDate): Int {
        return dailyStatsDao.getTotalMinutesBetweenDates(startDate, endDate) ?: 0
    }

    override suspend fun getCurrentStreak(): Int {
        return dailyStatsDao.getCurrentStreak(LocalDate.now())
    }

    override suspend fun getMaxStreak(): Int {
        return dailyStatsDao.getMaxStreak() ?: 0
    }

    override suspend fun getWeeklyData(weekStart: LocalDate): List<DayStats> {
        val dayLabels = listOf("月", "火", "水", "木", "金", "土", "日")
        val weekEnd = weekStart.plusDays(6)

        return (0..6).map { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            val stats = dailyStatsDao.getByDate(date)
            val minutes = stats?.totalStudyMinutes ?: 0

            DayStats(
                dayOfWeek = dayLabels[dayOffset],
                date = date,
                minutes = minutes
            )
        }
    }

    private fun parseBreakdown(json: String): Map<String, Int> {
        return try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun DailyStatsEntity.toDomain(): DailyStats {
        return DailyStats(
            date = date,
            totalStudyMinutes = totalStudyMinutes,
            sessionCount = sessionCount,
            subjectBreakdown = parseBreakdown(subjectBreakdownJson)
        )
    }
}
