package com.zenith.app.domain.model

import java.time.LocalDate

data class DailyStats(
    val date: LocalDate,
    val totalStudyMinutes: Int = 0,
    val sessionCount: Int = 0,
    val subjectBreakdown: Map<String, Int> = emptyMap()
) {
    val formattedTotalTime: String
        get() {
            val hours = totalStudyMinutes / 60
            val minutes = totalStudyMinutes % 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }
        }

    val hasStudied: Boolean
        get() = totalStudyMinutes > 0
}
