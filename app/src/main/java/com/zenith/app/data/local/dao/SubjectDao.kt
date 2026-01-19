package com.zenith.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zenith.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: Long): SubjectEntity?

    @Query("SELECT * FROM subjects ORDER BY displayOrder ASC, name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isTemplate = 1 ORDER BY displayOrder ASC, name ASC")
    fun getTemplateSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchSubjects(query: String): Flow<List<SubjectEntity>>

    @Query("SELECT MAX(displayOrder) FROM subjects")
    suspend fun getMaxDisplayOrder(): Int?
}
