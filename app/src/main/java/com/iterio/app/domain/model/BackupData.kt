package com.iterio.app.domain.model

/**
 * バックアップデータのルート構造
 */
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val exportedAt: String, // ISO-8601形式
    val subjectGroups: List<SubjectGroupBackup>,
    val tasks: List<TaskBackup>,
    val studySessions: List<StudySessionBackup>,
    val reviewTasks: List<ReviewTaskBackup>,
    val settings: Map<String, String>,
    val dailyStats: List<DailyStatsBackup>
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * 教科グループのバックアップ
 */
data class SubjectGroupBackup(
    val id: Long,
    val name: String,
    val colorHex: String,
    val displayOrder: Int,
    val createdAt: String, // ISO-8601形式
    val hasDeadline: Boolean = false,
    val deadlineDate: String? = null // ISO-8601 LocalDate形式
)

/**
 * タスクのバックアップ
 */
data class TaskBackup(
    val id: Long,
    val groupId: Long,
    val name: String,
    val progressNote: String?,
    val progressPercent: Int?,
    val nextGoal: String?,
    val workDurationMinutes: Int?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

/**
 * 学習セッションのバックアップ
 */
data class StudySessionBackup(
    val id: Long,
    val taskId: Long,
    val startedAt: String,
    val endedAt: String?,
    val workDurationMinutes: Int,
    val plannedDurationMinutes: Int,
    val cyclesCompleted: Int,
    val wasInterrupted: Boolean,
    val notes: String?
)

/**
 * 復習タスクのバックアップ
 */
data class ReviewTaskBackup(
    val id: Long,
    val studySessionId: Long,
    val taskId: Long,
    val scheduledDate: String, // ISO-8601 LocalDate形式
    val reviewNumber: Int,
    val isCompleted: Boolean,
    val completedAt: String?,
    val createdAt: String
)

/**
 * 日別統計のバックアップ
 */
data class DailyStatsBackup(
    val date: String, // ISO-8601 LocalDate形式
    val totalStudyMinutes: Int,
    val sessionCount: Int,
    val subjectBreakdownJson: String
)

/**
 * インポート結果
 */
data class ImportResult(
    val groupsImported: Int,
    val tasksImported: Int,
    val sessionsImported: Int,
    val reviewTasksImported: Int,
    val settingsImported: Int,
    val statsImported: Int
)
