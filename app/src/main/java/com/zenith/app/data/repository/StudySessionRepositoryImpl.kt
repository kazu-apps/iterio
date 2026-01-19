package com.zenith.app.data.repository

import com.zenith.app.data.local.dao.StudySessionDao
import com.zenith.app.data.local.entity.StudySessionEntity
import com.zenith.app.domain.model.StudySession
import com.zenith.app.domain.repository.StudySessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudySessionRepositoryImpl @Inject constructor(
    private val studySessionDao: StudySessionDao
) : StudySessionRepository {

    override fun getAllSessions(): Flow<List<StudySession>> {
        return studySessionDao.getAllSessions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getSessionsByTask(taskId: Long): Flow<List<StudySession>> {
        return studySessionDao.getSessionsByTask(taskId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getSessionsForDay(date: LocalDate): Flow<List<StudySession>> {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)
        return studySessionDao.getSessionsForDay(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getSessionById(id: Long): StudySession? {
        return studySessionDao.getSessionById(id)?.toDomainModel()
    }

    override suspend fun getTotalMinutesForDay(date: LocalDate): Int {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)
        return studySessionDao.getTotalMinutesForDay(startOfDay, endOfDay)
    }

    override suspend fun getTotalCyclesForDay(date: LocalDate): Int {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)
        return studySessionDao.getTotalCyclesForDay(startOfDay, endOfDay)
    }

    override suspend fun insertSession(session: StudySession): Long {
        return studySessionDao.insertSession(session.toEntity())
    }

    override suspend fun updateSession(session: StudySession) {
        studySessionDao.updateSession(session.toEntity())
    }

    override suspend fun deleteSession(session: StudySession) {
        studySessionDao.deleteSession(session.toEntity())
    }

    override suspend fun finishSession(id: Long, durationMinutes: Int, cycles: Int, interrupted: Boolean) {
        studySessionDao.finishSession(
            id = id,
            endedAt = LocalDateTime.now(),
            durationMinutes = durationMinutes,
            cycles = cycles,
            interrupted = interrupted
        )
    }

    private fun StudySessionEntity.toDomainModel(): StudySession {
        return StudySession(
            id = id,
            taskId = taskId,
            startedAt = startedAt,
            endedAt = endedAt,
            workDurationMinutes = workDurationMinutes,
            plannedDurationMinutes = plannedDurationMinutes,
            cyclesCompleted = cyclesCompleted,
            wasInterrupted = wasInterrupted,
            notes = notes
        )
    }

    private fun StudySession.toEntity(): StudySessionEntity {
        return StudySessionEntity(
            id = id,
            taskId = taskId,
            startedAt = startedAt,
            endedAt = endedAt,
            workDurationMinutes = workDurationMinutes,
            plannedDurationMinutes = plannedDurationMinutes,
            cyclesCompleted = cyclesCompleted,
            wasInterrupted = wasInterrupted,
            notes = notes
        )
    }
}
