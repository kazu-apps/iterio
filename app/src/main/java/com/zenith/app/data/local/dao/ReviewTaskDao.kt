package com.zenith.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zenith.app.data.local.entity.ReviewTaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface ReviewTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: ReviewTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<ReviewTaskEntity>)

    @Update
    suspend fun update(task: ReviewTaskEntity)

    @Delete
    suspend fun delete(task: ReviewTaskEntity)

    @Query("SELECT * FROM review_tasks WHERE id = :id")
    suspend fun getById(id: Long): ReviewTaskEntity?

    @Query("SELECT * FROM review_tasks WHERE studySessionId = :studySessionId ORDER BY reviewNumber ASC")
    fun getTasksForSession(studySessionId: Long): Flow<List<ReviewTaskEntity>>

    @Query("SELECT * FROM review_tasks WHERE taskId = :taskId ORDER BY scheduledDate ASC")
    fun getTasksForTask(taskId: Long): Flow<List<ReviewTaskEntity>>

    @Query("SELECT * FROM review_tasks WHERE scheduledDate = :date AND isCompleted = 0 ORDER BY reviewNumber ASC")
    fun getPendingTasksForDate(date: LocalDate): Flow<List<ReviewTaskEntity>>

    @Query("SELECT * FROM review_tasks WHERE scheduledDate = :date ORDER BY isCompleted ASC, reviewNumber ASC")
    fun getAllTasksForDate(date: LocalDate): Flow<List<ReviewTaskEntity>>

    @Query("SELECT * FROM review_tasks WHERE scheduledDate <= :date AND isCompleted = 0 ORDER BY scheduledDate ASC, reviewNumber ASC")
    fun getOverdueAndTodayTasks(date: LocalDate): Flow<List<ReviewTaskEntity>>

    @Query("SELECT COUNT(*) FROM review_tasks WHERE scheduledDate = :date AND isCompleted = 0")
    suspend fun getPendingTaskCountForDate(date: LocalDate): Int

    @Query("SELECT COUNT(*) FROM review_tasks WHERE scheduledDate = :date")
    suspend fun getTotalTaskCountForDate(date: LocalDate): Int

    @Query("DELETE FROM review_tasks WHERE studySessionId = :studySessionId")
    suspend fun deleteTasksForSession(studySessionId: Long)

    @Query("DELETE FROM review_tasks WHERE taskId = :taskId")
    suspend fun deleteTasksForTask(taskId: Long)

    @Query("UPDATE review_tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :id")
    suspend fun markAsCompleted(id: Long, completedAt: LocalDateTime)

    @Query("UPDATE review_tasks SET isCompleted = 0, completedAt = NULL WHERE id = :id")
    suspend fun markAsIncomplete(id: Long)

    @Query("UPDATE review_tasks SET scheduledDate = :newDate WHERE id = :id")
    suspend fun reschedule(id: Long, newDate: LocalDate)

    @Query("SELECT * FROM review_tasks ORDER BY createdAt DESC")
    suspend fun getAll(): List<ReviewTaskEntity>
}
