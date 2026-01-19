package com.zenith.app.data.repository

import com.zenith.app.data.local.dao.ReviewTaskDao
import com.zenith.app.data.local.entity.ReviewTaskEntity
import com.zenith.app.domain.model.ReviewTask
import com.zenith.app.domain.repository.ReviewTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewTaskRepositoryImpl @Inject constructor(
    private val reviewTaskDao: ReviewTaskDao
) : ReviewTaskRepository {

    override suspend fun insert(task: ReviewTask): Long {
        return reviewTaskDao.insert(task.toEntity())
    }

    override suspend fun insertAll(tasks: List<ReviewTask>) {
        reviewTaskDao.insertAll(tasks.map { it.toEntity() })
    }

    override suspend fun update(task: ReviewTask) {
        reviewTaskDao.update(task.toEntity())
    }

    override suspend fun delete(task: ReviewTask) {
        reviewTaskDao.delete(task.toEntity())
    }

    override suspend fun getById(id: Long): ReviewTask? {
        return reviewTaskDao.getById(id)?.toDomain()
    }

    override fun getTasksForSession(studySessionId: Long): Flow<List<ReviewTask>> {
        return reviewTaskDao.getTasksForSession(studySessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksForTask(taskId: Long): Flow<List<ReviewTask>> {
        return reviewTaskDao.getTasksForTask(taskId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPendingTasksForDate(date: LocalDate): Flow<List<ReviewTask>> {
        return reviewTaskDao.getPendingTasksForDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllTasksForDate(date: LocalDate): Flow<List<ReviewTask>> {
        return reviewTaskDao.getAllTasksForDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getOverdueAndTodayTasks(date: LocalDate): Flow<List<ReviewTask>> {
        return reviewTaskDao.getOverdueAndTodayTasks(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPendingTaskCountForDate(date: LocalDate): Int {
        return reviewTaskDao.getPendingTaskCountForDate(date)
    }

    override suspend fun markAsCompleted(taskId: Long) {
        reviewTaskDao.markAsCompleted(taskId, LocalDateTime.now())
    }

    override suspend fun markAsIncomplete(taskId: Long) {
        reviewTaskDao.markAsIncomplete(taskId)
    }

    override suspend fun reschedule(taskId: Long, newDate: LocalDate) {
        reviewTaskDao.reschedule(taskId, newDate)
    }

    override suspend fun deleteTasksForSession(studySessionId: Long) {
        reviewTaskDao.deleteTasksForSession(studySessionId)
    }

    override suspend fun deleteTasksForTask(taskId: Long) {
        reviewTaskDao.deleteTasksForTask(taskId)
    }

    private fun ReviewTask.toEntity(): ReviewTaskEntity {
        return ReviewTaskEntity(
            id = id,
            studySessionId = studySessionId,
            taskId = taskId,
            scheduledDate = scheduledDate,
            reviewNumber = reviewNumber,
            isCompleted = isCompleted,
            completedAt = completedAt,
            createdAt = createdAt
        )
    }

    private fun ReviewTaskEntity.toDomain(): ReviewTask {
        return ReviewTask(
            id = id,
            studySessionId = studySessionId,
            taskId = taskId,
            scheduledDate = scheduledDate,
            reviewNumber = reviewNumber,
            isCompleted = isCompleted,
            completedAt = completedAt,
            createdAt = createdAt
        )
    }
}
