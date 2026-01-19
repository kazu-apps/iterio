package com.zenith.app.di

import android.content.Context
import androidx.room.Room
import com.zenith.app.data.local.ZenithDatabase
import com.zenith.app.data.local.dao.*
import com.zenith.app.data.local.migration.MIGRATION_3_4
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
    fun provideDatabase(@ApplicationContext context: Context): ZenithDatabase {
        return Room.databaseBuilder(
            context,
            ZenithDatabase::class.java,
            ZenithDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSubjectGroupDao(database: ZenithDatabase): SubjectGroupDao {
        return database.subjectGroupDao()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: ZenithDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideStudySessionDao(database: ZenithDatabase): StudySessionDao {
        return database.studySessionDao()
    }

    @Provides
    @Singleton
    fun provideReviewTaskDao(database: ZenithDatabase): ReviewTaskDao {
        return database.reviewTaskDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: ZenithDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideDailyStatsDao(database: ZenithDatabase): DailyStatsDao {
        return database.dailyStatsDao()
    }
}
