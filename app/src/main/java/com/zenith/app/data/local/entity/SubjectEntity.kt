package com.zenith.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#00838F",
    val isTemplate: Boolean = false,
    val displayOrder: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
