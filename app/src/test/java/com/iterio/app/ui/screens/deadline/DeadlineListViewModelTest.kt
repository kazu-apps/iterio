package com.iterio.app.ui.screens.deadline

import app.cash.turbine.test
import com.iterio.app.domain.model.DeadlineItem
import com.iterio.app.domain.model.ScheduleType
import com.iterio.app.domain.model.SubjectGroup
import com.iterio.app.domain.model.Task
import com.iterio.app.fakes.FakeSubjectGroupRepository
import com.iterio.app.fakes.FakeTaskRepository
import com.iterio.app.testutil.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * DeadlineListViewModel のユニットテスト
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeadlineListViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var taskRepository: FakeTaskRepository
    private lateinit var subjectGroupRepository: FakeSubjectGroupRepository

    @Before
    fun setup() {
        taskRepository = FakeTaskRepository()
        subjectGroupRepository = FakeSubjectGroupRepository()
    }

    private fun createViewModel() = DeadlineListViewModel(
        taskRepository = taskRepository,
        subjectGroupRepository = subjectGroupRepository
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
    fun `loads task deadlines`() = runTest {
        val today = LocalDate.now()
        taskRepository.setTasks(listOf(
            Task(id = 1L, groupId = 1L, name = "Report", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = today.plusDays(3))
        ))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.allItems.size)
            assertEquals(1, state.taskCount)
            assertEquals(0, state.groupCount)
            assertTrue(state.allItems[0] is DeadlineItem.TaskDeadline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads group deadlines`() = runTest {
        val today = LocalDate.now()
        subjectGroupRepository.setGroups(listOf(
            SubjectGroup(id = 1L, name = "Math", colorHex = "#FF0000", hasDeadline = true, deadlineDate = today.plusDays(5))
        ))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.allItems.size)
            assertEquals(0, state.taskCount)
            assertEquals(1, state.groupCount)
            assertTrue(state.allItems[0] is DeadlineItem.GroupDeadline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `combines and sorts task and group deadlines by date`() = runTest {
        val today = LocalDate.now()
        taskRepository.setTasks(listOf(
            Task(id = 1L, groupId = 1L, name = "Report", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = today.plusDays(5))
        ))
        subjectGroupRepository.setGroups(listOf(
            SubjectGroup(id = 10L, name = "Physics", colorHex = "#00FF00", hasDeadline = true, deadlineDate = today.plusDays(2))
        ))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.allItems.size)
            assertEquals(2, state.totalCount)
            assertEquals(1, state.taskCount)
            assertEquals(1, state.groupCount)
            // Group (2 days) comes before Task (5 days)
            assertTrue(state.allItems[0] is DeadlineItem.GroupDeadline)
            assertTrue(state.allItems[1] is DeadlineItem.TaskDeadline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter TASKS shows only task deadlines`() = runTest {
        val today = LocalDate.now()
        taskRepository.setTasks(listOf(
            Task(id = 1L, groupId = 1L, name = "Report", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = today.plusDays(3))
        ))
        subjectGroupRepository.setGroups(listOf(
            SubjectGroup(id = 10L, name = "Physics", colorHex = "#00FF00", hasDeadline = true, deadlineDate = today.plusDays(5))
        ))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateFilter(DeadlineFilter.TASKS)

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(DeadlineFilter.TASKS, state.selectedFilter)
            assertEquals(1, state.filteredItems.size)
            assertTrue(state.filteredItems[0] is DeadlineItem.TaskDeadline)
            // Total counts remain unchanged
            assertEquals(2, state.totalCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter GROUPS shows only group deadlines`() = runTest {
        val today = LocalDate.now()
        taskRepository.setTasks(listOf(
            Task(id = 1L, groupId = 1L, name = "Report", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = today.plusDays(3))
        ))
        subjectGroupRepository.setGroups(listOf(
            SubjectGroup(id = 10L, name = "Physics", colorHex = "#00FF00", hasDeadline = true, deadlineDate = today.plusDays(5))
        ))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateFilter(DeadlineFilter.GROUPS)

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(DeadlineFilter.GROUPS, state.selectedFilter)
            assertEquals(1, state.filteredItems.size)
            assertTrue(state.filteredItems[0] is DeadlineItem.GroupDeadline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter ALL shows all items`() = runTest {
        val today = LocalDate.now()
        taskRepository.setTasks(listOf(
            Task(id = 1L, groupId = 1L, name = "Report", groupColor = "#FF0000", scheduleType = ScheduleType.DEADLINE, deadlineDate = today.plusDays(3))
        ))
        subjectGroupRepository.setGroups(listOf(
            SubjectGroup(id = 10L, name = "Physics", colorHex = "#00FF00", hasDeadline = true, deadlineDate = today.plusDays(5))
        ))

        val vm = createViewModel()
        advanceUntilIdle()

        // Switch to TASKS filter first
        vm.updateFilter(DeadlineFilter.TASKS)
        // Then switch back to ALL
        vm.updateFilter(DeadlineFilter.ALL)

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(DeadlineFilter.ALL, state.selectedFilter)
            assertEquals(2, state.filteredItems.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty state when no deadline items`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.allItems.isEmpty())
            assertTrue(state.filteredItems.isEmpty())
            assertEquals(0, state.totalCount)
            assertEquals(0, state.taskCount)
            assertEquals(0, state.groupCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
