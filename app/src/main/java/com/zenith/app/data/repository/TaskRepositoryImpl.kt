package com.zenith.app.data.repository

import com.zenith.app.data.local.dao.TaskDao
import com.zenith.app.data.local.entity.TaskEntity
import com.zenith.app.domain.model.ScheduleType
import com.zenith.app.domain.model.Task
import com.zenith.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getTasksByGroup(groupId: Long): Flow<List<Task>> {
        return taskDao.getTasksByGroup(groupId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAllActiveTasks(): Flow<List<Task>> {
        return taskDao.getAllActiveTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)?.toDomainModel()
    }

    override suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity().copy(updatedAt = LocalDateTime.now()))
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }

    override suspend fun deactivateTask(id: Long) {
        taskDao.deactivateTask(id)
    }

    override suspend fun updateProgress(id: Long, note: String?, percent: Int?, goal: String?) {
        taskDao.updateProgress(id, note, percent, goal, LocalDateTime.now())
    }

    override fun getTodayScheduledTasks(today: LocalDate): Flow<List<Task>> {
        val dayOfWeek = today.dayOfWeek.value.toString()  // 1=月曜, 7=日曜
        val todayStr = today.toString()  // ISO format (yyyy-MM-dd)

        return taskDao.getTodayScheduledTasks(todayStr, dayOfWeek).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun updateLastStudiedAt(taskId: Long, studiedAt: LocalDateTime) {
        taskDao.updateLastStudiedAt(taskId, studiedAt)
    }

    private fun TaskEntity.toDomainModel(): Task {
        return Task(
            id = id,
            groupId = groupId,
            name = name,
            progressNote = progressNote,
            progressPercent = progressPercent,
            nextGoal = nextGoal,
            workDurationMinutes = workDurationMinutes,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            scheduleType = ScheduleType.fromString(scheduleType),
            repeatDays = Task.parseRepeatDays(repeatDays),
            deadlineDate = deadlineDate,
            specificDate = specificDate,
            lastStudiedAt = lastStudiedAt
        )
    }

    private fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            groupId = groupId,
            name = name,
            progressNote = progressNote,
            progressPercent = progressPercent,
            nextGoal = nextGoal,
            workDurationMinutes = workDurationMinutes,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            scheduleType = if (scheduleType == ScheduleType.NONE) null else scheduleType.name.lowercase(),
            repeatDays = Task.formatRepeatDays(repeatDays),
            deadlineDate = deadlineDate,
            specificDate = specificDate,
            lastStudiedAt = lastStudiedAt
        )
    }
}
