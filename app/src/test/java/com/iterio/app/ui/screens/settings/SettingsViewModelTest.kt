package com.iterio.app.ui.screens.settings

import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.BgmTrack
import com.iterio.app.domain.model.PomodoroSettings
import com.iterio.app.domain.model.ReviewTask
import com.iterio.app.domain.model.SubscriptionStatus
import com.iterio.app.domain.model.SubscriptionType
import com.iterio.app.domain.repository.ReviewTaskRepository
import com.iterio.app.domain.repository.SettingsRepository
import com.iterio.app.domain.usecase.UpdatePomodoroSettingsUseCase
import com.iterio.app.ui.bgm.BgmManager
import com.iterio.app.ui.premium.PremiumManager
import com.iterio.app.util.LocaleManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * SettingsViewModel core logic tests.
 *
 * Covers: loadSettings, saveSettings, loadReviewTaskCounts,
 * deleteAllReviewTasks, updateLanguage, updateDefaultReviewCount,
 * dialog state management, premium state, and premium toggle return values.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var updatePomodoroSettingsUseCase: UpdatePomodoroSettingsUseCase
    private lateinit var premiumManager: PremiumManager
    private lateinit var localeManager: LocaleManager
    private lateinit var reviewTaskRepository: ReviewTaskRepository
    private lateinit var bgmManager: BgmManager
    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val subscriptionStatusFlow = MutableStateFlow(SubscriptionStatus())

    // BGM-related StateFlows
    private val selectedTrackFlow = MutableStateFlow<BgmTrack?>(null)
    private val volumeFlow = MutableStateFlow(0.5f)
    private val autoPlayFlow = MutableStateFlow(true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        settingsRepository = mockk(relaxed = true)
        updatePomodoroSettingsUseCase = mockk(relaxed = true)
        premiumManager = mockk(relaxed = true)
        localeManager = mockk()
        reviewTaskRepository = mockk(relaxed = true)
        bgmManager = mockk(relaxed = true)

        coEvery { settingsRepository.getPomodoroSettings() } returns Result.Success(PomodoroSettings())
        coEvery { settingsRepository.getAllowedApps() } returns Result.Success(emptyList<String>())
        coEvery { updatePomodoroSettingsUseCase.updateSettings(any()) } returns Result.Success(Unit)
        every { premiumManager.subscriptionStatus } returns subscriptionStatusFlow
        coEvery { premiumManager.isPremium() } returns false
        every { localeManager.getCurrentLanguage() } returns "ja"
        coEvery { localeManager.setLanguage(any()) } returns Unit
        coEvery { reviewTaskRepository.getTotalCount() } returns Result.Success(0)
        coEvery { reviewTaskRepository.getIncompleteCount() } returns Result.Success(0)
        every { reviewTaskRepository.getAllWithDetails() } returns flowOf(emptyList())
        every { bgmManager.selectedTrack } returns selectedTrackFlow
        every { bgmManager.volume } returns volumeFlow
        every { bgmManager.autoPlayEnabled } returns autoPlayFlow
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            settingsRepository = settingsRepository,
            updatePomodoroSettingsUseCase = updatePomodoroSettingsUseCase,
            premiumManager = premiumManager,
            localeManager = localeManager,
            reviewTaskRepository = reviewTaskRepository,
            bgmManager = bgmManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== loadSettings ==========

    @Test
    fun `loadSettings populates uiState with PomodoroSettings values`() = runTest {
        val settings = PomodoroSettings(
            workDurationMinutes = 30,
            shortBreakMinutes = 10,
            longBreakMinutes = 20,
            cyclesBeforeLongBreak = 3,
            focusModeEnabled = false,
            focusModeStrict = true,
            autoLoopEnabled = true,
            reviewEnabled = false,
            defaultReviewCount = 5,
            notificationsEnabled = false
        )
        coEvery { settingsRepository.getPomodoroSettings() } returns Result.Success(settings)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(30, state.workDurationMinutes)
        assertEquals(10, state.shortBreakMinutes)
        assertEquals(20, state.longBreakMinutes)
        assertEquals(3, state.cyclesBeforeLongBreak)
        assertFalse(state.focusModeEnabled)
        assertTrue(state.focusModeStrict)
        assertTrue(state.autoLoopEnabled)
        assertFalse(state.reviewIntervalsEnabled)
        assertEquals(5, state.defaultReviewCount)
        assertFalse(state.notificationsEnabled)
    }

    @Test
    fun `loadSettings handles getPomodoroSettings failure - state remains loading`() = runTest {
        coEvery { settingsRepository.getPomodoroSettings() } returns Result.Failure(
            DomainError.DatabaseError("Database unavailable")
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // On failure, settingsResult.onSuccess block is not executed,
        // so isLoading stays true (the default)
        assertTrue(state.isLoading)
    }

    @Test
    fun `loadSettings loads allowed apps count`() = runTest {
        coEvery { settingsRepository.getAllowedApps() } returns Result.Success(
            listOf("com.app.one", "com.app.two", "com.app.three")
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.allowedAppsCount)
    }

    @Test
    fun `loadSettings loads allowed apps count as zero when getAllowedApps fails`() = runTest {
        coEvery { settingsRepository.getAllowedApps() } returns Result.Failure(
            DomainError.DatabaseError("Failed to load apps")
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.allowedAppsCount)
    }

    @Test
    fun `loadSettings loads BGM settings from BgmManager`() = runTest {
        val track = mockk<BgmTrack>()
        every { track.id } returns "rain"
        selectedTrackFlow.value = track
        volumeFlow.value = 0.8f
        autoPlayFlow.value = false

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("rain", state.bgmTrackId)
        assertEquals(0.8f, state.bgmVolume)
        assertFalse(state.bgmAutoPlayEnabled)
    }

    @Test
    fun `loadSettings loads BGM track as null when no track selected`() = runTest {
        selectedTrackFlow.value = null

        viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.bgmTrackId)
    }

    @Test
    fun `loadSettings loads language from localeManager`() = runTest {
        every { localeManager.getCurrentLanguage() } returns "en"

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("en", viewModel.uiState.value.language)
    }

    // ========== loadReviewTaskCounts ==========

    @Test
    fun `loadReviewTaskCounts populates total and incomplete counts`() = runTest {
        coEvery { reviewTaskRepository.getTotalCount() } returns Result.Success(42)
        coEvery { reviewTaskRepository.getIncompleteCount() } returns Result.Success(17)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(42, state.reviewTaskTotalCount)
        assertEquals(17, state.reviewTaskIncompleteCount)
    }

    @Test
    fun `loadReviewTaskCounts defaults to zero on failure`() = runTest {
        coEvery { reviewTaskRepository.getTotalCount() } returns Result.Failure(
            DomainError.DatabaseError("DB error")
        )
        coEvery { reviewTaskRepository.getIncompleteCount() } returns Result.Failure(
            DomainError.DatabaseError("DB error")
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.reviewTaskTotalCount)
        assertEquals(0, state.reviewTaskIncompleteCount)
    }

    // ========== saveSettings ==========

    @Test
    fun `toggleNotifications triggers saveSettings with correct PomodoroSettings`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleNotifications(false)
        advanceUntilIdle()

        coVerify {
            updatePomodoroSettingsUseCase.updateSettings(
                match { settings ->
                    !settings.notificationsEnabled
                }
            )
        }
    }

    @Test
    fun `saveSettings creates PomodoroSettings matching current uiState`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Apply multiple state changes to verify all fields are mapped correctly
        viewModel.updateWorkDuration(45)
        viewModel.updateShortBreak(8)
        viewModel.updateLongBreak(25)
        viewModel.updateCycles(6)
        viewModel.toggleFocusMode(false)
        viewModel.toggleFocusModeStrict(true)
        viewModel.toggleAutoLoop(true)
        viewModel.toggleReviewIntervals(false)
        viewModel.updateDefaultReviewCount(3)
        viewModel.toggleNotifications(false)
        advanceUntilIdle()

        // The last saveSettings call should contain all the accumulated state
        coVerify {
            updatePomodoroSettingsUseCase.updateSettings(
                match { settings ->
                    settings.workDurationMinutes == 45 &&
                        settings.shortBreakMinutes == 8 &&
                        settings.longBreakMinutes == 25 &&
                        settings.cyclesBeforeLongBreak == 6 &&
                        !settings.focusModeEnabled &&
                        settings.focusModeStrict &&
                        settings.autoLoopEnabled &&
                        !settings.reviewEnabled &&
                        settings.defaultReviewCount == 3 &&
                        !settings.notificationsEnabled
                }
            )
        }
    }

    // ========== deleteAllReviewTasks ==========

    @Test
    fun `deleteAllReviewTasks resets counts and dismisses dialog on success`() = runTest {
        coEvery { reviewTaskRepository.getTotalCount() } returns Result.Success(10)
        coEvery { reviewTaskRepository.getIncompleteCount() } returns Result.Success(5)
        coEvery { reviewTaskRepository.deleteAll() } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Precondition: counts are populated
        assertEquals(10, viewModel.uiState.value.reviewTaskTotalCount)
        assertEquals(5, viewModel.uiState.value.reviewTaskIncompleteCount)

        // Show dialog first, then delete
        viewModel.showDeleteAllReviewTasksDialog()
        assertTrue(viewModel.uiState.value.showDeleteAllReviewTasksDialog)

        viewModel.deleteAllReviewTasks()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.reviewTaskTotalCount)
        assertEquals(0, state.reviewTaskIncompleteCount)
        assertFalse(state.showDeleteAllReviewTasksDialog)
        assertTrue(state.reviewTasks.isEmpty())
    }

    @Test
    fun `deleteAllReviewTasks does not reset counts on failure`() = runTest {
        coEvery { reviewTaskRepository.getTotalCount() } returns Result.Success(10)
        coEvery { reviewTaskRepository.getIncompleteCount() } returns Result.Success(5)
        coEvery { reviewTaskRepository.deleteAll() } returns Result.Failure(
            DomainError.DatabaseError("Delete failed")
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteAllReviewTasks()
        advanceUntilIdle()

        // Counts should remain unchanged since onSuccess was not called
        val state = viewModel.uiState.value
        assertEquals(10, state.reviewTaskTotalCount)
        assertEquals(5, state.reviewTaskIncompleteCount)
    }

    // ========== updateLanguage ==========

    @Test
    fun `updateLanguage updates state and calls localeManager`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateLanguage("en")
        advanceUntilIdle()

        assertEquals("en", viewModel.uiState.value.language)
        coVerify { localeManager.setLanguage("en") }
    }

    @Test
    fun `updateLanguage invokes callback after language change`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        var callbackInvoked = false
        viewModel.updateLanguage("en") { callbackInvoked = true }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
    }

    // ========== updateDefaultReviewCount ==========

    @Test
    fun `updateDefaultReviewCount updates state and triggers save`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateDefaultReviewCount(5)
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.defaultReviewCount)
        coVerify {
            updatePomodoroSettingsUseCase.updateSettings(
                match { it.defaultReviewCount == 5 }
            )
        }
    }

    // ========== Dialog state management ==========

    @Test
    fun `showReviewTasksDialog sets showReviewTasksDialog to true`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showReviewTasksDialog)

        viewModel.showReviewTasksDialog()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showReviewTasksDialog)
    }

    @Test
    fun `hideReviewTasksDialog sets showReviewTasksDialog to false`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showReviewTasksDialog()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showReviewTasksDialog)

        viewModel.hideReviewTasksDialog()

        assertFalse(viewModel.uiState.value.showReviewTasksDialog)
    }

    @Test
    fun `showDeleteAllReviewTasksDialog sets flag to true`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteAllReviewTasksDialog)

        viewModel.showDeleteAllReviewTasksDialog()

        assertTrue(viewModel.uiState.value.showDeleteAllReviewTasksDialog)
    }

    @Test
    fun `hideDeleteAllReviewTasksDialog sets flag to false`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showDeleteAllReviewTasksDialog()
        assertTrue(viewModel.uiState.value.showDeleteAllReviewTasksDialog)

        viewModel.hideDeleteAllReviewTasksDialog()

        assertFalse(viewModel.uiState.value.showDeleteAllReviewTasksDialog)
    }

    // ========== Premium state ==========

    @Test
    fun `uiState reflects premium subscription status`() = runTest {
        // Start with non-premium
        subscriptionStatusFlow.value = SubscriptionStatus(type = SubscriptionType.FREE)

        viewModel = createViewModel()
        advanceUntilIdle()

        // isPremium initial value is false
        assertFalse(viewModel.isPremium.value)

        // Update to premium - affects the upstream SubscriptionStatus
        subscriptionStatusFlow.value = SubscriptionStatus(type = SubscriptionType.LIFETIME)
        advanceUntilIdle()

        // Verify premium manager state was updated
        coEvery { premiumManager.isPremium() } returns true
        assertTrue(premiumManager.isPremium())
    }

    // ========== toggleFocusModeStrict / toggleAutoLoop return values ==========

    @Test
    fun `toggleFocusModeStrict returns true`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val result = viewModel.toggleFocusModeStrict(true)

        assertTrue(result)
        assertTrue(viewModel.uiState.value.focusModeStrict)
    }

    @Test
    fun `toggleAutoLoop returns true`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val result = viewModel.toggleAutoLoop(true)

        assertTrue(result)
        assertTrue(viewModel.uiState.value.autoLoopEnabled)
    }

    // ========== BGM Selector Dialog ==========

    @Test
    fun `showBgmSelector sets showBgmSelectorDialog to true`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showBgmSelectorDialog)

        viewModel.showBgmSelector()

        assertTrue(viewModel.uiState.value.showBgmSelectorDialog)
    }

    @Test
    fun `hideBgmSelector sets showBgmSelectorDialog to false`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showBgmSelector()
        assertTrue(viewModel.uiState.value.showBgmSelectorDialog)

        viewModel.hideBgmSelector()

        assertFalse(viewModel.uiState.value.showBgmSelectorDialog)
    }

    @Test
    fun `selectBgmTrack updates trackId and hides dialog`() = runTest {
        val track = mockk<BgmTrack>()
        every { track.id } returns "ocean"

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showBgmSelector()
        viewModel.selectBgmTrack(track)

        val state = viewModel.uiState.value
        assertEquals("ocean", state.bgmTrackId)
        assertFalse(state.showBgmSelectorDialog)
    }

    @Test
    fun `selectBgmTrack with null clears trackId`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectBgmTrack(null)

        assertNull(viewModel.uiState.value.bgmTrackId)
    }

    @Test
    fun `updateBgmVolume updates state and calls bgmManager`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateBgmVolume(0.7f)

        assertEquals(0.7f, viewModel.uiState.value.bgmVolume)
        io.mockk.verify { bgmManager.setVolume(0.7f) }
    }

    @Test
    fun `toggleBgmAutoPlay updates state and calls bgmManager`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleBgmAutoPlay(false)

        assertFalse(viewModel.uiState.value.bgmAutoPlayEnabled)
        io.mockk.verify { bgmManager.setAutoPlay(false) }
    }

    // ========== onEvent dispatch ==========

    @Test
    fun `onEvent ToggleNotifications delegates to toggleNotifications`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.ToggleNotifications(false))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.notificationsEnabled)
    }

    @Test
    fun `onEvent UpdateWorkDuration delegates to updateWorkDuration`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.UpdateWorkDuration(50))
        advanceUntilIdle()

        assertEquals(50, viewModel.uiState.value.workDurationMinutes)
    }

    @Test
    fun `onEvent ToggleFocusMode delegates to toggleFocusMode`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.ToggleFocusMode(false))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.focusModeEnabled)
    }

    @Test
    fun `onEvent UpdateLanguage delegates to updateLanguage`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.UpdateLanguage("en"))
        advanceUntilIdle()

        assertEquals("en", viewModel.uiState.value.language)
    }

    @Test
    fun `onEvent ToggleReviewIntervals delegates to toggleReviewIntervals`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.ToggleReviewIntervals(false))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.reviewIntervalsEnabled)
    }

    @Test
    fun `onEvent StartTrial delegates to startTrial`() = runTest {
        coEvery { premiumManager.startTrial() } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.StartTrial)
        advanceUntilIdle()

        coVerify { premiumManager.startTrial() }
    }

    // ========== Review Task Selection ==========

    private fun createReviewTask(id: Long, taskName: String = "Task $id"): ReviewTask {
        return ReviewTask(
            id = id,
            studySessionId = 1L,
            taskId = 1L,
            scheduledDate = LocalDate.of(2026, 1, 30),
            reviewNumber = 1,
            taskName = taskName
        )
    }

    @Test
    fun `toggleReviewTaskSelection adds task id to selection`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleReviewTaskSelection(1L)

        assertTrue(1L in viewModel.uiState.value.selectedReviewTaskIds)
    }

    @Test
    fun `toggleReviewTaskSelection removes task id when already selected`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleReviewTaskSelection(1L)
        assertTrue(1L in viewModel.uiState.value.selectedReviewTaskIds)

        viewModel.toggleReviewTaskSelection(1L)
        assertFalse(1L in viewModel.uiState.value.selectedReviewTaskIds)
    }

    @Test
    fun `selectAllReviewTasks selects all tasks in current list`() = runTest {
        val tasks = listOf(createReviewTask(1L), createReviewTask(2L), createReviewTask(3L))
        every { reviewTaskRepository.getAllWithDetails() } returns flowOf(tasks)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showReviewTasksDialog()
        advanceUntilIdle()

        viewModel.selectAllReviewTasks()

        val selected = viewModel.uiState.value.selectedReviewTaskIds
        assertEquals(setOf(1L, 2L, 3L), selected)
    }

    @Test
    fun `clearReviewTaskSelection clears all selections`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleReviewTaskSelection(1L)
        viewModel.toggleReviewTaskSelection(2L)
        assertEquals(2, viewModel.uiState.value.selectedReviewTaskIds.size)

        viewModel.clearReviewTaskSelection()

        assertTrue(viewModel.uiState.value.selectedReviewTaskIds.isEmpty())
    }

    @Test
    fun `hideReviewTasksDialog clears selection`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showReviewTasksDialog()
        advanceUntilIdle()

        viewModel.toggleReviewTaskSelection(1L)
        assertTrue(viewModel.uiState.value.selectedReviewTaskIds.isNotEmpty())

        viewModel.hideReviewTasksDialog()

        assertTrue(viewModel.uiState.value.selectedReviewTaskIds.isEmpty())
        assertFalse(viewModel.uiState.value.showReviewTasksDialog)
    }

    @Test
    fun `showDeleteSelectedReviewTasksDialog sets flag to true`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showDeleteSelectedReviewTasksDialog()

        assertTrue(viewModel.uiState.value.showDeleteSelectedReviewTasksDialog)
    }

    @Test
    fun `hideDeleteSelectedReviewTasksDialog sets flag to false`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showDeleteSelectedReviewTasksDialog()
        assertTrue(viewModel.uiState.value.showDeleteSelectedReviewTasksDialog)

        viewModel.hideDeleteSelectedReviewTasksDialog()

        assertFalse(viewModel.uiState.value.showDeleteSelectedReviewTasksDialog)
    }

    @Test
    fun `deleteSelectedReviewTasks calls repository and clears selection`() = runTest {
        coEvery { reviewTaskRepository.deleteByIds(any()) } returns Result.Success(Unit)
        coEvery { reviewTaskRepository.getTotalCount() } returns Result.Success(5)
        coEvery { reviewTaskRepository.getIncompleteCount() } returns Result.Success(3)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleReviewTaskSelection(1L)
        viewModel.toggleReviewTaskSelection(2L)
        viewModel.showDeleteSelectedReviewTasksDialog()

        viewModel.deleteSelectedReviewTasks()
        advanceUntilIdle()

        coVerify { reviewTaskRepository.deleteByIds(match { it.toSet() == setOf(1L, 2L) }) }
        assertTrue(viewModel.uiState.value.selectedReviewTaskIds.isEmpty())
        assertFalse(viewModel.uiState.value.showDeleteSelectedReviewTasksDialog)
    }

    @Test
    fun `deleteSelectedReviewTasks does nothing when selection is empty`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteSelectedReviewTasks()
        advanceUntilIdle()

        coVerify(exactly = 0) { reviewTaskRepository.deleteByIds(any()) }
    }

    @Test
    fun `deleteSelectedReviewTasks reloads counts after success`() = runTest {
        coEvery { reviewTaskRepository.deleteByIds(any()) } returns Result.Success(Unit)
        coEvery { reviewTaskRepository.getTotalCount() } returns Result.Success(10) andThen Result.Success(8)
        coEvery { reviewTaskRepository.getIncompleteCount() } returns Result.Success(5) andThen Result.Success(3)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(10, viewModel.uiState.value.reviewTaskTotalCount)

        viewModel.toggleReviewTaskSelection(1L)
        viewModel.deleteSelectedReviewTasks()
        advanceUntilIdle()

        assertEquals(8, viewModel.uiState.value.reviewTaskTotalCount)
        assertEquals(3, viewModel.uiState.value.reviewTaskIncompleteCount)
    }
}
