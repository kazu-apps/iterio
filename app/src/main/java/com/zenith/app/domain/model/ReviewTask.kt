package com.zenith.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class ReviewTask(
    val id: Long = 0,
    val studySessionId: Long,
    val taskId: Long,
    val scheduledDate: LocalDate,
    val reviewNumber: Int,
    val isCompleted: Boolean = false,
    val completedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    // Optional related data
    val taskName: String? = null,
    val groupName: String? = null
) {
    val reviewLabel: String
        get() = when (reviewNumber) {
            1 -> "1日後復習"
            2 -> "3日後復習"
            3 -> "1週間後復習"
            4 -> "2週間後復習"
            5 -> "1ヶ月後復習"
            6 -> "2ヶ月後復習"
            else -> "復習 #$reviewNumber"
        }

    companion object {
        val DEFAULT_INTERVALS = listOf(1, 3, 7, 14, 30, 60)

        fun generateReviewTasks(
            studySessionId: Long,
            taskId: Long,
            studyDate: LocalDate,
            intervals: List<Int> = DEFAULT_INTERVALS
        ): List<ReviewTask> {
            return intervals.mapIndexed { index, dayOffset ->
                ReviewTask(
                    studySessionId = studySessionId,
                    taskId = taskId,
                    scheduledDate = studyDate.plusDays(dayOffset.toLong()),
                    reviewNumber = index + 1
                )
            }
        }
    }
}
