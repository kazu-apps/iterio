package com.zenith.app.domain.repository

import com.zenith.app.domain.model.ReviewTask
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ReviewTaskRepository {
    suspend fun insert(task: ReviewTask): Long
    suspend fun insertAll(tasks: List<ReviewTask>)
    suspend fun update(task: ReviewTask)
    suspend fun delete(task: ReviewTask)
    suspend fun getById(id: Long): ReviewTask?
    fun getTasksForSession(studySessionId: Long): Flow<List<ReviewTask>>
    fun getTasksForTask(taskId: Long): Flow<List<ReviewTask>>
    fun getPendingTasksForDate(date: LocalDate): Flow<List<ReviewTask>>
    fun getAllTasksForDate(date: LocalDate): Flow<List<ReviewTask>>
    fun getOverdueAndTodayTasks(date: LocalDate): Flow<List<ReviewTask>>
    suspend fun getPendingTaskCountForDate(date: LocalDate): Int
    suspend fun markAsCompleted(taskId: Long)
    suspend fun markAsIncomplete(taskId: Long)
    suspend fun reschedule(taskId: Long, newDate: LocalDate)
    suspend fun deleteTasksForSession(studySessionId: Long)
    suspend fun deleteTasksForTask(taskId: Long)
}
