package com.iterio.app.fakes

import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.ReviewTask
import com.iterio.app.domain.repository.ReviewTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * テスト用の ReviewTaskRepository 実装
 */
class FakeReviewTaskRepository : ReviewTaskRepository {

    private val tasks = MutableStateFlow<Map<Long, ReviewTask>>(emptyMap())
    private var nextId = 1L

    override suspend fun insert(task: ReviewTask): Result<Long, DomainError> {
        val id = nextId++
        val taskWithId = task.copy(id = id)
        tasks.value = tasks.value + (id to taskWithId)
        return Result.Success(id)
    }

    override suspend fun insertAll(taskList: List<ReviewTask>): Result<Unit, DomainError> {
        taskList.forEach { insert(it) }
        return Result.Success(Unit)
    }

    override suspend fun update(task: ReviewTask): Result<Unit, DomainError> {
        tasks.value = tasks.value + (task.id to task)
        return Result.Success(Unit)
    }

    override suspend fun delete(task: ReviewTask): Result<Unit, DomainError> {
        tasks.value = tasks.value - task.id
        return Result.Success(Unit)
    }

    override suspend fun getById(id: Long): Result<ReviewTask?, DomainError> =
        Result.Success(tasks.value[id])

    override fun getTasksForSession(studySessionId: Long): Flow<List<ReviewTask>> =
        tasks.map { map ->
            map.values.filter { it.studySessionId == studySessionId }
        }

    override fun getTasksForTask(taskId: Long): Flow<List<ReviewTask>> =
        tasks.map { map ->
            map.values.filter { it.taskId == taskId }
        }

    override fun getPendingTasksForDate(date: LocalDate): Flow<List<ReviewTask>> =
        tasks.map { map ->
            map.values.filter {
                it.scheduledDate == date && !it.isCompleted
            }
        }

    override fun getAllTasksForDate(date: LocalDate): Flow<List<ReviewTask>> =
        tasks.map { map ->
            map.values.filter { it.scheduledDate == date }
        }

    override fun getOverdueAndTodayTasks(date: LocalDate): Flow<List<ReviewTask>> =
        tasks.map { map ->
            map.values.filter {
                !it.isCompleted && it.scheduledDate <= date
            }
        }

    override suspend fun getPendingTaskCountForDate(date: LocalDate): Result<Int, DomainError> =
        Result.Success(
            tasks.value.values.count {
                it.scheduledDate == date && !it.isCompleted
            }
        )

    override suspend fun markAsCompleted(taskId: Long): Result<Unit, DomainError> {
        val task = tasks.value[taskId] ?: return Result.Success(Unit)
        val completed = task.copy(
            isCompleted = true,
            completedAt = LocalDateTime.now()
        )
        tasks.value = tasks.value + (taskId to completed)
        return Result.Success(Unit)
    }

    override suspend fun markAsIncomplete(taskId: Long): Result<Unit, DomainError> {
        val task = tasks.value[taskId] ?: return Result.Success(Unit)
        val incomplete = task.copy(
            isCompleted = false,
            completedAt = null
        )
        tasks.value = tasks.value + (taskId to incomplete)
        return Result.Success(Unit)
    }

    override suspend fun reschedule(taskId: Long, newDate: LocalDate): Result<Unit, DomainError> {
        val task = tasks.value[taskId] ?: return Result.Success(Unit)
        val rescheduled = task.copy(scheduledDate = newDate)
        tasks.value = tasks.value + (taskId to rescheduled)
        return Result.Success(Unit)
    }

    override suspend fun deleteTasksForSession(studySessionId: Long): Result<Unit, DomainError> {
        tasks.value = tasks.value.filterValues { it.studySessionId != studySessionId }
        return Result.Success(Unit)
    }

    override suspend fun deleteTasksForTask(taskId: Long): Result<Unit, DomainError> {
        tasks.value = tasks.value.filterValues { it.taskId != taskId }
        return Result.Success(Unit)
    }

    override suspend fun getTaskCountByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Map<LocalDate, Int>, DomainError> {
        val result = mutableMapOf<LocalDate, Int>()
        tasks.value.values.forEach { task ->
            val date = task.scheduledDate
            if (date >= startDate && date <= endDate) {
                result[date] = (result[date] ?: 0) + 1
            }
        }
        return Result.Success(result)
    }

    override fun observeTaskCountByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Map<LocalDate, Int>> {
        return tasks.map { map ->
            val result = mutableMapOf<LocalDate, Int>()
            map.values.forEach { task ->
                val date = task.scheduledDate
                if (date in startDate..endDate) {
                    result[date] = (result[date] ?: 0) + 1
                }
            }
            result.toMap()
        }
    }

    override fun observeGroupColorsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Map<LocalDate, List<String>>> {
        return tasks.map { map ->
            val result = mutableMapOf<LocalDate, MutableList<String>>()
            map.values.forEach { task ->
                val date = task.scheduledDate
                if (date in startDate..endDate) {
                    val color = task.groupName ?: "#00838F" // Fake: groupNameをcolorとして代用
                    result.getOrPut(date) { mutableListOf() }.add(color)
                }
            }
            result.mapValues { it.value.toList() }
        }
    }

    override fun getAllWithDetails(): Flow<List<ReviewTask>> =
        tasks.map { map -> map.values.toList() }

    override suspend fun getTotalCount(): Result<Int, DomainError> =
        Result.Success(tasks.value.size)

    override suspend fun getIncompleteCount(): Result<Int, DomainError> =
        Result.Success(tasks.value.values.count { !it.isCompleted })

    override suspend fun deleteByIds(ids: List<Long>): Result<Unit, DomainError> {
        tasks.value = tasks.value.filterKeys { it !in ids }
        return Result.Success(Unit)
    }

    override suspend fun deleteAll(): Result<Unit, DomainError> {
        tasks.value = emptyMap()
        return Result.Success(Unit)
    }

    // Test helpers
    fun clear() {
        tasks.value = emptyMap()
        nextId = 1L
    }
}
