package com.zenith.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.zenith.app.data.local.ZenithDatabase
import com.zenith.app.data.local.dao.*
import com.zenith.app.data.repository.*
import com.zenith.app.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.premiumDataStore: DataStore<Preferences> by preferencesDataStore(name = "premium_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }

    @Provides
    @Singleton
    fun provideSubjectGroupRepository(
        subjectGroupDao: SubjectGroupDao
    ): SubjectGroupRepository {
        return SubjectGroupRepositoryImpl(subjectGroupDao)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao
    ): TaskRepository {
        return TaskRepositoryImpl(taskDao)
    }

    @Provides
    @Singleton
    fun provideStudySessionRepository(
        studySessionDao: StudySessionDao
    ): StudySessionRepository {
        return StudySessionRepositoryImpl(studySessionDao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsDao: SettingsDao
    ): SettingsRepository {
        return SettingsRepositoryImpl(settingsDao)
    }

    @Provides
    @Singleton
    fun provideDailyStatsRepository(
        dailyStatsDao: DailyStatsDao,
        gson: Gson
    ): DailyStatsRepository {
        return DailyStatsRepositoryImpl(dailyStatsDao, gson)
    }

    @Provides
    @Singleton
    fun provideReviewTaskRepository(
        reviewTaskDao: ReviewTaskDao
    ): ReviewTaskRepository {
        return ReviewTaskRepositoryImpl(reviewTaskDao)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.premiumDataStore
    }

    @Provides
    @Singleton
    fun providePremiumRepository(dataStore: DataStore<Preferences>): PremiumRepository {
        return PremiumRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
        database: ZenithDatabase,
        subjectGroupDao: SubjectGroupDao,
        taskDao: TaskDao,
        studySessionDao: StudySessionDao,
        reviewTaskDao: ReviewTaskDao,
        settingsDao: SettingsDao,
        dailyStatsDao: DailyStatsDao,
        gson: Gson
    ): BackupRepository {
        return BackupRepositoryImpl(
            context = context,
            database = database,
            subjectGroupDao = subjectGroupDao,
            taskDao = taskDao,
            studySessionDao = studySessionDao,
            reviewTaskDao = reviewTaskDao,
            settingsDao = settingsDao,
            dailyStatsDao = dailyStatsDao,
            gson = gson
        )
    }
}
