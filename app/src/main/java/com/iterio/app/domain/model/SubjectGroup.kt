package com.iterio.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class SubjectGroup(
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#00838F",
    val displayOrder: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val hasDeadline: Boolean = false,
    val deadlineDate: LocalDate? = null
)
