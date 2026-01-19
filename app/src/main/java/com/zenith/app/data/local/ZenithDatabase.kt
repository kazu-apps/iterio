package com.zenith.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zenith.app.data.local.converter.DateTimeConverters
import com.zenith.app.data.local.dao.*
import com.zenith.app.data.local.entity.*

@Database(
    entities = [
        SubjectGroupEntity::class,
        TaskEntity::class,
        StudySessionEntity::class,
        ReviewTaskEntity::class,
        SettingsEntity::class,
        DailyStatsEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(DateTimeConverters::class)
abstract class ZenithDatabase : RoomDatabase() {
    abstract fun subjectGroupDao(): SubjectGroupDao
    abstract fun taskDao(): TaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun reviewTaskDao(): ReviewTaskDao
    abstract fun settingsDao(): SettingsDao
    abstract fun dailyStatsDao(): DailyStatsDao

    companion object {
        const val DATABASE_NAME = "zenith_database"
    }
}
