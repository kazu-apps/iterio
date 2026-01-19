package com.zenith.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zenith.app.data.local.entity.DailyStatsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: DailyStatsEntity)

    @Update
    suspend fun update(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getByDate(date: LocalDate): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    fun getByDateFlow(date: LocalDate): Flow<DailyStatsEntity?>

    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getStatsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyStatsEntity>>

    @Query("SELECT SUM(totalStudyMinutes) FROM daily_stats WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalMinutesBetweenDates(startDate: LocalDate, endDate: LocalDate): Int?

    @Query("SELECT SUM(sessionCount) FROM daily_stats WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalSessionsBetweenDates(startDate: LocalDate, endDate: LocalDate): Int?

    @Query("""
        SELECT COUNT(*) FROM (
            SELECT date FROM daily_stats
            WHERE totalStudyMinutes > 0 AND date <= :today
            ORDER BY date DESC
        ) AS recent_dates
        WHERE date >= (
            SELECT COALESCE(
                (SELECT date FROM daily_stats
                 WHERE totalStudyMinutes = 0 AND date <= :today
                 ORDER BY date DESC LIMIT 1),
                '1970-01-01'
            )
        )
    """)
    suspend fun getCurrentStreak(today: LocalDate): Int

    @Query("""
        SELECT MAX(streak_length) FROM (
            SELECT COUNT(*) as streak_length
            FROM daily_stats ds1
            WHERE totalStudyMinutes > 0
            AND NOT EXISTS (
                SELECT 1 FROM daily_stats ds2
                WHERE ds2.date = date(ds1.date, '-1 day')
                AND ds2.totalStudyMinutes = 0
            )
            GROUP BY (
                SELECT COUNT(*) FROM daily_stats ds3
                WHERE ds3.date < ds1.date AND ds3.totalStudyMinutes = 0
            )
        )
    """)
    suspend fun getMaxStreak(): Int?

    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    suspend fun getAll(): List<DailyStatsEntity>
}
