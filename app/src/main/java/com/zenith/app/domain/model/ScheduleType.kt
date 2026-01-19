package com.zenith.app.domain.model

/**
 * タスクのスケジュールタイプ
 */
enum class ScheduleType(val label: String) {
    NONE("なし"),
    REPEAT("繰り返し"),
    DEADLINE("期限あり"),
    SPECIFIC("特定日");

    companion object {
        fun fromString(value: String?): ScheduleType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}
