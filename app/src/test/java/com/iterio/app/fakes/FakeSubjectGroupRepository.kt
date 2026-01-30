package com.iterio.app.fakes

import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.SubjectGroup
import com.iterio.app.domain.repository.SubjectGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * SubjectGroupRepositoryのテスト用Fake実装
 *
 * インメモリでデータを保持し、Flowで変更を通知します。
 *
 * 使用例:
 * ```
 * val fakeRepo = FakeSubjectGroupRepository()
 * fakeRepo.insertGroup(SubjectGroup(name = "Math"))
 *
 * // Flowでの検証
 * fakeRepo.getAllGroups().test {
 *     assertThat(awaitItem()).hasSize(1)
 * }
 * ```
 */
class FakeSubjectGroupRepository : SubjectGroupRepository {

    private val groups = MutableStateFlow<List<SubjectGroup>>(emptyList())
    private var nextId = 1L

    override fun getAllGroups(): Flow<List<SubjectGroup>> {
        return groups.map { list ->
            list.sortedBy { it.displayOrder }
        }
    }

    override fun getUpcomingDeadlineGroups(startDate: LocalDate, endDate: LocalDate): Flow<List<SubjectGroup>> {
        return groups.map { list ->
            list.filter { group ->
                group.hasDeadline &&
                group.deadlineDate != null &&
                group.deadlineDate > startDate &&
                group.deadlineDate <= endDate
            }.sortedBy { it.deadlineDate }
        }
    }

    override suspend fun getGroupById(id: Long): Result<SubjectGroup?, DomainError> {
        return Result.Success(groups.value.find { it.id == id })
    }

    override suspend fun insertGroup(group: SubjectGroup): Result<Long, DomainError> {
        val newId = nextId++
        val newGroup = group.copy(
            id = newId,
            displayOrder = groups.value.size
        )
        groups.update { currentList ->
            currentList + newGroup
        }
        return Result.Success(newId)
    }

    override suspend fun updateGroup(group: SubjectGroup): Result<Unit, DomainError> {
        groups.update { currentList ->
            currentList.map { existing ->
                if (existing.id == group.id) group else existing
            }
        }
        return Result.Success(Unit)
    }

    override suspend fun deleteGroup(group: SubjectGroup): Result<Unit, DomainError> {
        groups.update { currentList ->
            currentList.filter { it.id != group.id }
        }
        return Result.Success(Unit)
    }

    override suspend fun deleteGroupById(id: Long): Result<Unit, DomainError> {
        groups.update { currentList ->
            currentList.filter { it.id != id }
        }
        return Result.Success(Unit)
    }

    // ==================== Test helpers ====================

    /**
     * 全データをクリア
     */
    fun clear() {
        groups.value = emptyList()
        nextId = 1L
    }

    /**
     * テスト用にデータを直接セット
     */
    fun setGroups(groupList: List<SubjectGroup>) {
        groups.value = groupList
        nextId = (groupList.maxOfOrNull { it.id } ?: 0) + 1
    }

    /**
     * 現在のデータを取得（非同期なし）
     */
    fun getGroupsSnapshot(): List<SubjectGroup> = groups.value
}
