package com.iterio.app.di

import android.content.Context
import androidx.room.Room
import com.iterio.app.data.local.IterioDatabase
import com.iterio.app.data.local.dao.*
import com.iterio.app.data.local.migration.MIGRATION_3_4
import com.iterio.app.data.local.migration.MIGRATION_4_5
import com.iterio.app.data.local.migration.MIGRATION_5_6
import com.iterio.app.data.local.migration.MIGRATION_6_7
import com.iterio.app.data.local.migration.MIGRATION_7_8
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IterioDatabase {
        return Room.databaseBuilder(
            context,
            IterioDatabase::class.java,
            IterioDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSubjectGroupDao(database: IterioDatabase): SubjectGroupDao {
        return database.subjectGroupDao()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: IterioDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideStudySessionDao(database: IterioDatabase): StudySessionDao {
        return database.studySessionDao()
    }

    @Provides
    @Singleton
    fun provideReviewTaskDao(database: IterioDatabase): ReviewTaskDao {
        return database.reviewTaskDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: IterioDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideDailyStatsDao(database: IterioDatabase): DailyStatsDao {
        return database.dailyStatsDao()
    }
}
