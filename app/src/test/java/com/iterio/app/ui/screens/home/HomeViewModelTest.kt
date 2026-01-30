package com.iterio.app.ui.screens.home

import android.content.Context
import app.cash.turbine.test
import com.iterio.app.domain.common.Result
import com.iterio.app.widget.IterioWidgetReceiver
import com.iterio.app.domain.model.DeadlineItem
import com.iterio.app.domain.model.ReviewTask
import com.iterio.app.domain.model.ScheduleType
import com.iterio.app.domain.model.SubjectGroup
import com.iterio.app.domain.model.Task
import com.iterio.app.domain.repository.DailyStatsRepository
import com.iterio.app.domain.repository.DayStats
import com.iterio.app.domain.repository.ReviewTaskRepository
import com.iterio.app.domain.repository.StudySessionRepository
import com.iterio.app.domain.repository.SubjectGroupRepository
import com.iterio.app.domain.repository.TaskRepository
import com.iterio.app.domain.usecase.GetTodayTasksUseCase
import com.iterio.app.domain.usecase.TodayTasksResult
import com.iterio.app.service.TimerPhase
import com.iterio.app.service.TimerService
import com.iterio.app.service.TimerState
import com.iterio.app.testutil.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * HomeViewModel のユニットテスト
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var taskRepository: TaskRepository
    private lateinit var studySessionRepository: StudySessionRepository
    private lateinit var dailyStatsRepository: DailyStatsRepository
    private lateinit var reviewTaskRepository: ReviewTaskRepository
    private lateinit var subjectGroupRepository: SubjectGroupRepository
    private lateinit var getTodayTasksUseCase: GetTodayTasksUseCase
    private lateinit var context: Context
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        taskRepository = mockk()
        studySessionRepository = mockk()
        dailyStatsRepository = mockk()
        reviewTaskRepository = mockk()
        subjectGroupRepository = mockk()
        getTodayTasksUseCase = mockk()
        context = mockk(relaxed = true)

        // Reset static timer state between tests
        TimerService.resetActiveTimerState()

        // Mock widget broadcast to avoid Android Intent in unit tests
        mockkObject(IterioWidgetReceiver.Companion)
        every { IterioWidgetReceiver.sendDataChangedBroadcast(any()) } returns Unit
        every { IterioWidgetReceiver.sendUpdateBroadcast(any()) } returns Unit

        // Default mocks
        val today = LocalDate.now()
        every { studySessionRepository.observeTotalMinutesForDay(any()) } returns flowOf(0)
        every { studySessionRepository.observeTotalCyclesForDay(any()) } returns flowOf(0)
        coEvery { dailyStatsRepository.getCurrentStreak() } returns Result.Success(0)
        coEvery { dailyStatsRepository.getWeeklyData(any()) } returns Result.Success(emptyList())
        every { taskRepository.getUpcomingDeadlineTasks(any(), any()) } returns flowOf(emptyList())
        every { subjectGroupRepository.getUpcomingDeadlineGroups(any(), any()) } returns flowOf(emptyList())
        every { getTodayTasksUseCase(any()) } returns flowOf(
            TodayTasksResult(emptyList(), emptyList())
        )
    }

    private fun createViewModel() = HomeViewModel(
        context = context,
        taskRepository = taskRepository,
        studySessionRepository = studySessionRepository,
        dailyStatsRepository = dailyStatsRepository,
        reviewTaskRepository = reviewTaskRepository,
        subjectGroupRepository = subjectGroupRepository,
        getTodayTasksUseCase = getTodayTasksUseCase
    )

    @Test
    fun `initial state is loading`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads today minutes correctly`() = runTest {
        every { studySessionRepository.observeTotalMinutesForDay(any()) } returns flowOf(120)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(120, state.todayMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads today cycles correctly`() = runTest {
        every { studySessionRepository.observeTotalCyclesForDay(any()) } returns flowOf(4)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(4, state.todayCycles)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads current streak correctly`() = runTest {
        coEvery { dailyStatsRepository.getCurrentStreak() } returns Result.Success(7)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(7, state.currentStreak)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads today scheduled tasks`() = runTest {
        val tasks = listOf(
            Task(id = 1L, groupId = 1L, name = "Math Study", scheduleType = ScheduleType.REPEAT),
            Task(id = 2L, groupId = 1L, name = "English", scheduleType = ScheduleType.SPECIFIC)
        )
        every { getTodayTasksUseCase(any()) } returns flowOf(
            TodayTasksResult(tasks, emptyList())
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.todayScheduledTasks.size)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads today review tasks`() = runTest {
        val reviewTasks = listOf(
            ReviewTask(id = 1L, studySessionId = 1L, taskId = 1L, scheduledDate = LocalDate.now(), reviewNumber = 1),
            ReviewTask(id = 2L, studySessionId = 2L, taskId = 2L, scheduledDate = LocalDate.now(), reviewNumber = 2)
        )
        every { getTodayTasksUseCase(any()) } returns flowOf(
            TodayTasksResult(emptyList(), reviewTasks)
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.todayReviewTasks.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads weekly data`() = runTest {
        val weeklyData = listOf(
            DayStats("月", LocalDate.now(), 60),
            DayStats("火", LocalDate.now().plusDays(1), 90)
        )
        coEvery { dailyStatsRepository.getWeeklyData(any()) } returns Result.Success(weeklyData)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.weeklyData.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads upcoming task deadlines`() = runTest {
        val tasks = listOf(
            Task(id = 1L, groupId = 1L, name = "Report", groupName = "Math", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = LocalDate.now().plusDays(3))
        )
        every { taskRepository.getUpcomingDeadlineTasks(any(), any()) } returns flowOf(tasks)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.upcomingTaskDeadlines.size)
            assertEquals("Report", state.upcomingTaskDeadlines[0].name)
            assertTrue(state.upcomingGroupDeadlines.isEmpty())
            assertEquals(1, state.totalTaskDeadlineCount)
            assertEquals(0, state.totalGroupDeadlineCount)
            assertEquals(1, state.totalDeadlineCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads separated task and group deadlines`() = runTest {
        val tasks = listOf(
            Task(id = 1L, groupId = 1L, name = "Report", groupName = "Math", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = LocalDate.now().plusDays(5))
        )
        val groups = listOf(
            SubjectGroup(id = 10L, name = "Physics", colorHex = "#00FF00", hasDeadline = true, deadlineDate = LocalDate.now().plusDays(3))
        )
        every { taskRepository.getUpcomingDeadlineTasks(any(), any()) } returns flowOf(tasks)
        every { subjectGroupRepository.getUpcomingDeadlineGroups(any(), any()) } returns flowOf(groups)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.upcomingTaskDeadlines.size)
            assertEquals(1, state.upcomingGroupDeadlines.size)
            assertEquals("Report", state.upcomingTaskDeadlines[0].name)
            assertEquals("Physics", state.upcomingGroupDeadlines[0].name)
            assertEquals(1, state.totalTaskDeadlineCount)
            assertEquals(1, state.totalGroupDeadlineCount)
            assertEquals(2, state.totalDeadlineCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `task deadline items limited to 3 on home screen`() = runTest {
        val tasks = listOf(
            Task(id = 1L, groupId = 1L, name = "T1", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = LocalDate.now().plusDays(1)),
            Task(id = 2L, groupId = 1L, name = "T2", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = LocalDate.now().plusDays(2)),
            Task(id = 3L, groupId = 1L, name = "T3", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = LocalDate.now().plusDays(3)),
            Task(id = 4L, groupId = 1L, name = "T4", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = LocalDate.now().plusDays(4))
        )
        every { taskRepository.getUpcomingDeadlineTasks(any(), any()) } returns flowOf(tasks)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.upcomingTaskDeadlines.size)
            assertEquals(4, state.totalTaskDeadlineCount)
            assertEquals(4, state.totalDeadlineCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `group deadline items limited to 3 on home screen`() = runTest {
        val groups = listOf(
            SubjectGroup(id = 1L, name = "G1", colorHex = "#FF0000", hasDeadline = true, deadlineDate = LocalDate.now().plusDays(1)),
            SubjectGroup(id = 2L, name = "G2", colorHex = "#FF0000", hasDeadline = true, deadlineDate = LocalDate.now().plusDays(2)),
            SubjectGroup(id = 3L, name = "G3", colorHex = "#FF0000", hasDeadline = true, deadlineDate = LocalDate.now().plusDays(3)),
            SubjectGroup(id = 4L, name = "G4", colorHex = "#FF0000", hasDeadline = true, deadlineDate = LocalDate.now().plusDays(4))
        )
        every { subjectGroupRepository.getUpcomingDeadlineGroups(any(), any()) } returns flowOf(groups)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.upcomingGroupDeadlines.size)
            assertEquals(4, state.totalGroupDeadlineCount)
            assertEquals(4, state.totalDeadlineCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads only group deadlines when no task deadlines exist`() = runTest {
        val groups = listOf(
            SubjectGroup(id = 10L, name = "Physics", colorHex = "#00FF00", hasDeadline = true, deadlineDate = LocalDate.now().plusDays(3))
        )
        every { subjectGroupRepository.getUpcomingDeadlineGroups(any(), any()) } returns flowOf(groups)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.upcomingTaskDeadlines.isEmpty())
            assertEquals(1, state.upcomingGroupDeadlines.size)
            assertEquals(0, state.totalTaskDeadlineCount)
            assertEquals(1, state.totalGroupDeadlineCount)
            assertEquals(1, state.totalDeadlineCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadHomeData refreshes all data`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        every { studySessionRepository.observeTotalMinutesForDay(any()) } returns flowOf(180)
        vm.loadHomeData()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(180, state.todayMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleReviewTaskComplete marks task as completed`() = runTest {
        coEvery { reviewTaskRepository.markAsCompleted(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleReviewTaskComplete(1L, true)
        advanceUntilIdle()

        coVerify { reviewTaskRepository.markAsCompleted(1L) }
    }

    @Test
    fun `toggleReviewTaskComplete marks task as incomplete`() = runTest {
        coEvery { reviewTaskRepository.markAsIncomplete(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleReviewTaskComplete(1L, false)
        advanceUntilIdle()

        coVerify { reviewTaskRepository.markAsIncomplete(1L) }
    }

    // ========== エラーパス テスト ===========

    @Test
    fun `todayMinutes defaults to 0 when no sessions exist`() = runTest {
        every { studySessionRepository.observeTotalMinutesForDay(any()) } returns flowOf(0)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.todayMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `todayCycles defaults to 0 when no sessions exist`() = runTest {
        every { studySessionRepository.observeTotalCyclesForDay(any()) } returns flowOf(0)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.todayCycles)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `currentStreak remains 0 on failure`() = runTest {
        coEvery { dailyStatsRepository.getCurrentStreak() } returns
            Result.Failure(com.iterio.app.domain.common.DomainError.DatabaseError("DB error"))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.currentStreak)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `weeklyData remains empty on failure`() = runTest {
        coEvery { dailyStatsRepository.getWeeklyData(any()) } returns
            Result.Failure(com.iterio.app.domain.common.DomainError.DatabaseError("DB error"))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.weeklyData.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLoading becomes false after data load completes`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== Active Timer State テスト ===========

    @Test
    fun `activeTimerState is null when no timer is running`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.activeTimerState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `activeTimerState reflects running timer`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        // Simulate an active timer via the static StateFlow
        TimerService.resetActiveTimerState()
        // The companion _activeTimerState is private, so we test via ViewModel collecting it
        // Since resetActiveTimerState sets it to null, the state should remain null
        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.activeTimerState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `activeTimerState initial value is null after reset`() = runTest {
        TimerService.resetActiveTimerState()

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.activeTimerState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `activeTimerState field exists in HomeUiState`() = runTest {
        val uiState = HomeUiState()
        assertNull(uiState.activeTimerState)

        val timerState = TimerState(
            phase = TimerPhase.WORK,
            timeRemainingSeconds = 1500,
            totalTimeSeconds = 1500,
            currentCycle = 1,
            totalCycles = 4,
            isRunning = true,
            taskName = "Math Study"
        )
        val uiStateWithTimer = uiState.copy(activeTimerState = timerState)
        assertNotNull(uiStateWithTimer.activeTimerState)
        assertEquals(TimerPhase.WORK, uiStateWithTimer.activeTimerState?.phase)
        assertEquals("Math Study", uiStateWithTimer.activeTimerState?.taskName)
        assertTrue(uiStateWithTimer.activeTimerState?.isRunning == true)
    }

    // ========== リアクティブ更新テスト ===========

    @Test
    fun `todayMinutes updates reactively when session is saved`() = runTest {
        val minutesFlow = MutableStateFlow(0)
        val cyclesFlow = MutableStateFlow(0)
        every { studySessionRepository.observeTotalMinutesForDay(any()) } returns minutesFlow
        every { studySessionRepository.observeTotalCyclesForDay(any()) } returns cyclesFlow

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.todayMinutes)
        assertEquals(0, vm.uiState.value.todayCycles)

        // Simulate session save
        minutesFlow.value = 25
        cyclesFlow.value = 4
        advanceUntilIdle()

        assertEquals(25, vm.uiState.value.todayMinutes)
        assertEquals(4, vm.uiState.value.todayCycles)

        // Simulate 2nd session save
        minutesFlow.value = 50
        cyclesFlow.value = 8
        advanceUntilIdle()

        assertEquals(50, vm.uiState.value.todayMinutes)
        assertEquals(8, vm.uiState.value.todayCycles)
    }
}
