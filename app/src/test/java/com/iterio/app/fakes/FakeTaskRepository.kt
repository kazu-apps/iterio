package com.iterio.app.fakes

import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.ScheduleType
import com.iterio.app.domain.model.Task
import com.iterio.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * TaskRepositoryのテスト用Fake実装
 *
 * インメモリでデータを保持し、Flowで変更を通知します。
 *
 * 使用例:
 * ```
 * val fakeRepo = FakeTaskRepository()
 * fakeRepo.insertTask(Task(name = "Study Math", groupId = 1))
 *
 * // Flowでの検証
 * fakeRepo.getAllActiveTasks().test {
 *     assertThat(awaitItem()).hasSize(1)
 * }
 * ```
 */
class FakeTaskRepository : TaskRepository {

    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L

    override fun getTasksByGroup(groupId: Long): Flow<List<Task>> {
        return tasks.map { list ->
            list.filter { it.groupId == groupId && it.isActive }
        }
    }

    override fun getAllActiveTasks(): Flow<List<Task>> {
        return tasks.map { list ->
            list.filter { it.isActive }
        }
    }

    override suspend fun getTaskById(id: Long): Result<Task?, DomainError> {
        return Result.Success(tasks.value.find { it.id == id })
    }

    override suspend fun insertTask(task: Task): Result<Long, DomainError> {
        val newId = nextId++
        val newTask = task.copy(id = newId)
        tasks.update { currentList ->
            currentList + newTask
        }
        return Result.Success(newId)
    }

    override suspend fun updateTask(task: Task): Result<Unit, DomainError> {
        tasks.update { currentList ->
            currentList.map { existing ->
                if (existing.id == task.id) task else existing
            }
        }
        return Result.Success(Unit)
    }

    override suspend fun deleteTask(task: Task): Result<Unit, DomainError> {
        tasks.update { currentList ->
            currentList.filter { it.id != task.id }
        }
        return Result.Success(Unit)
    }

    override suspend fun deactivateTask(id: Long): Result<Unit, DomainError> {
        tasks.update { currentList ->
            currentList.map { task ->
                if (task.id == id) task.copy(isActive = false) else task
            }
        }
        return Result.Success(Unit)
    }

    override suspend fun updateProgress(id: Long, note: String?, percent: Int?, goal: String?): Result<Unit, DomainError> {
        tasks.update { currentList ->
            currentList.map { task ->
                if (task.id == id) {
                    task.copy(
                        progressNote = note,
                        progressPercent = percent,
                        nextGoal = goal,
                        updatedAt = LocalDateTime.now()
                    )
                } else {
                    task
                }
            }
        }
        return Result.Success(Unit)
    }

    override fun getTodayScheduledTasks(today: LocalDate): Flow<List<Task>> {
        val todayDayOfWeek = today.dayOfWeek.value // 1=Monday, 7=Sunday
        return tasks.map { list ->
            list.filter { task ->
                if (!task.isActive) return@filter false
                when (task.scheduleType) {
                    ScheduleType.REPEAT -> task.repeatDays.contains(todayDayOfWeek)
                    ScheduleType.SPECIFIC -> task.specificDate == today
                    ScheduleType.DEADLINE -> task.deadlineDate?.let { it >= today } ?: false
                    ScheduleType.NONE -> false
                }
            }
        }
    }

    override suspend fun updateLastStudiedAt(taskId: Long, studiedAt: LocalDateTime): Result<Unit, DomainError> {
        tasks.update { currentList ->
            currentList.map { task ->
                if (task.id == taskId) {
                    task.copy(lastStudiedAt = studiedAt, updatedAt = LocalDateTime.now())
                } else {
                    task
                }
            }
        }
        return Result.Success(Unit)
    }

    override fun getUpcomingDeadlineTasks(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> {
        return tasks.map { list ->
            list.filter { task ->
                task.isActive &&
                task.scheduleType == ScheduleType.DEADLINE &&
                task.deadlineDate != null &&
                task.deadlineDate in startDate..endDate
            }.sortedBy { it.deadlineDate }
        }
    }

    override suspend fun getTasksForDate(date: LocalDate): Result<List<Task>, DomainError> {
        val dayOfWeek = date.dayOfWeek.value
        val result = tasks.value.filter { task ->
            if (!task.isActive) return@filter false
            when (task.scheduleType) {
                ScheduleType.REPEAT -> task.repeatDays.contains(dayOfWeek)
                ScheduleType.SPECIFIC -> task.specificDate == date
                ScheduleType.DEADLINE -> task.deadlineDate == date
                ScheduleType.NONE -> false
            }
        }
        return Result.Success(result)
    }

    override suspend fun getTaskCountByDateRange(startDate: LocalDate, endDate: LocalDate): Result<Map<LocalDate, Int>, DomainError> {
        val result = mutableMapOf<LocalDate, Int>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            val tasksResult = getTasksForDate(current)
            val count = when (tasksResult) {
                is Result.Success -> tasksResult.value.size
                is Result.Failure -> 0
            }
            if (count > 0) {
                result[current] = count
            }
            current = current.plusDays(1)
        }
        return Result.Success(result)
    }

    override fun observeTaskCountByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, Int>> {
        return tasks.map { list ->
            val result = mutableMapOf<LocalDate, Int>()
            var current = startDate
            while (!current.isAfter(endDate)) {
                val dayOfWeek = current.dayOfWeek.value
                val count = list.count { task ->
                    if (!task.isActive) return@count false
                    when (task.scheduleType) {
                        ScheduleType.REPEAT -> task.repeatDays.contains(dayOfWeek)
                        ScheduleType.SPECIFIC -> task.specificDate == current
                        ScheduleType.DEADLINE -> task.deadlineDate == current
                        ScheduleType.NONE -> false
                    }
                }
                if (count > 0) {
                    result[current] = count
                }
                current = current.plusDays(1)
            }
            result.toMap()
        }
    }

    override fun observeTasksForDate(date: LocalDate): Flow<List<Task>> {
        val dayOfWeek = date.dayOfWeek.value
        return tasks.map { list ->
            list.filter { task ->
                if (!task.isActive) return@filter false
                when (task.scheduleType) {
                    ScheduleType.REPEAT -> task.repeatDays.contains(dayOfWeek)
                    ScheduleType.SPECIFIC -> task.specificDate == date
                    ScheduleType.DEADLINE -> task.deadlineDate == date
                    ScheduleType.NONE -> false
                }
            }
        }
    }

    override fun observeGroupColorsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, List<String>>> {
        return tasks.map { list ->
            val result = mutableMapOf<LocalDate, MutableList<String>>()
            var current = startDate
            while (!current.isAfter(endDate)) {
                val dayOfWeek = current.dayOfWeek.value
                list.filter { task ->
                    if (!task.isActive) return@filter false
                    when (task.scheduleType) {
                        ScheduleType.REPEAT -> task.repeatDays.contains(dayOfWeek)
                        ScheduleType.SPECIFIC -> task.specificDate == current
                        ScheduleType.DEADLINE -> task.deadlineDate == current
                        ScheduleType.NONE -> false
                    }
                }.forEach { task ->
                    val color = task.groupColor ?: "#00838F"
                    result.getOrPut(current) { mutableListOf() }.add(color)
                }
                current = current.plusDays(1)
            }
            result.mapValues { it.value.toList() }
        }
    }

    // ==================== Test helpers ====================

    /**
     * 全データをクリア
     */
    fun clear() {
        tasks.value = emptyList()
        nextId = 1L
    }

    /**
     * テスト用にデータを直接セット
     */
    fun setTasks(taskList: List<Task>) {
        tasks.value = taskList
        nextId = (taskList.maxOfOrNull { it.id } ?: 0) + 1
    }

    /**
     * 現在のデータを取得（非同期なし）
     */
    fun getTasksSnapshot(): List<Task> = tasks.value
}
