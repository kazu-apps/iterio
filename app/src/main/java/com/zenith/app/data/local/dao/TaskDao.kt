package com.zenith.app.data.local.dao

import androidx.room.*
import com.zenith.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE groupId = :groupId AND isActive = 1 ORDER BY createdAt DESC")
    fun getTasksByGroup(groupId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET isActive = 0 WHERE id = :id")
    suspend fun deactivateTask(id: Long)

    @Query("UPDATE tasks SET progressNote = :note, progressPercent = :percent, nextGoal = :goal, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        note: String?,
        percent: Int?,
        goal: String?,
        updatedAt: java.time.LocalDateTime
    )

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    suspend fun getAllTasks(): List<TaskEntity>

    /**
     * 今日のスケジュール対象タスクを取得
     * - 繰り返し: 今日の曜日が含まれる
     * - 期限: 期限日が今日
     * - 特定日: 今日と一致
     */
    @Query("""
        SELECT * FROM tasks
        WHERE isActive = 1 AND (
            (scheduleType = 'repeat' AND repeatDays LIKE '%' || :dayOfWeek || '%')
            OR (scheduleType = 'deadline' AND deadlineDate = :today)
            OR (scheduleType = 'specific' AND specificDate = :today)
        )
        ORDER BY updatedAt DESC
    """)
    fun getTodayScheduledTasks(today: String, dayOfWeek: String): Flow<List<TaskEntity>>

    /**
     * 最終学習日時を更新
     */
    @Query("UPDATE tasks SET lastStudiedAt = :studiedAt, updatedAt = :studiedAt WHERE id = :taskId")
    suspend fun updateLastStudiedAt(taskId: Long, studiedAt: java.time.LocalDateTime)
}
