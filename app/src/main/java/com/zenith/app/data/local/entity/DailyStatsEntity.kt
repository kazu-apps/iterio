package com.zenith.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val date: LocalDate,
    val totalStudyMinutes: Int = 0,
    val sessionCount: Int = 0,
    val subjectBreakdownJson: String = "{}"
)
