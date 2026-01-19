package com.zenith.app.data.local.dao

import androidx.room.*
import com.zenith.app.data.local.entity.SubjectGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectGroupDao {
    @Query("SELECT * FROM subject_groups ORDER BY displayOrder ASC, createdAt DESC")
    fun getAllGroups(): Flow<List<SubjectGroupEntity>>

    @Query("SELECT * FROM subject_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): SubjectGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: SubjectGroupEntity): Long

    @Update
    suspend fun updateGroup(group: SubjectGroupEntity)

    @Delete
    suspend fun deleteGroup(group: SubjectGroupEntity)

    @Query("DELETE FROM subject_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    @Query("SELECT COALESCE(MAX(displayOrder), 0) + 1 FROM subject_groups")
    suspend fun getNextDisplayOrder(): Int

    @Query("SELECT * FROM subject_groups ORDER BY displayOrder ASC, createdAt DESC")
    suspend fun getAll(): List<SubjectGroupEntity>
}
