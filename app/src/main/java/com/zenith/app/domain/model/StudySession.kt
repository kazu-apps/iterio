package com.zenith.app.domain.model

import java.time.LocalDateTime

data class StudySession(
    val id: Long = 0,
    val taskId: Long,
    val taskName: String? = null,
    val groupName: String? = null,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime? = null,
    val workDurationMinutes: Int = 0,
    val plannedDurationMinutes: Int = 25,
    val cyclesCompleted: Int = 0,
    val wasInterrupted: Boolean = false,
    val notes: String? = null
)
