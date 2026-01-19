package com.zenith.app.domain.repository

import com.zenith.app.domain.model.Subject
import kotlinx.coroutines.flow.Flow

interface SubjectRepository {
    suspend fun insert(subject: Subject): Long
    suspend fun update(subject: Subject)
    suspend fun delete(subject: Subject)
    suspend fun getById(id: Long): Subject?
    fun getAllSubjects(): Flow<List<Subject>>
    fun getTemplateSubjects(): Flow<List<Subject>>
    fun searchSubjects(query: String): Flow<List<Subject>>
}
