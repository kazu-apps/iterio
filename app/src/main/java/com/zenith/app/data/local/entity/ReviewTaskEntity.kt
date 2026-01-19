package com.zenith.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "review_tasks",
    foreignKeys = [
        ForeignKey(
            entity = StudySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["studySessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studySessionId"]),
        Index(value = ["taskId"]),
        Index(value = ["scheduledDate"])
    ]
)
data class ReviewTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studySessionId: Long,
    val taskId: Long,
    val scheduledDate: LocalDate,
    val reviewNumber: Int, // 1=1日後, 2=3日後, 3=1週後, 4=2週後, 5=1ヶ月後, 6=2ヶ月後
    val isCompleted: Boolean = false,
    val completedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        // Review intervals in days
        val REVIEW_INTERVALS = listOf(1, 3, 7, 14, 30, 60)

        fun getReviewIntervalDays(reviewNumber: Int): Int {
            return REVIEW_INTERVALS.getOrElse(reviewNumber - 1) { 1 }
        }
    }
}
