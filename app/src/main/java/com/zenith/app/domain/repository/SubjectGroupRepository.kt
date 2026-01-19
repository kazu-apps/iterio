package com.zenith.app.domain.repository

import com.zenith.app.domain.model.SubjectGroup
import kotlinx.coroutines.flow.Flow

interface SubjectGroupRepository {
    fun getAllGroups(): Flow<List<SubjectGroup>>
    suspend fun getGroupById(id: Long): SubjectGroup?
    suspend fun insertGroup(group: SubjectGroup): Long
    suspend fun updateGroup(group: SubjectGroup)
    suspend fun deleteGroup(group: SubjectGroup)
    suspend fun deleteGroupById(id: Long)
}
