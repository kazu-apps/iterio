package com.iterio.app.di

import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.PomodoroSettings
import com.iterio.app.domain.model.SubscriptionStatus
import com.iterio.app.domain.model.Task
import com.iterio.app.domain.repository.*
import com.iterio.app.ui.premium.PremiumManager
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Singleton

/**
 * E2Eテスト用のテストモジュール
 * 本番のAppModuleをこのモジュールで置換
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {

    @Provides
    @Singleton
    fun provideTaskRepository(): TaskRepository = mockk(relaxed = true) {
        val testTask = Task(
            id = 1L,
            groupId = 1L,
            name = "テストタスク",
            workDurationMinutes = 25
        )
        coEvery { getTaskById(any()) } returns Result.Success(testTask)
        every { getTasksByGroup(any()) } returns flowOf(listOf(testTask))
        coEvery { getTasksForDate(any()) } returns Result.Success(listOf(testTask))
        coEvery { getTaskCountByDateRange(any(), any()) } returns Result.Success(emptyMap())
        every { observeTaskCountByDateRange(any(), any()) } returns flowOf(emptyMap())
        every { observeTasksForDate(any()) } returns flowOf(emptyList())
        every { getTodayScheduledTasks(any()) } returns flowOf(emptyList())
        every { getUpcomingDeadlineTasks(any(), any()) } returns flowOf(emptyList())
    }

    @Provides
    @Singleton
    fun provideSubjectGroupRepository(): SubjectGroupRepository = mockk(relaxed = true) {
        every { getAllGroups() } returns flowOf(emptyList())
        every { getUpcomingDeadlineGroups(any(), any()) } returns flowOf(emptyList())
    }

    @Provides
    @Singleton
    fun provideStudySessionRepository(): StudySessionRepository = mockk(relaxed = true) {
        coEvery { insertSession(any()) } returns Result.Success(1L)
        every { getSessionsForDay(any()) } returns flowOf(emptyList())
        coEvery { getTotalMinutesForDay(any()) } returns Result.Success(0)
        coEvery { getTotalCyclesForDay(any()) } returns Result.Success(0)
        coEvery { getSessionCount() } returns Result.Success(0)
    }

    @Provides
    @Singleton
    fun provideReviewTaskRepository(): ReviewTaskRepository = mockk(relaxed = true) {
        every { getPendingTasksForDate(any()) } returns flowOf(emptyList())
        every { getOverdueAndTodayTasks(any()) } returns flowOf(emptyList())
        every { getAllWithDetails() } returns flowOf(emptyList())
        coEvery { getPendingTaskCountForDate(any()) } returns Result.Success(0)
        coEvery { getTaskCountByDateRange(any(), any()) } returns Result.Success(emptyMap())
        every { observeTaskCountByDateRange(any(), any()) } returns flowOf(emptyMap())
        coEvery { getTotalCount() } returns Result.Success(0)
        coEvery { getIncompleteCount() } returns Result.Success(0)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(): SettingsRepository = mockk(relaxed = true) {
        coEvery { getPomodoroSettings() } returns Result.Success(PomodoroSettings())
        every { getPomodoroSettingsFlow() } returns flowOf(PomodoroSettings())
        coEvery { getAllowedApps() } returns Result.Success(emptyList())
        every { getAllowedAppsFlow() } returns flowOf(emptyList())
        coEvery { getLanguage() } returns Result.Success("ja")
        every { getLanguageFlow() } returns flowOf("ja")
        coEvery { getBgmTrackId() } returns Result.Success(null)
        every { getBgmTrackIdFlow() } returns flowOf(null)
        coEvery { getBgmVolume() } returns Result.Success(0.5f)
        every { getBgmVolumeFlow() } returns flowOf(0.5f)
        coEvery { getBgmAutoPlay() } returns Result.Success(true)
        every { getBgmAutoPlayFlow() } returns flowOf(true)
    }

    @Provides
    @Singleton
    fun provideDailyStatsRepository(): DailyStatsRepository = mockk(relaxed = true) {
        coEvery { getByDate(any()) } returns Result.Success(null)
        every { getByDateFlow(any()) } returns flowOf(null)
        coEvery { getCurrentStreak() } returns Result.Success(0)
        coEvery { getMaxStreak() } returns Result.Success(0)
        coEvery { getTotalMinutesBetweenDates(any(), any()) } returns Result.Success(0)
        val today = LocalDate.now()
        val weeklyData = listOf(
            DayStats("月", today.minusDays(6), 30),
            DayStats("火", today.minusDays(5), 0),
            DayStats("水", today.minusDays(4), 45),
            DayStats("木", today.minusDays(3), 0),
            DayStats("金", today.minusDays(2), 20),
            DayStats("土", today.minusDays(1), 60),
            DayStats("日", today, 0)
        )
        coEvery { getWeeklyData(any()) } returns Result.Success(weeklyData)
    }

    @Provides
    @Singleton
    fun providePremiumRepository(): PremiumRepository = mockk(relaxed = true) {
        val status = SubscriptionStatus(hasSeenTrialOffer = true, isTrialUsed = true)
        val subscriptionFlow = MutableStateFlow(status)
        every { subscriptionStatus } returns subscriptionFlow
        coEvery { getSubscriptionStatus() } returns Result.Success(status)
        coEvery { canAccessFeature(any()) } returns Result.Success(false)
    }

    @Provides
    @Singleton
    fun provideBackupRepository(): BackupRepository = mockk(relaxed = true)

    @Provides
    @Singleton
    fun providePremiumManager(
        premiumRepository: PremiumRepository
    ): PremiumManager = PremiumManager(premiumRepository)
}
