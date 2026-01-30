package com.iterio.app.data.mapper

import com.iterio.app.data.local.entity.SubjectGroupEntity
import com.iterio.app.domain.model.SubjectGroup
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SubjectGroupEntity と SubjectGroup 間の変換を行うマッパー
 */
@Singleton
class SubjectGroupMapper @Inject constructor() : Mapper<SubjectGroupEntity, SubjectGroup> {

    override fun toDomain(entity: SubjectGroupEntity): SubjectGroup {
        return SubjectGroup(
            id = entity.id,
            name = entity.name,
            colorHex = entity.colorHex,
            displayOrder = entity.displayOrder,
            createdAt = entity.createdAt,
            hasDeadline = entity.hasDeadline,
            deadlineDate = entity.deadlineDate
        )
    }

    override fun toEntity(domain: SubjectGroup): SubjectGroupEntity {
        return SubjectGroupEntity(
            id = domain.id,
            name = domain.name,
            colorHex = domain.colorHex,
            displayOrder = domain.displayOrder,
            createdAt = domain.createdAt,
            hasDeadline = domain.hasDeadline,
            deadlineDate = domain.deadlineDate
        )
    }
}
