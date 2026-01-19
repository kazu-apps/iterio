package com.zenith.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime? = null,
    val workDurationMinutes: Int = 0,
    val plannedDurationMinutes: Int = 25,
    val cyclesCompleted: Int = 0,
    val wasInterrupted: Boolean = false,
    val notes: String? = null
)
