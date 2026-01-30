package com.iterio.app.domain.repository

import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.SubjectGroup
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface SubjectGroupRepository {
    fun getAllGroups(): Flow<List<SubjectGroup>>
    fun getUpcomingDeadlineGroups(startDate: LocalDate, endDate: LocalDate): Flow<List<SubjectGroup>>
    suspend fun getGroupById(id: Long): Result<SubjectGroup?, DomainError>
    suspend fun insertGroup(group: SubjectGroup): Result<Long, DomainError>
    suspend fun updateGroup(group: SubjectGroup): Result<Unit, DomainError>
    suspend fun deleteGroup(group: SubjectGroup): Result<Unit, DomainError>
    suspend fun deleteGroupById(id: Long): Result<Unit, DomainError>
}
