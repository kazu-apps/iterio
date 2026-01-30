package com.iterio.app.data.local.dao

import androidx.room.*
import com.iterio.app.data.local.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE taskId = :taskId ORDER BY startedAt DESC")
    fun getSessionsByTask(taskId: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): StudySessionEntity?

    @Query("""
        SELECT * FROM study_sessions
        WHERE startedAt >= :startOfDay AND startedAt < :endOfDay
        ORDER BY startedAt DESC
    """)
    fun getSessionsForDay(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Flow<List<StudySessionEntity>>

    @Query("""
        SELECT COALESCE(SUM(workDurationMinutes), 0) FROM study_sessions
        WHERE startedAt >= :startOfDay AND startedAt < :endOfDay
    """)
    suspend fun getTotalMinutesForDay(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Int

    @Query("""
        SELECT COALESCE(SUM(cyclesCompleted), 0) FROM study_sessions
        WHERE startedAt >= :startOfDay AND startedAt < :endOfDay
    """)
    suspend fun getTotalCyclesForDay(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Int

    @Query("""
        SELECT COALESCE(SUM(workDurationMinutes), 0) FROM study_sessions
        WHERE startedAt >= :startOfDay AND startedAt < :endOfDay
    """)
    fun observeTotalMinutesForDay(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(cyclesCompleted), 0) FROM study_sessions
        WHERE startedAt >= :startOfDay AND startedAt < :endOfDay
    """)
    fun observeTotalCyclesForDay(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Delete
    suspend fun deleteSession(session: StudySessionEntity)

    @Query("""
        UPDATE study_sessions
        SET endedAt = :endedAt, workDurationMinutes = :durationMinutes, cyclesCompleted = :cycles, wasInterrupted = :interrupted
        WHERE id = :id
    """)
    suspend fun finishSession(
        id: Long,
        endedAt: LocalDateTime,
        durationMinutes: Int,
        cycles: Int,
        interrupted: Boolean
    )

    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC")
    suspend fun getAll(): List<StudySessionEntity>

    @Query("SELECT COUNT(*) FROM study_sessions")
    suspend fun getSessionCount(): Int
}
