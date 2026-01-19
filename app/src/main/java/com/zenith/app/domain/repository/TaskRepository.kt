package com.zenith.app.domain.repository

import com.zenith.app.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface TaskRepository {
    fun getTasksByGroup(groupId: Long): Flow<List<Task>>
    fun getAllActiveTasks(): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Task?
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun deactivateTask(id: Long)
    suspend fun updateProgress(id: Long, note: String?, percent: Int?, goal: String?)

    /**
     * 今日のスケジュール対象タスクを取得
     */
    fun getTodayScheduledTasks(today: LocalDate): Flow<List<Task>>

    /**
     * 最終学習日時を更新
     */
    suspend fun updateLastStudiedAt(taskId: Long, studiedAt: LocalDateTime)
}
