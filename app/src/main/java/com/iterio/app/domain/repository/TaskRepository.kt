package com.iterio.app.domain.repository

import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface TaskRepository {
    fun getTasksByGroup(groupId: Long): Flow<List<Task>>
    fun getAllActiveTasks(): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Result<Task?, DomainError>
    suspend fun insertTask(task: Task): Result<Long, DomainError>
    suspend fun updateTask(task: Task): Result<Unit, DomainError>
    suspend fun deleteTask(task: Task): Result<Unit, DomainError>
    suspend fun deactivateTask(id: Long): Result<Unit, DomainError>
    suspend fun updateProgress(id: Long, note: String?, percent: Int?, goal: String?): Result<Unit, DomainError>

    /**
     * 今日のスケジュール対象タスクを取得
     */
    fun getTodayScheduledTasks(today: LocalDate): Flow<List<Task>>

    /**
     * 最終学習日時を更新
     */
    suspend fun updateLastStudiedAt(taskId: Long, studiedAt: LocalDateTime): Result<Unit, DomainError>

    /**
     * 期限が近いタスクを取得
     */
    fun getUpcomingDeadlineTasks(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>>

    /**
     * 特定日のタスクを取得
     */
    suspend fun getTasksForDate(date: LocalDate): Result<List<Task>, DomainError>

    /**
     * 日付範囲のタスク数を取得
     */
    suspend fun getTaskCountByDateRange(startDate: LocalDate, endDate: LocalDate): Result<Map<LocalDate, Int>, DomainError>

    /**
     * 日付範囲のタスク数をリアクティブに観察
     */
    fun observeTaskCountByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, Int>>

    /**
     * 特定日のタスクをリアクティブに観察
     */
    fun observeTasksForDate(date: LocalDate): Flow<List<Task>>

    /**
     * 日付範囲のタスクごとのグループ色をリアクティブに観察（カレンダードット色用）
     */
    fun observeGroupColorsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, List<String>>>
}
