package com.zenith.app.data.repository

import com.zenith.app.data.local.dao.SubjectDao
import com.zenith.app.data.local.entity.SubjectEntity
import com.zenith.app.domain.model.Subject
import com.zenith.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SubjectRepositoryImpl @Inject constructor(
    private val subjectDao: SubjectDao
) : SubjectRepository {

    override suspend fun insert(subject: Subject): Long {
        val maxOrder = subjectDao.getMaxDisplayOrder() ?: 0
        val entity = subject.toEntity().copy(displayOrder = maxOrder + 1)
        return subjectDao.insert(entity)
    }

    override suspend fun update(subject: Subject) {
        subjectDao.update(subject.toEntity())
    }

    override suspend fun delete(subject: Subject) {
        subjectDao.delete(subject.toEntity())
    }

    override suspend fun getById(id: Long): Subject? {
        return subjectDao.getById(id)?.toDomain()
    }

    override fun getAllSubjects(): Flow<List<Subject>> {
        return subjectDao.getAllSubjects().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTemplateSubjects(): Flow<List<Subject>> {
        return subjectDao.getTemplateSubjects().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchSubjects(query: String): Flow<List<Subject>> {
        return subjectDao.searchSubjects(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun Subject.toEntity(): SubjectEntity {
        return SubjectEntity(
            id = id,
            name = name,
            colorHex = colorHex,
            isTemplate = isTemplate,
            displayOrder = displayOrder,
            createdAt = createdAt
        )
    }

    private fun SubjectEntity.toDomain(): Subject {
        return Subject(
            id = id,
            name = name,
            colorHex = colorHex,
            isTemplate = isTemplate,
            displayOrder = displayOrder,
            createdAt = createdAt
        )
    }
}
