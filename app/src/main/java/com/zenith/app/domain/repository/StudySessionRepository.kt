package com.zenith.app.domain.repository

import com.zenith.app.domain.model.StudySession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface StudySessionRepository {
    fun getAllSessions(): Flow<List<StudySession>>
    fun getSessionsByTask(taskId: Long): Flow<List<StudySession>>
    fun getSessionsForDay(date: LocalDate): Flow<List<StudySession>>
    suspend fun getSessionById(id: Long): StudySession?
    suspend fun getTotalMinutesForDay(date: LocalDate): Int
    suspend fun getTotalCyclesForDay(date: LocalDate): Int
    suspend fun insertSession(session: StudySession): Long
    suspend fun updateSession(session: StudySession)
    suspend fun deleteSession(session: StudySession)
    suspend fun finishSession(id: Long, durationMinutes: Int, cycles: Int, interrupted: Boolean)
}
