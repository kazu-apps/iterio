package com.zenith.app.data.repository

import com.zenith.app.data.local.dao.SubjectGroupDao
import com.zenith.app.data.local.entity.SubjectGroupEntity
import com.zenith.app.domain.model.SubjectGroup
import com.zenith.app.domain.repository.SubjectGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectGroupRepositoryImpl @Inject constructor(
    private val subjectGroupDao: SubjectGroupDao
) : SubjectGroupRepository {

    override fun getAllGroups(): Flow<List<SubjectGroup>> {
        return subjectGroupDao.getAllGroups().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getGroupById(id: Long): SubjectGroup? {
        return subjectGroupDao.getGroupById(id)?.toDomainModel()
    }

    override suspend fun insertGroup(group: SubjectGroup): Long {
        val displayOrder = subjectGroupDao.getNextDisplayOrder()
        return subjectGroupDao.insertGroup(group.toEntity().copy(displayOrder = displayOrder))
    }

    override suspend fun updateGroup(group: SubjectGroup) {
        subjectGroupDao.updateGroup(group.toEntity())
    }

    override suspend fun deleteGroup(group: SubjectGroup) {
        subjectGroupDao.deleteGroup(group.toEntity())
    }

    override suspend fun deleteGroupById(id: Long) {
        subjectGroupDao.deleteGroupById(id)
    }

    private fun SubjectGroupEntity.toDomainModel(): SubjectGroup {
        return SubjectGroup(
            id = id,
            name = name,
            colorHex = colorHex,
            displayOrder = displayOrder,
            createdAt = createdAt
        )
    }

    private fun SubjectGroup.toEntity(): SubjectGroupEntity {
        return SubjectGroupEntity(
            id = id,
            name = name,
            colorHex = colorHex,
            displayOrder = displayOrder,
            createdAt = createdAt
        )
    }
}
