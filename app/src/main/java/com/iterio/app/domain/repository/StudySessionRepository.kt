package com.iterio.app.domain.repository

import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.StudySession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface StudySessionRepository {
    fun getAllSessions(): Flow<List<StudySession>>
    fun getSessionsByTask(taskId: Long): Flow<List<StudySession>>
    fun getSessionsForDay(date: LocalDate): Flow<List<StudySession>>
    suspend fun getSessionById(id: Long): Result<StudySession?, DomainError>
    suspend fun getTotalMinutesForDay(date: LocalDate): Result<Int, DomainError>
    suspend fun getTotalCyclesForDay(date: LocalDate): Result<Int, DomainError>
    fun observeTotalMinutesForDay(date: LocalDate): Flow<Int>
    fun observeTotalCyclesForDay(date: LocalDate): Flow<Int>
    suspend fun insertSession(session: StudySession): Result<Long, DomainError>
    suspend fun updateSession(session: StudySession): Result<Unit, DomainError>
    suspend fun deleteSession(session: StudySession): Result<Unit, DomainError>
    suspend fun finishSession(id: Long, durationMinutes: Int, cycles: Int, interrupted: Boolean): Result<Unit, DomainError>
    suspend fun getSessionCount(): Result<Int, DomainError>
}
