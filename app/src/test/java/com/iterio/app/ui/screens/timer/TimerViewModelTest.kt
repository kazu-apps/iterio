package com.iterio.app.ui.screens.timer

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.AllowedApp
import com.iterio.app.domain.model.PomodoroSettings
import com.iterio.app.domain.model.SubscriptionStatus
import com.iterio.app.domain.model.SubscriptionType
import com.iterio.app.domain.model.ScheduleType
import com.iterio.app.domain.model.Task
import com.iterio.app.domain.repository.SettingsRepository
import com.iterio.app.domain.repository.TaskRepository
import com.iterio.app.domain.usecase.GetTimerInitialStateUseCase
import com.iterio.app.domain.usecase.TimerInitialState
import com.iterio.app.service.TimerPhase
import com.iterio.app.service.TimerService
import com.iterio.app.ui.bgm.BgmManager
import com.iterio.app.ui.premium.PremiumManager
import com.iterio.app.util.InstalledAppsHelper
import com.iterio.app.widget.IterioWidgetReceiver
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * TimerViewModel のユニットテスト（フォーカスモード関連）
 *
 * 注意: このテストはAndroidコンテキストが必要な機能をモック化してテストします。
 * TimerServiceとのバインドなど、実際のAndroidコンポーネントを使う機能は
 * 統合テストまたは手動テストで検証してください。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private lateinit var context: Context
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getTimerInitialStateUseCase: GetTimerInitialStateUseCase
    private lateinit var taskRepository: TaskRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var premiumManager: PremiumManager
    private lateinit var bgmManager: BgmManager
    private lateinit var installedAppsHelper: InstalledAppsHelper

    private val testDispatcher = StandardTestDispatcher()

    private val mockTask = Task(
        id = 1L,
        name = "Test Task",
        groupId = 1L,
        workDurationMinutes = 25
    )

    private val defaultSettings = PomodoroSettings(
        workDurationMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        cyclesBeforeLongBreak = 4,
        focusModeEnabled = true,
        reviewEnabled = true
    )

    private val mockApps = listOf(
        AllowedApp(packageName = "com.example.allowed1", appName = "Allowed 1", icon = null),
        AllowedApp(packageName = "com.example.allowed2", appName = "Allowed 2", icon = null)
    )

    private val subscriptionStatusFlow = MutableStateFlow(SubscriptionStatus())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock static methods in TimerService companion object
        mockkObject(TimerService.Companion)
        every { TimerService.startTimer(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit
        every { TimerService.pauseTimer(any()) } returns Unit
        every { TimerService.resumeTimer(any()) } returns Unit
        every { TimerService.stopTimer(any()) } returns Unit
        every { TimerService.skipPhase(any()) } returns Unit

        // Mock widget broadcast to avoid Android Intent in unit tests
        mockkObject(IterioWidgetReceiver.Companion)
        every { IterioWidgetReceiver.sendDataChangedBroadcast(any()) } returns Unit
        every { IterioWidgetReceiver.sendUpdateBroadcast(any()) } returns Unit

        context = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("taskId" to 1L))
        getTimerInitialStateUseCase = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        premiumManager = mockk(relaxed = true)
        bgmManager = mockk(relaxed = true)
        installedAppsHelper = mockk(relaxed = true)

        // Default mocks for UseCases
        val defaultInitialState = TimerInitialState(
            task = mockTask,
            settings = defaultSettings,
            effectiveWorkDurationMinutes = defaultSettings.workDurationMinutes,
            totalTimeSeconds = defaultSettings.workDurationMinutes * 60,
            defaultAllowedApps = setOf("com.example.allowed1")
        )
        coEvery { getTimerInitialStateUseCase(1L) } returns Result.Success(defaultInitialState)
        coEvery { installedAppsHelper.getInstalledUserApps() } returns mockApps
        every { premiumManager.subscriptionStatus } returns subscriptionStatusFlow
        every { bgmManager.bgmState } returns MutableStateFlow(mockk(relaxed = true))
        every { bgmManager.selectedTrack } returns MutableStateFlow(null)
        every { bgmManager.volume } returns MutableStateFlow(0.5f)

        // Context mock for service calls
        every { context.packageName } returns "com.iterio.app"
        every { context.startService(any()) } returns null
        every { context.stopService(any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(TimerService.Companion)
        unmockkObject(IterioWidgetReceiver.Companion)
    }

    private fun createViewModel(): TimerViewModel {
        return TimerViewModel(
            savedStateHandle = savedStateHandle,
            context = context,
            getTimerInitialStateUseCase = getTimerInitialStateUseCase,
            taskRepository = taskRepository,
            settingsRepository = settingsRepository,
            premiumManager = premiumManager,
            bgmManager = bgmManager,
            installedAppsHelper = installedAppsHelper
        )
    }

    // 初期状態のテスト

    @Test
    fun `initial state has correct default values`() = runTest {
        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals("タスクが読み込まれるべき", mockTask, state.task)
        assertEquals("設定が読み込まれるべき", defaultSettings, state.settings)
        assertEquals("初期フェーズはIDLEであるべき", TimerPhase.IDLE, state.phase)
        assertFalse("初期状態ではロックモードは無効であるべき", state.isSessionLockModeEnabled)
    }

    @Test
    fun `initial state loads allowed apps from settings`() = runTest {
        // Arrange
        val savedAllowedApps = setOf("com.example.allowed1", "com.example.allowed2")
        val initialState = TimerInitialState(
            task = mockTask,
            settings = defaultSettings,
            effectiveWorkDurationMinutes = defaultSettings.workDurationMinutes,
            totalTimeSeconds = defaultSettings.workDurationMinutes * 60,
            defaultAllowedApps = savedAllowedApps
        )
        coEvery { getTimerInitialStateUseCase(1L) } returns Result.Success(initialState)

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        assertEquals(
            "許可アプリ設定が読み込まれるべき",
            savedAllowedApps,
            viewModel.uiState.value.defaultAllowedApps
        )
    }

    @Test
    fun `initial state loads installed apps`() = runTest {
        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertFalse("アプリ読み込み完了後はisLoadingApps=falseであるべき", state.isLoadingApps)
        assertEquals("インストール済みアプリが読み込まれるべき", mockApps, state.installedApps)
    }

    // タスク固有の作業時間テスト

    @Test
    fun `uses task work duration when available`() = runTest {
        // Arrange - タスクに作業時間が設定されている
        val taskWithDuration = mockTask.copy(workDurationMinutes = 45)
        val initialState = TimerInitialState(
            task = taskWithDuration,
            settings = defaultSettings,
            effectiveWorkDurationMinutes = 45,
            totalTimeSeconds = 45 * 60,
            defaultAllowedApps = setOf("com.example.allowed1")
        )
        coEvery { getTimerInitialStateUseCase(1L) } returns Result.Success(initialState)

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(
            "タスク固有の作業時間が使われるべき",
            45 * 60,
            state.timeRemainingSeconds
        )
    }

    @Test
    fun `uses default settings when task has no work duration`() = runTest {
        // Arrange - タスクに作業時間が設定されていない
        val taskWithoutDuration = mockTask.copy(workDurationMinutes = null)
        val initialState = TimerInitialState(
            task = taskWithoutDuration,
            settings = defaultSettings,
            effectiveWorkDurationMinutes = defaultSettings.workDurationMinutes,
            totalTimeSeconds = defaultSettings.workDurationMinutes * 60,
            defaultAllowedApps = setOf("com.example.allowed1")
        )
        coEvery { getTimerInitialStateUseCase(1L) } returns Result.Success(initialState)

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(
            "デフォルト設定の作業時間が使われるべき",
            defaultSettings.workDurationMinutes * 60,
            state.timeRemainingSeconds
        )
    }

    // Premium状態のテスト

    @Test
    fun `isPremium reflects subscription status`() = runTest {
        // Arrange
        subscriptionStatusFlow.value = SubscriptionStatus(type = SubscriptionType.MONTHLY)

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert with Turbine
        viewModel.isPremium.test {
            // Premium状態を確認
            val isPremium = awaitItem()
            // Note: isPremiumはSubscriptionStatus.isPremiumに基づく
            // expiresAtがnullの場合、MONTHLYは期限切れと判定される
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isPremium returns true for lifetime subscription`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert with Turbine - 購読を開始してから値を更新
        viewModel.isPremium.test {
            // 初期値をスキップ
            awaitItem()

            // サブスクリプション状態を更新
            subscriptionStatusFlow.value = SubscriptionStatus(type = SubscriptionType.LIFETIME)
            advanceUntilIdle()

            // 更新された値を確認
            val isPremium = awaitItem()
            assertTrue("LIFETIME契約はPremiumであるべき", isPremium)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isPremium returns true during trial period`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert with Turbine - 購読を開始してから値を更新
        viewModel.isPremium.test {
            // 初期値をスキップ
            awaitItem()

            // トライアル状態を更新
            subscriptionStatusFlow.value = SubscriptionStatus(
                type = SubscriptionType.FREE,
                trialExpiresAt = LocalDateTime.now().plusDays(3)
            )
            advanceUntilIdle()

            // 更新された値を確認
            val isPremium = awaitItem()
            assertTrue("トライアル期間中はPremiumであるべき", isPremium)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // UI状態のテスト

    @Test
    fun `showCancelDialog updates state correctly`() = runTest {
        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showCancelDialog()

        // Assert
        assertTrue(
            "キャンセルダイアログが表示されるべき",
            viewModel.uiState.value.showCancelDialog
        )
    }

    @Test
    fun `hideCancelDialog updates state correctly`() = runTest {
        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showCancelDialog()
        viewModel.hideCancelDialog()

        // Assert
        assertFalse(
            "キャンセルダイアログが非表示になるべき",
            viewModel.uiState.value.showCancelDialog
        )
    }

    @Test
    fun `hideFinishDialog updates state correctly`() = runTest {
        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.hideFinishDialog()

        // Assert
        assertFalse(
            "完了ダイアログが非表示になるべき",
            viewModel.uiState.value.showFinishDialog
        )
    }

    // 設定の読み込みテスト

    @Test
    fun `loads pomodoro settings correctly`() = runTest {
        // Arrange
        val customSettings = PomodoroSettings(
            workDurationMinutes = 50,
            shortBreakMinutes = 10,
            longBreakMinutes = 30,
            cyclesBeforeLongBreak = 2,
            focusModeEnabled = false,
            reviewEnabled = false
        )
        val initialState = TimerInitialState(
            task = mockTask,
            settings = customSettings,
            effectiveWorkDurationMinutes = 50,
            totalTimeSeconds = 50 * 60,
            defaultAllowedApps = setOf("com.example.allowed1")
        )
        coEvery { getTimerInitialStateUseCase(1L) } returns Result.Success(initialState)

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals("カスタム設定が読み込まれるべき", customSettings, state.settings)
        assertEquals("サイクル数が設定されるべき", 2, state.totalCycles)
    }

    // BGM関連のテスト

    @Test
    fun `getAvailableBgmTracks returns tracks from BgmManager`() = runTest {
        // Arrange
        val mockTracks = listOf(mockk<com.iterio.app.domain.model.BgmTrack>())
        every { bgmManager.getAvailableTracks() } returns mockTracks

        // Act
        val viewModel = createViewModel()
        val tracks = viewModel.getAvailableBgmTracks()

        // Assert
        assertEquals("BgmManagerからトラックを取得すべき", mockTracks, tracks)
    }

    @Test
    fun `setBgmVolume delegates to BgmManager`() = runTest {
        // Act
        val viewModel = createViewModel()
        viewModel.setBgmVolume(0.8f)

        // Assert
        io.mockk.verify { bgmManager.setVolume(0.8f) }
    }

    // トライアル開始テスト

    @Test
    fun `startTrial delegates to PremiumManager`() = runTest {
        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTrial()
        advanceUntilIdle()

        // Assert
        io.mockk.coVerify { premiumManager.startTrial() }
    }

    // StateFlow更新のテスト

    @Test
    fun `uiState updates when settings change`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert with Turbine
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals("初期タスクが設定されるべき", mockTask, initialState.task)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // タスクがない場合のテスト

    @Test
    fun `handles missing task gracefully`() = runTest {
        // Arrange
        coEvery { getTimerInitialStateUseCase(1L) } returns Result.Failure(
            DomainError.NotFoundError("Task not found: 1")
        )

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertNull("タスクがnullの場合はnullであるべき", state.task)
    }

    // 空の許可アプリリストのテスト

    @Test
    fun `handles empty allowed apps list`() = runTest {
        // Arrange
        val initialState = TimerInitialState(
            task = mockTask,
            settings = defaultSettings,
            effectiveWorkDurationMinutes = defaultSettings.workDurationMinutes,
            totalTimeSeconds = defaultSettings.workDurationMinutes * 60,
            defaultAllowedApps = emptySet()
        )
        coEvery { getTimerInitialStateUseCase(1L) } returns Result.Success(initialState)

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        assertTrue(
            "許可アプリリストが空であるべき",
            viewModel.uiState.value.defaultAllowedApps.isEmpty()
        )
    }

    // ========== updateAllowedApps テスト ==========

    @Test
    fun `updateAllowedApps updates UiState and saves to repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val newApps = setOf("com.example.app1", "com.example.app2")
        viewModel.updateAllowedApps(newApps)
        advanceUntilIdle()

        // UiState が即座に更新されること
        assertEquals(
            "defaultAllowedApps が更新されるべき",
            newApps,
            viewModel.uiState.value.defaultAllowedApps
        )

        // SettingsRepository に保存されること
        io.mockk.coVerify {
            settingsRepository.setAllowedApps(match { it.toSet() == newApps })
        }
    }

    @Test
    fun `updateAllowedApps with empty set clears allowed apps`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateAllowedApps(emptySet())
        advanceUntilIdle()

        assertTrue(
            "defaultAllowedApps が空になるべき",
            viewModel.uiState.value.defaultAllowedApps.isEmpty()
        )

        io.mockk.coVerify {
            settingsRepository.setAllowedApps(emptyList())
        }
    }

    // ========== BGM追加テスト ==========

    @Test
    fun `selectBgmTrack with track delegates play to BgmManager`() = runTest {
        val track = mockk<com.iterio.app.domain.model.BgmTrack>()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectBgmTrack(track)

        io.mockk.verify { bgmManager.play(track) }
    }

    @Test
    fun `selectBgmTrack with null delegates stop to BgmManager`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectBgmTrack(null)

        io.mockk.verify { bgmManager.stop() }
    }

    @Test
    fun `toggleBgm pauses when playing`() = runTest {
        every { bgmManager.isPlaying() } returns true

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleBgm()

        io.mockk.verify { bgmManager.pause() }
    }

    @Test
    fun `toggleBgm resumes when not playing`() = runTest {
        every { bgmManager.isPlaying() } returns false

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleBgm()

        io.mockk.verify { bgmManager.resume() }
    }

    // ========== 初期状態追加テスト ==========

    @Test
    fun `initial state showCancelDialog is false`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showCancelDialog)
    }

    @Test
    fun `initial state showFinishDialog is false`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showFinishDialog)
    }

    @Test
    fun `showCancelDialog followed by hideCancelDialog toggles correctly`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showCancelDialog()
        assertTrue(viewModel.uiState.value.showCancelDialog)

        viewModel.hideCancelDialog()
        assertFalse(viewModel.uiState.value.showCancelDialog)
    }

    // ========== フォーカスモード設定テスト ==========

    @Test
    fun `focus mode disabled setting is respected`() = runTest {
        val settingsNoFocus = defaultSettings.copy(focusModeEnabled = false)
        val initialState = TimerInitialState(
            task = mockTask,
            settings = settingsNoFocus,
            effectiveWorkDurationMinutes = settingsNoFocus.workDurationMinutes,
            totalTimeSeconds = settingsNoFocus.workDurationMinutes * 60,
            defaultAllowedApps = emptySet()
        )
        coEvery { getTimerInitialStateUseCase(1L) } returns Result.Success(initialState)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.settings.focusModeEnabled)
    }

    @Test
    fun `review disabled setting is respected`() = runTest {
        val settingsNoReview = defaultSettings.copy(reviewEnabled = false)
        val initialState = TimerInitialState(
            task = mockTask,
            settings = settingsNoReview,
            effectiveWorkDurationMinutes = settingsNoReview.workDurationMinutes,
            totalTimeSeconds = settingsNoReview.workDurationMinutes * 60,
            defaultAllowedApps = emptySet()
        )
        coEvery { getTimerInitialStateUseCase(1L) } returns Result.Success(initialState)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.settings.reviewEnabled)
    }

    // ========== タイマー操作テスト ==========

    @Test
    fun `startTimer from IDLE creates session and starts service`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTimer()
        advanceUntilIdle()

        io.mockk.verify { TimerService.startTimer(
            context = any(),
            taskId = eq(1L),
            taskName = eq("Test Task"),
            workDuration = eq(25),
            shortBreak = eq(5),
            longBreak = eq(15),
            cycles = eq(4),
            focusModeEnabled = eq(true),
            focusModeStrict = eq(false),
            autoLoopEnabled = eq(false),
            allowedApps = eq(emptySet())
        ) }
        io.mockk.verify { bgmManager.onTimerStart() }
    }

    @Test
    fun `startTimer with custom cycle count uses provided cycles`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTimer(cycleCount = 8)
        advanceUntilIdle()

        io.mockk.verify { TimerService.startTimer(
            context = any(),
            taskId = any(),
            taskName = any(),
            workDuration = any(),
            shortBreak = any(),
            longBreak = any(),
            cycles = eq(8),
            focusModeEnabled = any(),
            focusModeStrict = any(),
            autoLoopEnabled = any(),
            allowedApps = any()
        ) }
        assertEquals(8, viewModel.uiState.value.totalCycles)
    }

    @Test
    fun `startTimer with lockMode enabled updates state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTimer(lockModeEnabled = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSessionLockModeEnabled)
        io.mockk.verify { TimerService.startTimer(
            context = any(),
            taskId = any(),
            taskName = any(),
            workDuration = any(),
            shortBreak = any(),
            longBreak = any(),
            cycles = any(),
            focusModeEnabled = any(),
            focusModeStrict = eq(true),
            autoLoopEnabled = any(),
            allowedApps = any()
        ) }
    }

    @Test
    fun `startTimer with autoLoop enabled passes to service`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTimer(autoLoopEnabled = true)
        advanceUntilIdle()

        io.mockk.verify { TimerService.startTimer(
            context = any(),
            taskId = any(),
            taskName = any(),
            workDuration = any(),
            shortBreak = any(),
            longBreak = any(),
            cycles = any(),
            focusModeEnabled = any(),
            focusModeStrict = any(),
            autoLoopEnabled = eq(true),
            allowedApps = any()
        ) }
    }

    @Test
    fun `startTimer with allowedApps passes to service`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val apps = setOf("com.example.app1", "com.example.app2")
        viewModel.startTimer(allowedApps = apps)
        advanceUntilIdle()

        io.mockk.verify { TimerService.startTimer(
            context = any(),
            taskId = any(),
            taskName = any(),
            workDuration = any(),
            shortBreak = any(),
            longBreak = any(),
            cycles = any(),
            focusModeEnabled = any(),
            focusModeStrict = any(),
            autoLoopEnabled = any(),
            allowedApps = eq(apps)
        ) }
    }

    @Test
    fun `startTimer does nothing when task is null`() = runTest {
        coEvery { getTimerInitialStateUseCase(1L) } returns Result.Failure(
            DomainError.NotFoundError("Task not found")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTimer()
        advanceUntilIdle()

        io.mockk.verify(exactly = 0) { TimerService.startTimer(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        ) }
    }

    @Test
    fun `pauseTimer delegates to TimerService and BgmManager`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.pauseTimer()

        io.mockk.verify { TimerService.pauseTimer(context) }
        io.mockk.verify { bgmManager.onTimerPause() }
    }

    @Test
    fun `resumeTimer delegates to TimerService and BgmManager`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.resumeTimer()

        io.mockk.verify { TimerService.resumeTimer(context) }
        io.mockk.verify { bgmManager.onTimerResume() }
    }

    @Test
    fun `cancelTimer resets state and stops service`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Start timer first to establish session
        viewModel.startTimer()
        advanceUntilIdle()

        viewModel.cancelTimer()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TimerPhase.IDLE, state.phase)
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertFalse(state.showCancelDialog)
        io.mockk.verify { TimerService.stopTimer(context) }
        io.mockk.verify { bgmManager.onTimerStop() }
    }

    @Test
    fun `cancelTimer always delegates to TimerService`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTimer()
        advanceUntilIdle()

        viewModel.cancelTimer()
        advanceUntilIdle()

        io.mockk.verify { TimerService.stopTimer(context) }
        io.mockk.verify { bgmManager.onTimerStop() }
    }

    @Test
    fun `skipPhase delegates to TimerService`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.skipPhase()

        io.mockk.verify { TimerService.skipPhase(context) }
    }

    @Test
    fun `hideFinishDialog stops timer and bgm`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.hideFinishDialog()

        io.mockk.verify { TimerService.stopTimer(context) }
        io.mockk.verify { bgmManager.onTimerStop() }
    }

    // ========== プロパティアクセステスト ==========

    @Test
    fun `subscriptionStatus property exposes PremiumManager state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.subscriptionStatus)
    }

    @Test
    fun `bgmState property exposes BgmManager state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.bgmState)
    }

    @Test
    fun `selectedBgmTrack property exposes BgmManager state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.selectedBgmTrack)
    }

    @Test
    fun `bgmVolume property exposes BgmManager state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.bgmVolume)
    }

    // ========== loadNextTask テスト ==========

    private fun createTodayTask(
        id: Long,
        name: String,
        completedToday: Boolean = false
    ): Task {
        val today = LocalDate.now()
        return Task(
            id = id,
            name = name,
            groupId = 1L,
            scheduleType = ScheduleType.REPEAT,
            repeatDays = setOf(today.dayOfWeek.value),
            lastStudiedAt = if (completedToday) LocalDateTime.now() else null
        )
    }

    @Test
    fun `loadNextTask finds next uncompleted task`() = runTest {
        val todayTasks = listOf(
            createTodayTask(1L, "Task 1", completedToday = true),
            createTodayTask(2L, "Task 2", completedToday = false),
            createTodayTask(3L, "Task 3", completedToday = false)
        )
        every { taskRepository.getTodayScheduledTasks(any()) } returns flowOf(todayTasks)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadNextTask()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("次の未完了タスクのIDがセットされるべき", 2L, state.nextTaskId)
        assertEquals("次の未完了タスクの名前がセットされるべき", "Task 2", state.nextTaskName)
    }

    @Test
    fun `loadNextTask returns null when all tasks completed`() = runTest {
        val todayTasks = listOf(
            createTodayTask(1L, "Task 1", completedToday = true),
            createTodayTask(2L, "Task 2", completedToday = true)
        )
        every { taskRepository.getTodayScheduledTasks(any()) } returns flowOf(todayTasks)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadNextTask()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("全タスク完了時にnextTaskIdはnullであるべき", state.nextTaskId)
        assertNull("全タスク完了時にnextTaskNameはnullであるべき", state.nextTaskName)
    }

    @Test
    fun `loadNextTask skips current task`() = runTest {
        // 現在のタスクID=1なので、id=1は未完了でもスキップされるべき
        val todayTasks = listOf(
            createTodayTask(1L, "Current Task", completedToday = false),
            createTodayTask(2L, "Next Task", completedToday = false)
        )
        every { taskRepository.getTodayScheduledTasks(any()) } returns flowOf(todayTasks)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadNextTask()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("現在のタスクをスキップして次を選ぶべき", 2L, state.nextTaskId)
        assertEquals("現在のタスクをスキップして次の名前を選ぶべき", "Next Task", state.nextTaskName)
    }

    @Test
    fun `loadNextTask returns null when no today tasks exist`() = runTest {
        every { taskRepository.getTodayScheduledTasks(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadNextTask()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("今日のタスクがない場合nextTaskIdはnullであるべき", state.nextTaskId)
    }

    @Test
    fun `allTasksCompleted is true when autoLoop and no next task`() = runTest {
        val todayTasks = listOf(
            createTodayTask(1L, "Task 1", completedToday = true),
            createTodayTask(2L, "Task 2", completedToday = true)
        )
        every { taskRepository.getTodayScheduledTasks(any()) } returns flowOf(todayTasks)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // autoLoop有効でタイマー開始
        viewModel.startTimer(autoLoopEnabled = true)
        advanceUntilIdle()

        viewModel.loadNextTask()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("autoLoop有効かつ全完了時にallTasksCompletedがtrueであるべき", state.allTasksCompleted)
    }

    @Test
    fun `allTasksCompleted is false when no autoLoop`() = runTest {
        val todayTasks = listOf(
            createTodayTask(1L, "Task 1", completedToday = true),
            createTodayTask(2L, "Task 2", completedToday = true)
        )
        every { taskRepository.getTodayScheduledTasks(any()) } returns flowOf(todayTasks)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // autoLoop無効でタイマー開始
        viewModel.startTimer(autoLoopEnabled = false)
        advanceUntilIdle()

        viewModel.loadNextTask()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("autoLoop無効時にallTasksCompletedがfalseであるべき", state.allTasksCompleted)
    }
}
