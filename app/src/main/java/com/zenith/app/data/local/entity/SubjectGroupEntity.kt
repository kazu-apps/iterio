package com.zenith.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "subject_groups")
data class SubjectGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#00838F",
    val displayOrder: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
