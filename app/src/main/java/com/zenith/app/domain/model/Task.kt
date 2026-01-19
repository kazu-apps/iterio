package com.zenith.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class Task(
    val id: Long = 0,
    val groupId: Long,
    val groupName: String? = null,
    val groupColor: String? = null,
    val name: String,
    val progressNote: String? = null,
    val progressPercent: Int? = null,
    val nextGoal: String? = null,
    val workDurationMinutes: Int? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    // スケジュール関連
    val scheduleType: ScheduleType = ScheduleType.NONE,
    val repeatDays: Set<Int> = emptySet(),  // 1=月曜, 7=日曜
    val deadlineDate: LocalDate? = null,
    val specificDate: LocalDate? = null,
    val lastStudiedAt: LocalDateTime? = null
) {
    /**
     * スケジュール表示用ラベル
     */
    val scheduleLabel: String?
        get() = when (scheduleType) {
            ScheduleType.NONE -> null
            ScheduleType.REPEAT -> {
                if (repeatDays.isEmpty()) null
                else {
                    val dayLabels = listOf("月", "火", "水", "木", "金", "土", "日")
                    repeatDays.sorted().mapNotNull { day ->
                        dayLabels.getOrNull(day - 1)
                    }.joinToString("・")
                }
            }
            ScheduleType.DEADLINE -> deadlineDate?.let { date ->
                val formatter = DateTimeFormatter.ofPattern("M/d")
                "期限: ${date.format(formatter)}"
            }
            ScheduleType.SPECIFIC -> specificDate?.let { date ->
                val formatter = DateTimeFormatter.ofPattern("M/d")
                date.format(formatter)
            }
        }

    /**
     * 最終学習日の表示用ラベル
     */
    val lastStudiedLabel: String?
        get() = lastStudiedAt?.let { date ->
            val formatter = DateTimeFormatter.ofPattern("M/d")
            "最終: ${date.format(formatter)}"
        }

    /**
     * 期限切れかどうか
     */
    val isOverdue: Boolean
        get() = scheduleType == ScheduleType.DEADLINE &&
                deadlineDate != null &&
                deadlineDate.isBefore(LocalDate.now())

    companion object {
        /**
         * repeatDays文字列をSetに変換
         */
        fun parseRepeatDays(repeatDaysStr: String?): Set<Int> {
            if (repeatDaysStr.isNullOrBlank()) return emptySet()
            return repeatDaysStr.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .toSet()
        }

        /**
         * SetをrepeatDays文字列に変換
         */
        fun formatRepeatDays(days: Set<Int>): String? {
            if (days.isEmpty()) return null
            return days.sorted().joinToString(",")
        }
    }
}
