package com.zenith.app.data.local.dao

import androidx.room.*
import com.zenith.app.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): SettingsEntity?

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingsEntity)

    @Query("INSERT OR REPLACE INTO settings (`key`, value) VALUES (:key, :value)")
    suspend fun setSetting(key: String, value: String)

    @Delete
    suspend fun deleteSetting(setting: SettingsEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteSettingByKey(key: String)

    @Transaction
    suspend fun getSettingOrDefault(key: String, defaultValue: String): String {
        return getSettingValue(key) ?: defaultValue.also { setSetting(key, it) }
    }

    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingsEntity>
}
