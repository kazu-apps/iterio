package com.iterio.app.fakes

import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.StudySession
import com.iterio.app.domain.repository.StudySessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * テスト用の StudySessionRepository 実装
 */
class FakeStudySessionRepository : StudySessionRepository {

    private val sessions = MutableStateFlow<Map<Long, StudySession>>(emptyMap())
    private var nextId = 1L

    override fun getAllSessions(): Flow<List<StudySession>> =
        sessions.map { it.values.toList() }

    override fun getSessionsByTask(taskId: Long): Flow<List<StudySession>> =
        sessions.map { map ->
            map.values.filter { it.taskId == taskId }
        }

    override fun getSessionsForDay(date: LocalDate): Flow<List<StudySession>> =
        sessions.map { map ->
            map.values.filter { it.startedAt.toLocalDate() == date }
        }

    override suspend fun getSessionById(id: Long): Result<StudySession?, DomainError> =
        Result.Success(sessions.value[id])

    override suspend fun getTotalMinutesForDay(date: LocalDate): Result<Int, DomainError> =
        Result.Success(
            sessions.value.values
                .filter { it.startedAt.toLocalDate() == date }
                .sumOf { it.workDurationMinutes }
        )

    override suspend fun getTotalCyclesForDay(date: LocalDate): Result<Int, DomainError> =
        Result.Success(
            sessions.value.values
                .filter { it.startedAt.toLocalDate() == date }
                .sumOf { it.cyclesCompleted }
        )

    override fun observeTotalMinutesForDay(date: LocalDate): Flow<Int> =
        sessions.map { map ->
            map.values
                .filter { it.startedAt.toLocalDate() == date }
                .sumOf { it.workDurationMinutes }
        }

    override fun observeTotalCyclesForDay(date: LocalDate): Flow<Int> =
        sessions.map { map ->
            map.values
                .filter { it.startedAt.toLocalDate() == date }
                .sumOf { it.cyclesCompleted }
        }

    override suspend fun insertSession(session: StudySession): Result<Long, DomainError> {
        val id = nextId++
        val sessionWithId = session.copy(id = id)
        sessions.value = sessions.value + (id to sessionWithId)
        return Result.Success(id)
    }

    override suspend fun updateSession(session: StudySession): Result<Unit, DomainError> {
        sessions.value = sessions.value + (session.id to session)
        return Result.Success(Unit)
    }

    override suspend fun deleteSession(session: StudySession): Result<Unit, DomainError> {
        sessions.value = sessions.value - session.id
        return Result.Success(Unit)
    }

    override suspend fun finishSession(
        id: Long,
        durationMinutes: Int,
        cycles: Int,
        interrupted: Boolean
    ): Result<Unit, DomainError> {
        val session = sessions.value[id] ?: return Result.Success(Unit)
        val finished = session.copy(
            workDurationMinutes = durationMinutes,
            cyclesCompleted = cycles,
            wasInterrupted = interrupted,
            endedAt = LocalDateTime.now()
        )
        sessions.value = sessions.value + (id to finished)
        return Result.Success(Unit)
    }

    override suspend fun getSessionCount(): Result<Int, DomainError> =
        Result.Success(sessions.value.size)

    // Test helpers
    fun clear() {
        sessions.value = emptyMap()
        nextId = 1L
    }
}
