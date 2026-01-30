package com.iterio.app.data.repository

import com.iterio.app.data.local.dao.ReviewTaskDao
import com.iterio.app.data.mapper.ReviewTaskMapper
import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.ReviewTask
import com.iterio.app.domain.repository.ReviewTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewTaskRepositoryImpl @Inject constructor(
    private val reviewTaskDao: ReviewTaskDao,
    private val mapper: ReviewTaskMapper
) : ReviewTaskRepository {

    override suspend fun insert(task: ReviewTask): Result<Long, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.insert(mapper.toEntity(task))
        }

    override suspend fun insertAll(tasks: List<ReviewTask>): Result<Unit, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.insertAll(mapper.toEntityList(tasks))
        }

    override suspend fun update(task: ReviewTask): Result<Unit, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.update(mapper.toEntity(task))
        }

    override suspend fun delete(task: ReviewTask): Result<Unit, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.delete(mapper.toEntity(task))
        }

    override suspend fun getById(id: Long): Result<ReviewTask?, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.getById(id)?.let { mapper.toDomain(it) }
        }

    override fun getTasksForSession(studySessionId: Long): Flow<List<ReviewTask>> {
        return reviewTaskDao.getTasksForSession(studySessionId).map { entities ->
            mapper.toDomainList(entities)
        }
    }

    override fun getTasksForTask(taskId: Long): Flow<List<ReviewTask>> {
        return reviewTaskDao.getTasksForTask(taskId).map { entities ->
            mapper.toDomainList(entities)
        }
    }

    override fun getPendingTasksForDate(date: LocalDate): Flow<List<ReviewTask>> {
        return reviewTaskDao.getPendingTasksForDateWithDetails(date).map { entities ->
            mapper.toDomainListFromDetails(entities)
        }
    }

    override fun getAllTasksForDate(date: LocalDate): Flow<List<ReviewTask>> {
        return reviewTaskDao.getAllTasksForDateWithDetails(date).map { entities ->
            mapper.toDomainListFromDetails(entities)
        }
    }

    override fun getOverdueAndTodayTasks(date: LocalDate): Flow<List<ReviewTask>> {
        return reviewTaskDao.getOverdueAndTodayTasksWithDetails(date).map { entities ->
            mapper.toDomainListFromDetails(entities)
        }
    }

    override suspend fun getPendingTaskCountForDate(date: LocalDate): Result<Int, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.getPendingTaskCountForDate(date)
        }

    override suspend fun markAsCompleted(taskId: Long): Result<Unit, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.markAsCompleted(taskId, LocalDateTime.now())
        }

    override suspend fun markAsIncomplete(taskId: Long): Result<Unit, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.markAsIncomplete(taskId)
        }

    override suspend fun reschedule(taskId: Long, newDate: LocalDate): Result<Unit, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.reschedule(taskId, newDate)
        }

    override suspend fun deleteTasksForSession(studySessionId: Long): Result<Unit, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.deleteTasksForSession(studySessionId)
        }

    override suspend fun deleteTasksForTask(taskId: Long): Result<Unit, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.deleteTasksForTask(taskId)
        }

    override suspend fun getTaskCountByDateRange(startDate: LocalDate, endDate: LocalDate): Result<Map<LocalDate, Int>, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.getTaskCountByDateRange(startDate, endDate)
                .associate { it.date to it.count }
        }

    override fun observeTaskCountByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, Int>> {
        return reviewTaskDao.observeTaskCountByDateRange(startDate, endDate).map { counts ->
            counts.associate { it.date to it.count }
        }
    }

    override fun observeGroupColorsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, List<String>>> {
        return reviewTaskDao.observeGroupColorsByDateRange(startDate, endDate).map { items ->
            val result = mutableMapOf<LocalDate, MutableList<String>>()
            for (item in items) {
                val color = item.colorHex ?: DEFAULT_GROUP_COLOR
                result.getOrPut(item.date) { mutableListOf() }.add(color)
            }
            result.mapValues { it.value.toList() }
        }
    }

    override fun getAllWithDetails(): Flow<List<ReviewTask>> {
        return reviewTaskDao.getAllWithDetails().map { entities ->
            mapper.toDomainListFromDetails(entities)
        }
    }

    override suspend fun getTotalCount(): Result<Int, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.getTotalCount()
        }

    override suspend fun getIncompleteCount(): Result<Int, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.getIncompleteCount()
        }

    override suspend fun deleteByIds(ids: List<Long>): Result<Unit, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.deleteByIds(ids)
        }

    override suspend fun deleteAll(): Result<Unit, DomainError> =
        Result.catchingSuspend {
            reviewTaskDao.deleteAll()
        }

    companion object {
        private const val DEFAULT_GROUP_COLOR = "#00838F"
    }
}
