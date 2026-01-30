package com.iterio.app.domain.usecase

import com.iterio.app.domain.common.DomainError
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.PomodoroSettings
import com.iterio.app.domain.model.ReviewTask
import com.iterio.app.domain.model.Task
import com.iterio.app.domain.repository.DailyStatsRepository
import com.iterio.app.domain.repository.ReviewTaskRepository
import com.iterio.app.domain.repository.StudySessionRepository
import com.iterio.app.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * FinishTimerSessionUseCase のユニットテスト
 */
class FinishTimerSessionUseCaseTest {

    private lateinit var studySessionRepository: StudySessionRepository
    private lateinit var reviewTaskRepository: ReviewTaskRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var dailyStatsRepository: DailyStatsRepository
    private lateinit var useCase: FinishTimerSessionUseCase

    @Before
    fun setup() {
        studySessionRepository = mockk(relaxed = true)
        reviewTaskRepository = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)
        dailyStatsRepository = mockk(relaxed = true)

        // Set up default return values for Result types
        coEvery { studySessionRepository.finishSession(any(), any(), any(), any()) } returns Result.Success(Unit)
        coEvery { reviewTaskRepository.insertAll(any()) } returns Result.Success(Unit)
        coEvery { taskRepository.updateLastStudiedAt(any(), any()) } returns Result.Success(Unit)
        coEvery { dailyStatsRepository.updateStats(any(), any(), any()) } returns Result.Success(Unit)

        useCase = FinishTimerSessionUseCase(studySessionRepository, reviewTaskRepository, taskRepository, dailyStatsRepository)
    }

    @Test
    fun `finishes session with correct parameters`() = runTest {
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 10L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(reviewEnabled = false),
            totalWorkMinutes = 50,
            currentCycle = 2,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = false,
            reviewIntervals = listOf(1, 3, 7)
        )

        useCase(params)

        coVerify {
            studySessionRepository.finishSession(
                id = 1L,
                durationMinutes = 50,
                cycles = 2,
                interrupted = false
            )
        }
    }

    @Test
    fun `creates review tasks when review enabled and not interrupted`() = runTest {
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 10L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(reviewEnabled = true),
            totalWorkMinutes = 50,
            currentCycle = 4,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = true,
            reviewIntervals = listOf(1, 3, 7, 14, 30, 60)
        )

        val capturedTasks = slot<List<ReviewTask>>()
        coEvery { reviewTaskRepository.insertAll(capture(capturedTasks)) } returns Result.Success(Unit)

        useCase(params)

        coVerify { reviewTaskRepository.insertAll(any()) }
        assertEquals(6, capturedTasks.captured.size)
    }

    @Test
    fun `does not create review tasks when interrupted`() = runTest {
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 10L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(reviewEnabled = true),
            totalWorkMinutes = 25,
            currentCycle = 1,
            totalCycles = 4,
            isInterrupted = true,
            isPremium = false,
            reviewIntervals = listOf(1, 3, 7)
        )

        useCase(params)

        coVerify(exactly = 0) { reviewTaskRepository.insertAll(any()) }
    }

    @Test
    fun `does not create review tasks when review disabled`() = runTest {
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 10L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(reviewEnabled = false),
            totalWorkMinutes = 50,
            currentCycle = 4,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = true,
            reviewIntervals = listOf(1, 3, 7)
        )

        useCase(params)

        coVerify(exactly = 0) { reviewTaskRepository.insertAll(any()) }
    }

    @Test
    fun `review tasks have correct session and task ids`() = runTest {
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 100L,
            task = Task(id = 50L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(reviewEnabled = true),
            totalWorkMinutes = 25,
            currentCycle = 4,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = false,
            reviewIntervals = listOf(1, 3)
        )

        val capturedTasks = slot<List<ReviewTask>>()
        coEvery { reviewTaskRepository.insertAll(capture(capturedTasks)) } returns Result.Success(Unit)

        useCase(params)

        capturedTasks.captured.forEach { task ->
            assertEquals(100L, task.studySessionId)
            assertEquals(50L, task.taskId)
        }
    }

    @Test
    fun `review tasks have correct scheduled dates based on intervals`() = runTest {
        val today = LocalDate.now()
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 1L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(reviewEnabled = true),
            totalWorkMinutes = 25,
            currentCycle = 4,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = false,
            reviewIntervals = listOf(1, 3, 7)
        )

        val capturedTasks = slot<List<ReviewTask>>()
        coEvery { reviewTaskRepository.insertAll(capture(capturedTasks)) } returns Result.Success(Unit)

        useCase(params)

        assertEquals(today.plusDays(1), capturedTasks.captured[0].scheduledDate)
        assertEquals(today.plusDays(3), capturedTasks.captured[1].scheduledDate)
        assertEquals(today.plusDays(7), capturedTasks.captured[2].scheduledDate)
    }

    @Test
    fun `uses totalCycles when session completed normally`() = runTest {
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 1L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(),
            totalWorkMinutes = 100,
            currentCycle = 4,
            totalCycles = 4,
            isInterrupted = false,
            isSessionCompleted = true,
            isPremium = false,
            reviewIntervals = emptyList()
        )

        useCase(params)

        coVerify {
            studySessionRepository.finishSession(
                id = 1L,
                durationMinutes = 100,
                cycles = 4,
                interrupted = false
            )
        }
    }

    @Test
    fun `uses currentCycle when session not completed`() = runTest {
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 1L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(),
            totalWorkMinutes = 25,
            currentCycle = 2,
            totalCycles = 4,
            isInterrupted = true,
            isSessionCompleted = false,
            isPremium = false,
            reviewIntervals = emptyList()
        )

        useCase(params)

        coVerify {
            studySessionRepository.finishSession(
                id = 1L,
                durationMinutes = 25,
                cycles = 2,
                interrupted = true
            )
        }
    }

    @Test
    fun `returns success on completion`() = runTest {
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 1L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(reviewEnabled = false),
            totalWorkMinutes = 25,
            currentCycle = 1,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = false,
            reviewIntervals = emptyList()
        )

        val result = useCase(params)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure when repository throws exception`() = runTest {
        coEvery { studySessionRepository.finishSession(any(), any(), any(), any()) } returns Result.Failure(DomainError.DatabaseError("DB error"))

        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 1L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(),
            totalWorkMinutes = 25,
            currentCycle = 1,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = false,
            reviewIntervals = emptyList()
        )

        val result = useCase(params)

        assertTrue(result.isFailure)
    }

    @Test
    fun `updates lastStudiedAt for the task`() = runTest {
        val taskId = 42L
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = taskId, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(reviewEnabled = false),
            totalWorkMinutes = 25,
            currentCycle = 1,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = false,
            reviewIntervals = emptyList()
        )

        val capturedTaskId = slot<Long>()
        val capturedDateTime = slot<LocalDateTime>()
        coEvery { taskRepository.updateLastStudiedAt(capture(capturedTaskId), capture(capturedDateTime)) } returns Result.Success(Unit)

        useCase(params)

        coVerify { taskRepository.updateLastStudiedAt(any(), any()) }
        assertEquals(taskId, capturedTaskId.captured)
        // Verify the timestamp is recent (within last minute)
        val now = LocalDateTime.now()
        assertTrue(capturedDateTime.captured.isBefore(now.plusSeconds(1)))
        assertTrue(capturedDateTime.captured.isAfter(now.minusMinutes(1)))
    }

    @Test
    fun `updates daily stats on session finish`() = runTest {
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 10L, groupId = 1L, name = "Math"),
            settings = PomodoroSettings(reviewEnabled = false),
            totalWorkMinutes = 50,
            currentCycle = 2,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = false,
            reviewIntervals = emptyList()
        )

        val capturedDate = slot<LocalDate>()
        val capturedMinutes = slot<Int>()
        val capturedSubject = slot<String>()
        coEvery {
            dailyStatsRepository.updateStats(capture(capturedDate), capture(capturedMinutes), capture(capturedSubject))
        } returns Result.Success(Unit)

        useCase(params)

        coVerify { dailyStatsRepository.updateStats(any(), any(), any()) }
        assertEquals(LocalDate.now(), capturedDate.captured)
        assertEquals(50, capturedMinutes.captured)
    }

    @Test
    fun `updates daily stats with correct task name`() = runTest {
        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 10L, groupId = 1L, name = "English Vocabulary"),
            settings = PomodoroSettings(reviewEnabled = false),
            totalWorkMinutes = 25,
            currentCycle = 1,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = false,
            reviewIntervals = emptyList()
        )

        val capturedSubject = slot<String>()
        coEvery {
            dailyStatsRepository.updateStats(any(), any(), capture(capturedSubject))
        } returns Result.Success(Unit)

        useCase(params)

        assertEquals("English Vocabulary", capturedSubject.captured)
    }

    @Test
    fun `does not update daily stats when finish session fails`() = runTest {
        coEvery {
            studySessionRepository.finishSession(any(), any(), any(), any())
        } returns Result.Failure(DomainError.DatabaseError("DB error"))

        val params = FinishTimerSessionUseCase.Params(
            sessionId = 1L,
            task = Task(id = 10L, groupId = 1L, name = "Study"),
            settings = PomodoroSettings(reviewEnabled = false),
            totalWorkMinutes = 25,
            currentCycle = 1,
            totalCycles = 4,
            isInterrupted = false,
            isPremium = false,
            reviewIntervals = emptyList()
        )

        val result = useCase(params)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dailyStatsRepository.updateStats(any(), any(), any()) }
    }
}
