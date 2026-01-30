package com.iterio.app.domain.model

import java.time.LocalDate

sealed class DeadlineItem {
    abstract val id: Long
    abstract val name: String
    abstract val deadlineDate: LocalDate
    abstract val colorHex: String

    data class TaskDeadline(
        override val id: Long,
        override val name: String,
        override val deadlineDate: LocalDate,
        override val colorHex: String,
        val groupName: String?,
        val taskId: Long
    ) : DeadlineItem()

    data class GroupDeadline(
        override val id: Long,
        override val name: String,
        override val deadlineDate: LocalDate,
        override val colorHex: String
    ) : DeadlineItem()
}
