package com.iterio.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.iterio.app.data.local.converter.DateTimeConverters
import com.iterio.app.data.local.dao.*
import com.iterio.app.data.local.entity.*

@Database(
    entities = [
        SubjectGroupEntity::class,
        TaskEntity::class,
        StudySessionEntity::class,
        ReviewTaskEntity::class,
        SettingsEntity::class,
        DailyStatsEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(DateTimeConverters::class)
abstract class IterioDatabase : RoomDatabase() {
    abstract fun subjectGroupDao(): SubjectGroupDao
    abstract fun taskDao(): TaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun reviewTaskDao(): ReviewTaskDao
    abstract fun settingsDao(): SettingsDao
    abstract fun dailyStatsDao(): DailyStatsDao

    companion object {
        const val DATABASE_NAME = "iterio_database"
    }
}
