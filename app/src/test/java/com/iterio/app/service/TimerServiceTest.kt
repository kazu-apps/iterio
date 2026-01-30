package com.iterio.app.service

import org.junit.Assert.*
import org.junit.Test

/**
 * TimerService のユニットテスト
 *
 * TimerService自体はAndroidのServiceクラスを継承しているため、
 * ここでは主にTimerState, TimerPhase, TimerDefaultsのロジックをテストする。
 * 完全なService統合テストはinstrumented testで行う。
 */
class TimerServiceTest {

    // TimerState Tests

    @Test
    fun `TimerState default values are correct`() {
        val state = TimerState()

        assertEquals(TimerPhase.IDLE, state.phase)
        assertEquals(0, state.timeRemainingSeconds)
        assertEquals(0, state.totalTimeSeconds)
        assertEquals(1, state.currentCycle)
        assertEquals(TimerDefaults.DEFAULT_CYCLES, state.totalCycles)
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(0L, state.taskId)
        assertEquals("", state.taskName)
        assertEquals(0, state.totalWorkMinutes)
        assertEquals(TimerDefaults.DEFAULT_WORK_MINUTES, state.workDurationMinutes)
        assertEquals(TimerDefaults.DEFAULT_SHORT_BREAK_MINUTES, state.shortBreakMinutes)
        assertEquals(TimerDefaults.DEFAULT_LONG_BREAK_MINUTES, state.longBreakMinutes)
        assertFalse(state.sessionCompleted)
        assertFalse(state.autoLoopEnabled)
    }

    @Test
    fun `TimerState initializes correctly for work phase`() {
        val workDuration = 25
        val totalSeconds = workDuration * 60

        val state = TimerState(
            phase = TimerPhase.WORK,
            timeRemainingSeconds = totalSeconds,
            totalTimeSeconds = totalSeconds,
            currentCycle = 1,
            totalCycles = 4,
            isRunning = true,
            isPaused = false,
            taskId = 1L,
            taskName = "Math Study",
            workDurationMinutes = workDuration,
            shortBreakMinutes = 5,
            longBreakMinutes = 15
        )

        assertEquals(TimerPhase.WORK, state.phase)
        assertEquals(totalSeconds, state.timeRemainingSeconds)
        assertEquals(totalSeconds, state.totalTimeSeconds)
        assertEquals(1, state.currentCycle)
        assertEquals(4, state.totalCycles)
        assertTrue(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(1L, state.taskId)
        assertEquals("Math Study", state.taskName)
    }

    @Test
    fun `TimerState copy correctly updates values`() {
        val initialState = TimerState(
            phase = TimerPhase.WORK,
            timeRemainingSeconds = 1500,
            totalTimeSeconds = 1500,
            isRunning = true
        )

        // Simulate pause
        val pausedState = initialState.copy(isRunning = false, isPaused = true)
        assertFalse(pausedState.isRunning)
        assertTrue(pausedState.isPaused)
        assertEquals(TimerPhase.WORK, pausedState.phase)

        // Simulate resume
        val resumedState = pausedState.copy(isRunning = true, isPaused = false)
        assertTrue(resumedState.isRunning)
        assertFalse(resumedState.isPaused)
    }

    @Test
    fun `TimerState transition from work to short break`() {
        val initialState = TimerState(
            phase = TimerPhase.WORK,
            timeRemainingSeconds = 0,
            totalTimeSeconds = 1500,
            currentCycle = 1,
            totalCycles = 4,
            isRunning = true,
            totalWorkMinutes = 0,
            workDurationMinutes = 25,
            shortBreakMinutes = 5
        )

        // After work phase completes, transition to short break
        val shortBreakSeconds = initialState.shortBreakMinutes * 60
        val newWorkMinutes = initialState.totalWorkMinutes + initialState.workDurationMinutes

        val breakState = initialState.copy(
            phase = TimerPhase.SHORT_BREAK,
            timeRemainingSeconds = shortBreakSeconds,
            totalTimeSeconds = shortBreakSeconds,
            totalWorkMinutes = newWorkMinutes
        )

        assertEquals(TimerPhase.SHORT_BREAK, breakState.phase)
        assertEquals(shortBreakSeconds, breakState.timeRemainingSeconds)
        assertEquals(25, breakState.totalWorkMinutes)
        assertEquals(1, breakState.currentCycle) // Still cycle 1 during break
    }

    @Test
    fun `TimerState transition from short break to next work cycle`() {
        val breakState = TimerState(
            phase = TimerPhase.SHORT_BREAK,
            timeRemainingSeconds = 0,
            totalTimeSeconds = 300,
            currentCycle = 1,
            totalCycles = 4,
            isRunning = true,
            totalWorkMinutes = 25,
            workDurationMinutes = 25
        )

        // After short break completes, transition to next work cycle
        val workSeconds = breakState.workDurationMinutes * 60
        val nextWorkState = breakState.copy(
            phase = TimerPhase.WORK,
            timeRemainingSeconds = workSeconds,
            totalTimeSeconds = workSeconds,
            currentCycle = breakState.currentCycle + 1
        )

        assertEquals(TimerPhase.WORK, nextWorkState.phase)
        assertEquals(workSeconds, nextWorkState.timeRemainingSeconds)
        assertEquals(2, nextWorkState.currentCycle)
    }

    @Test
    fun `TimerState session completion without autoLoop`() {
        val lastCycleState = TimerState(
            phase = TimerPhase.WORK,
            timeRemainingSeconds = 0,
            totalTimeSeconds = 1500,
            currentCycle = 4,
            totalCycles = 4,
            isRunning = true,
            totalWorkMinutes = 75,
            workDurationMinutes = 25,
            autoLoopEnabled = false
        )

        val newWorkMinutes = lastCycleState.totalWorkMinutes + lastCycleState.workDurationMinutes

        // Session completes (no more cycles, no auto loop)
        val completedState = lastCycleState.copy(
            phase = TimerPhase.IDLE,
            isRunning = false,
            sessionCompleted = true,
            totalWorkMinutes = newWorkMinutes
        )

        assertEquals(TimerPhase.IDLE, completedState.phase)
        assertFalse(completedState.isRunning)
        assertTrue(completedState.sessionCompleted)
        assertEquals(100, completedState.totalWorkMinutes)
    }

    @Test
    fun `TimerState session completion with autoLoop enabled`() {
        val lastCycleState = TimerState(
            phase = TimerPhase.WORK,
            timeRemainingSeconds = 0,
            totalTimeSeconds = 1500,
            currentCycle = 4,
            totalCycles = 4,
            isRunning = true,
            totalWorkMinutes = 75,
            workDurationMinutes = 25,
            autoLoopEnabled = true
        )

        val newWorkMinutes = lastCycleState.totalWorkMinutes + lastCycleState.workDurationMinutes

        // With autoLoop enabled, session still completes (next task navigation handled by UI)
        val completedState = lastCycleState.copy(
            phase = TimerPhase.IDLE,
            isRunning = false,
            sessionCompleted = true,
            totalWorkMinutes = newWorkMinutes
        )

        assertEquals(TimerPhase.IDLE, completedState.phase)
        assertFalse(completedState.isRunning)
        assertTrue(completedState.sessionCompleted)
        assertEquals(100, completedState.totalWorkMinutes)
        assertTrue(completedState.autoLoopEnabled)
    }

    // TimerDefaults Tests

    @Test
    fun `TimerDefaults constants are correct`() {
        assertEquals(25, TimerDefaults.DEFAULT_WORK_MINUTES)
        assertEquals(5, TimerDefaults.DEFAULT_SHORT_BREAK_MINUTES)
        assertEquals(15, TimerDefaults.DEFAULT_LONG_BREAK_MINUTES)
        assertEquals(4, TimerDefaults.DEFAULT_CYCLES)
        assertEquals(1, TimerDefaults.MIN_CYCLES)
        assertEquals(10, TimerDefaults.MAX_CYCLES)
        assertEquals(1, TimerDefaults.MIN_WORK_MINUTES)
        assertEquals(180, TimerDefaults.MAX_WORK_MINUTES)
    }

    @Test
    fun `TimerDefaults cycles within valid range`() {
        val cycles = 5
        val validCycles = cycles.coerceIn(TimerDefaults.MIN_CYCLES, TimerDefaults.MAX_CYCLES)
        assertEquals(5, validCycles)

        val lowCycles = 0.coerceIn(TimerDefaults.MIN_CYCLES, TimerDefaults.MAX_CYCLES)
        assertEquals(TimerDefaults.MIN_CYCLES, lowCycles)

        val highCycles = 15.coerceIn(TimerDefaults.MIN_CYCLES, TimerDefaults.MAX_CYCLES)
        assertEquals(TimerDefaults.MAX_CYCLES, highCycles)
    }

    @Test
    fun `TimerDefaults work minutes within valid range`() {
        val workMinutes = 45
        val validMinutes = workMinutes.coerceIn(TimerDefaults.MIN_WORK_MINUTES, TimerDefaults.MAX_WORK_MINUTES)
        assertEquals(45, validMinutes)

        val lowMinutes = 0.coerceIn(TimerDefaults.MIN_WORK_MINUTES, TimerDefaults.MAX_WORK_MINUTES)
        assertEquals(TimerDefaults.MIN_WORK_MINUTES, lowMinutes)

        val highMinutes = 200.coerceIn(TimerDefaults.MIN_WORK_MINUTES, TimerDefaults.MAX_WORK_MINUTES)
        assertEquals(TimerDefaults.MAX_WORK_MINUTES, highMinutes)
    }

    // TimerPhase Tests

    @Test
    fun `TimerPhase enum values are correct`() {
        val phases = TimerPhase.values()
        assertEquals(4, phases.size)
        assertTrue(phases.contains(TimerPhase.WORK))
        assertTrue(phases.contains(TimerPhase.SHORT_BREAK))
        assertTrue(phases.contains(TimerPhase.LONG_BREAK))
        assertTrue(phases.contains(TimerPhase.IDLE))
    }

    @Test
    fun `TimerPhase WORK phase`() {
        val phase = TimerPhase.WORK
        assertEquals("WORK", phase.name)
        assertEquals(0, phase.ordinal)
    }

    @Test
    fun `TimerPhase SHORT_BREAK phase`() {
        val phase = TimerPhase.SHORT_BREAK
        assertEquals("SHORT_BREAK", phase.name)
        assertEquals(1, phase.ordinal)
    }

    @Test
    fun `TimerPhase LONG_BREAK phase`() {
        val phase = TimerPhase.LONG_BREAK
        assertEquals("LONG_BREAK", phase.name)
        assertEquals(2, phase.ordinal)
    }

    @Test
    fun `TimerPhase IDLE phase`() {
        val phase = TimerPhase.IDLE
        assertEquals("IDLE", phase.name)
        assertEquals(3, phase.ordinal)
    }

    // Time calculation tests

    @Test
    fun `time remaining calculation is correct`() {
        val workDurationMinutes = 25
        val totalSeconds = workDurationMinutes * 60

        assertEquals(1500, totalSeconds)

        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        assertEquals(25, minutes)
        assertEquals(0, seconds)
    }

    @Test
    fun `time format with remaining seconds`() {
        val remainingSeconds = 754 // 12:34
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60

        assertEquals(12, minutes)
        assertEquals(34, seconds)

        val formatted = String.format("%02d:%02d", minutes, seconds)
        assertEquals("12:34", formatted)
    }

    @Test
    fun `accumulated work minutes calculation`() {
        var totalWorkMinutes = 0
        val workDurationMinutes = 25

        // Cycle 1 complete
        totalWorkMinutes += workDurationMinutes
        assertEquals(25, totalWorkMinutes)

        // Cycle 2 complete
        totalWorkMinutes += workDurationMinutes
        assertEquals(50, totalWorkMinutes)

        // Cycle 3 complete
        totalWorkMinutes += workDurationMinutes
        assertEquals(75, totalWorkMinutes)

        // Cycle 4 complete
        totalWorkMinutes += workDurationMinutes
        assertEquals(100, totalWorkMinutes)
    }
}
