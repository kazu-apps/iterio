package com.iterio.app.data.repository

import com.iterio.app.data.local.dao.StudySessionDao
import com.iterio.app.data.mapper.StudySessionMapper
import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.StudySession
import com.iterio.app.domain.repository.StudySessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudySessionRepositoryImpl @Inject constructor(
    private val studySessionDao: StudySessionDao,
    private val mapper: StudySessionMapper
) : StudySessionRepository {

    override fun getAllSessions(): Flow<List<StudySession>> {
        return studySessionDao.getAllSessions().map { entities ->
            mapper.toDomainList(entities)
        }
    }

    override fun getSessionsByTask(taskId: Long): Flow<List<StudySession>> {
        return studySessionDao.getSessionsByTask(taskId).map { entities ->
            mapper.toDomainList(entities)
        }
    }

    override fun getSessionsForDay(date: LocalDate): Flow<List<StudySession>> {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)
        return studySessionDao.getSessionsForDay(startOfDay, endOfDay).map { entities ->
            mapper.toDomainList(entities)
        }
    }

    override suspend fun getSessionById(id: Long): Result<StudySession?, DomainError> =
        Result.catchingSuspend {
            studySessionDao.getSessionById(id)?.let { mapper.toDomain(it) }
        }

    override suspend fun getTotalMinutesForDay(date: LocalDate): Result<Int, DomainError> =
        Result.catchingSuspend {
            val startOfDay = date.atStartOfDay()
            val endOfDay = date.atTime(LocalTime.MAX)
            studySessionDao.getTotalMinutesForDay(startOfDay, endOfDay)
        }

    override suspend fun getTotalCyclesForDay(date: LocalDate): Result<Int, DomainError> =
        Result.catchingSuspend {
            val startOfDay = date.atStartOfDay()
            val endOfDay = date.atTime(LocalTime.MAX)
            studySessionDao.getTotalCyclesForDay(startOfDay, endOfDay)
        }

    override fun observeTotalMinutesForDay(date: LocalDate): Flow<Int> {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)
        return studySessionDao.observeTotalMinutesForDay(startOfDay, endOfDay)
    }

    override fun observeTotalCyclesForDay(date: LocalDate): Flow<Int> {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)
        return studySessionDao.observeTotalCyclesForDay(startOfDay, endOfDay)
    }

    override suspend fun insertSession(session: StudySession): Result<Long, DomainError> =
        Result.catchingSuspend {
            studySessionDao.insertSession(mapper.toEntity(session))
        }

    override suspend fun updateSession(session: StudySession): Result<Unit, DomainError> =
        Result.catchingSuspend {
            studySessionDao.updateSession(mapper.toEntity(session))
        }

    override suspend fun deleteSession(session: StudySession): Result<Unit, DomainError> =
        Result.catchingSuspend {
            studySessionDao.deleteSession(mapper.toEntity(session))
        }

    override suspend fun finishSession(id: Long, durationMinutes: Int, cycles: Int, interrupted: Boolean): Result<Unit, DomainError> =
        Result.catchingSuspend {
            studySessionDao.finishSession(
                id = id,
                endedAt = LocalDateTime.now(),
                durationMinutes = durationMinutes,
                cycles = cycles,
                interrupted = interrupted
            )
        }

    override suspend fun getSessionCount(): Result<Int, DomainError> =
        Result.catchingSuspend {
            studySessionDao.getSessionCount()
        }
}
