package com.iterio.app.ui.screens.tasks

import com.iterio.app.domain.model.ScheduleType
import com.iterio.app.domain.model.SubjectGroup
import com.iterio.app.domain.model.Task
import java.time.LocalDate

/**
 * タスク画面のイベント
 *
 * ViewModel への全ての UI イベントを統一的に扱う sealed class
 */
sealed class TasksEvent {
    // グループ選択
    /**
     * グループを選択
     */
    data class SelectGroup(val group: SubjectGroup) : TasksEvent()

    // ダイアログ表示/非表示
    /**
     * グループ追加ダイアログを表示
     */
    data object ShowAddGroupDialog : TasksEvent()

    /**
     * グループ追加ダイアログを非表示
     */
    data object HideAddGroupDialog : TasksEvent()

    /**
     * グループ編集ダイアログを表示
     */
    data class ShowEditGroupDialog(val group: SubjectGroup) : TasksEvent()

    /**
     * グループ編集ダイアログを非表示
     */
    data object HideEditGroupDialog : TasksEvent()

    /**
     * タスク追加ダイアログを表示
     */
    data object ShowAddTaskDialog : TasksEvent()

    /**
     * タスク追加ダイアログを非表示
     */
    data object HideAddTaskDialog : TasksEvent()

    /**
     * タスク編集ダイアログを表示
     */
    data class ShowEditTaskDialog(val task: Task) : TasksEvent()

    /**
     * タスク編集ダイアログを非表示
     */
    data object HideEditTaskDialog : TasksEvent()

    // 追加ダイアログ用スケジュール状態
    /**
     * 追加中のスケジュールタイプを更新
     */
    data class UpdateAddingScheduleType(val type: ScheduleType) : TasksEvent()

    /**
     * 追加中の繰り返し曜日を更新
     */
    data class UpdateAddingRepeatDays(val days: Set<Int>) : TasksEvent()

    /**
     * 追加中の締め切り日を更新
     */
    data class UpdateAddingDeadlineDate(val date: LocalDate?) : TasksEvent()

    /**
     * 追加中の特定日を更新
     */
    data class UpdateAddingSpecificDate(val date: LocalDate?) : TasksEvent()

    // 編集ダイアログ用スケジュール状態
    /**
     * 編集中のスケジュールタイプを更新
     */
    data class UpdateEditingScheduleType(val type: ScheduleType) : TasksEvent()

    /**
     * 編集中の繰り返し曜日を更新
     */
    data class UpdateEditingRepeatDays(val days: Set<Int>) : TasksEvent()

    /**
     * 編集中の締め切り日を更新
     */
    data class UpdateEditingDeadlineDate(val date: LocalDate?) : TasksEvent()

    /**
     * 編集中の特定日を更新
     */
    data class UpdateEditingSpecificDate(val date: LocalDate?) : TasksEvent()

    // 復習回数状態
    /**
     * 追加中の復習回数を更新
     */
    data class UpdateAddingReviewCount(val reviewCount: Int?) : TasksEvent()

    /**
     * 編集中の復習回数を更新
     */
    data class UpdateEditingReviewCount(val reviewCount: Int?) : TasksEvent()

    // 復習タスク生成状態
    /**
     * 追加中の復習タスク生成有効/無効を更新
     */
    data class UpdateAddingReviewEnabled(val enabled: Boolean) : TasksEvent()

    /**
     * 編集中の復習タスク生成有効/無効を更新
     */
    data class UpdateEditingReviewEnabled(val enabled: Boolean) : TasksEvent()

    // CRUD操作
    /**
     * グループを追加
     */
    data class AddGroup(
        val name: String,
        val colorHex: String,
        val hasDeadline: Boolean = false,
        val deadlineDate: LocalDate? = null
    ) : TasksEvent()

    /**
     * グループを更新
     */
    data class UpdateGroup(val group: SubjectGroup) : TasksEvent()

    /**
     * グループを削除
     */
    data class DeleteGroup(val group: SubjectGroup) : TasksEvent()

    /**
     * タスクを追加
     */
    data class AddTask(
        val name: String,
        val workDurationMinutes: Int?,
        val scheduleType: ScheduleType = ScheduleType.NONE,
        val repeatDays: Set<Int> = emptySet(),
        val deadlineDate: LocalDate? = null,
        val specificDate: LocalDate? = null,
        val reviewCount: Int? = null,
        val reviewEnabled: Boolean = true
    ) : TasksEvent()

    /**
     * タスクを更新
     */
    data class UpdateTask(val task: Task) : TasksEvent()

    /**
     * タスクを削除
     */
    data class DeleteTask(val task: Task) : TasksEvent()

    /**
     * タスクの進捗を更新
     */
    data class UpdateTaskProgress(
        val taskId: Long,
        val note: String?,
        val percent: Int?,
        val goal: String?
    ) : TasksEvent()
}
