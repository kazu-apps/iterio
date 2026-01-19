package com.zenith.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.zenith.app.data.encryption.EncryptionException
import com.zenith.app.data.encryption.EncryptionManager
import com.zenith.app.data.local.ZenithDatabase
import com.zenith.app.data.local.dao.*
import com.zenith.app.data.local.entity.*
import com.zenith.app.domain.model.*
import com.zenith.app.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ZenithDatabase,
    private val subjectGroupDao: SubjectGroupDao,
    private val taskDao: TaskDao,
    private val studySessionDao: StudySessionDao,
    private val reviewTaskDao: ReviewTaskDao,
    private val settingsDao: SettingsDao,
    private val dailyStatsDao: DailyStatsDao,
    private val gson: Gson,
    private val encryptionManager: EncryptionManager
) : BackupRepository {

    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override suspend fun exportBackup(): BackupData {
        val groups = subjectGroupDao.getAll().map { it.toBackup() }
        val tasks = taskDao.getAllTasks().map { it.toBackup() }
        val sessions = studySessionDao.getAll().map { it.toBackup() }
        val reviews = reviewTaskDao.getAll().map { it.toBackup() }
        val settings = settingsDao.getAll().associate { it.key to it.value }
        val stats = dailyStatsDao.getAll().map { it.toBackup() }

        return BackupData(
            version = BackupData.CURRENT_VERSION,
            exportedAt = LocalDateTime.now().format(dateTimeFormatter),
            subjectGroups = groups,
            tasks = tasks,
            studySessions = sessions,
            reviewTasks = reviews,
            settings = settings,
            dailyStats = stats
        )
    }

    override suspend fun importBackup(data: BackupData): ImportResult {
        return database.withTransaction {
            // 外部キー制約があるため、子テーブルから削除
            clearAllData()

            // 親から順にインサート
            data.subjectGroups.forEach { group ->
                subjectGroupDao.insertGroup(group.toEntity())
            }

            data.tasks.forEach { task ->
                taskDao.insertTask(task.toEntity())
            }

            data.studySessions.forEach { session ->
                studySessionDao.insertSession(session.toEntity())
            }

            data.reviewTasks.forEach { review ->
                reviewTaskDao.insert(review.toEntity())
            }

            data.settings.forEach { (key, value) ->
                settingsDao.setSetting(key, value)
            }

            data.dailyStats.forEach { stat ->
                dailyStatsDao.insert(stat.toEntity())
            }

            ImportResult(
                groupsImported = data.subjectGroups.size,
                tasksImported = data.tasks.size,
                sessionsImported = data.studySessions.size,
                reviewTasksImported = data.reviewTasks.size,
                settingsImported = data.settings.size,
                statsImported = data.dailyStats.size
            )
        }
    }

    private fun clearAllData() {
        // 外部キー制約の順序でクリア（子→親）
        database.openHelper.writableDatabase.apply {
            execSQL("DELETE FROM review_tasks")
            execSQL("DELETE FROM study_sessions")
            execSQL("DELETE FROM tasks")
            execSQL("DELETE FROM subject_groups")
            execSQL("DELETE FROM settings")
            execSQL("DELETE FROM daily_stats")
        }
    }

    override suspend fun writeToFile(data: BackupData, uri: Uri): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val json = gson.toJson(data)
                // 暗号化してバイナリで書き込み
                val encryptedBytes = encryptionManager.encryptBackup(json)
                outputStream.write(encryptedBytes)
            } ?: return Result.failure(Exception("ファイルを開けませんでした"))
            Result.success(Unit)
        } catch (e: EncryptionException) {
            Result.failure(Exception("暗号化に失敗しました: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readFromFile(uri: Uri): Result<BackupData> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: return Result.failure(Exception("ファイルを開けませんでした"))

            // 暗号化判定して復号化
            val json = if (encryptionManager.isEncryptedData(bytes)) {
                try {
                    encryptionManager.decryptBackup(bytes)
                } catch (e: EncryptionException) {
                    return Result.failure(Exception("復号化に失敗しました: ${e.message}"))
                }
            } else {
                // 旧形式（平文JSON）の後方互換性
                String(bytes, Charsets.UTF_8)
            }

            val data = gson.fromJson(json, BackupData::class.java)
                ?: return Result.failure(Exception("ファイル形式が不正です"))

            if (data.version > BackupData.CURRENT_VERSION) {
                return Result.failure(Exception("このバックアップは新しいバージョンで作成されました"))
            }

            Result.success(data)
        } catch (e: JsonSyntaxException) {
            Result.failure(Exception("ファイル形式が不正です"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Entity → Backup変換
    private fun SubjectGroupEntity.toBackup() = SubjectGroupBackup(
        id = id,
        name = name,
        colorHex = colorHex,
        displayOrder = displayOrder,
        createdAt = createdAt.format(dateTimeFormatter)
    )

    private fun TaskEntity.toBackup() = TaskBackup(
        id = id,
        groupId = groupId,
        name = name,
        progressNote = progressNote,
        progressPercent = progressPercent,
        nextGoal = nextGoal,
        workDurationMinutes = workDurationMinutes,
        isActive = isActive,
        createdAt = createdAt.format(dateTimeFormatter),
        updatedAt = updatedAt.format(dateTimeFormatter)
    )

    private fun StudySessionEntity.toBackup() = StudySessionBackup(
        id = id,
        taskId = taskId,
        startedAt = startedAt.format(dateTimeFormatter),
        endedAt = endedAt?.format(dateTimeFormatter),
        workDurationMinutes = workDurationMinutes,
        plannedDurationMinutes = plannedDurationMinutes,
        cyclesCompleted = cyclesCompleted,
        wasInterrupted = wasInterrupted,
        notes = notes
    )

    private fun ReviewTaskEntity.toBackup() = ReviewTaskBackup(
        id = id,
        studySessionId = studySessionId,
        taskId = taskId,
        scheduledDate = scheduledDate.format(dateFormatter),
        reviewNumber = reviewNumber,
        isCompleted = isCompleted,
        completedAt = completedAt?.format(dateTimeFormatter),
        createdAt = createdAt.format(dateTimeFormatter)
    )

    private fun DailyStatsEntity.toBackup() = DailyStatsBackup(
        date = date.format(dateFormatter),
        totalStudyMinutes = totalStudyMinutes,
        sessionCount = sessionCount,
        subjectBreakdownJson = subjectBreakdownJson
    )

    // Backup → Entity変換
    private fun SubjectGroupBackup.toEntity() = SubjectGroupEntity(
        id = id,
        name = name,
        colorHex = colorHex,
        displayOrder = displayOrder,
        createdAt = LocalDateTime.parse(createdAt, dateTimeFormatter)
    )

    private fun TaskBackup.toEntity() = TaskEntity(
        id = id,
        groupId = groupId,
        name = name,
        progressNote = progressNote,
        progressPercent = progressPercent,
        nextGoal = nextGoal,
        workDurationMinutes = workDurationMinutes,
        isActive = isActive,
        createdAt = LocalDateTime.parse(createdAt, dateTimeFormatter),
        updatedAt = LocalDateTime.parse(updatedAt, dateTimeFormatter)
    )

    private fun StudySessionBackup.toEntity() = StudySessionEntity(
        id = id,
        taskId = taskId,
        startedAt = LocalDateTime.parse(startedAt, dateTimeFormatter),
        endedAt = endedAt?.let { LocalDateTime.parse(it, dateTimeFormatter) },
        workDurationMinutes = workDurationMinutes,
        plannedDurationMinutes = plannedDurationMinutes,
        cyclesCompleted = cyclesCompleted,
        wasInterrupted = wasInterrupted,
        notes = notes
    )

    private fun ReviewTaskBackup.toEntity() = ReviewTaskEntity(
        id = id,
        studySessionId = studySessionId,
        taskId = taskId,
        scheduledDate = LocalDate.parse(scheduledDate, dateFormatter),
        reviewNumber = reviewNumber,
        isCompleted = isCompleted,
        completedAt = completedAt?.let { LocalDateTime.parse(it, dateTimeFormatter) },
        createdAt = LocalDateTime.parse(createdAt, dateTimeFormatter)
    )

    private fun DailyStatsBackup.toEntity() = DailyStatsEntity(
        date = LocalDate.parse(date, dateFormatter),
        totalStudyMinutes = totalStudyMinutes,
        sessionCount = sessionCount,
        subjectBreakdownJson = subjectBreakdownJson
    )

    override fun serializeToJson(data: BackupData): String {
        return gson.toJson(data)
    }

    override fun deserializeFromJson(json: String): Result<BackupData> {
        return try {
            val data = gson.fromJson(json, BackupData::class.java)
                ?: return Result.failure(Exception("ファイル形式が不正です"))

            if (data.version > BackupData.CURRENT_VERSION) {
                return Result.failure(Exception("このバックアップは新しいバージョンで作成されました"))
            }

            Result.success(data)
        } catch (e: JsonSyntaxException) {
            Result.failure(Exception("ファイル形式が不正です"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
