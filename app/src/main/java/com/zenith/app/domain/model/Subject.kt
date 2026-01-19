package com.zenith.app.domain.model

import java.time.LocalDateTime

data class Subject(
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#00838F",
    val isTemplate: Boolean = false,
    val displayOrder: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
