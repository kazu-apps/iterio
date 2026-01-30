package com.iterio.app.data.repository

import com.iterio.app.data.local.dao.SubjectGroupDao
import com.iterio.app.data.mapper.SubjectGroupMapper
import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.SubjectGroup
import com.iterio.app.domain.repository.SubjectGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectGroupRepositoryImpl @Inject constructor(
    private val subjectGroupDao: SubjectGroupDao,
    private val mapper: SubjectGroupMapper
) : SubjectGroupRepository {

    override fun getAllGroups(): Flow<List<SubjectGroup>> {
        return subjectGroupDao.getAllGroups().map { entities ->
            mapper.toDomainList(entities)
        }
    }

    override fun getUpcomingDeadlineGroups(startDate: LocalDate, endDate: LocalDate): Flow<List<SubjectGroup>> {
        return subjectGroupDao.getUpcomingDeadlineGroups(startDate.toString(), endDate.toString())
            .map { entities -> mapper.toDomainList(entities) }
    }

    override suspend fun getGroupById(id: Long): Result<SubjectGroup?, DomainError> =
        Result.catchingSuspend {
            subjectGroupDao.getGroupById(id)?.let { mapper.toDomain(it) }
        }

    override suspend fun insertGroup(group: SubjectGroup): Result<Long, DomainError> =
        Result.catchingSuspend {
            val displayOrder = subjectGroupDao.getNextDisplayOrder()
            subjectGroupDao.insertGroup(mapper.toEntity(group).copy(displayOrder = displayOrder))
        }

    override suspend fun updateGroup(group: SubjectGroup): Result<Unit, DomainError> =
        Result.catchingSuspend {
            subjectGroupDao.updateGroup(mapper.toEntity(group))
        }

    override suspend fun deleteGroup(group: SubjectGroup): Result<Unit, DomainError> =
        Result.catchingSuspend {
            subjectGroupDao.deleteGroup(mapper.toEntity(group))
        }

    override suspend fun deleteGroupById(id: Long): Result<Unit, DomainError> =
        Result.catchingSuspend {
            subjectGroupDao.deleteGroupById(id)
        }
}
