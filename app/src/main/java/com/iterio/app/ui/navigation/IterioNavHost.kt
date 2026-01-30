package com.iterio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.iterio.app.service.FocusModeService
import com.iterio.app.service.TimerService
import com.iterio.app.ui.screens.calendar.CalendarScreen
import com.iterio.app.ui.screens.home.HomeScreen
import com.iterio.app.ui.screens.backup.BackupScreen
import com.iterio.app.ui.screens.premium.PremiumScreen
import com.iterio.app.ui.screens.settings.SettingsScreen
import com.iterio.app.ui.screens.settings.allowedapps.AllowedAppsScreen
import com.iterio.app.ui.screens.stats.StatsScreen
import com.iterio.app.ui.screens.tasks.TasksScreen
import com.iterio.app.ui.screens.deadline.DeadlineListScreen
import com.iterio.app.ui.screens.review.ReviewScheduleScreen
import com.iterio.app.ui.screens.timer.TimerScreen
import kotlinx.coroutines.flow.combine

@Composable
fun IterioNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Auto-navigate to Timer screen when a session completes while user is on another screen
    val completedTaskId by TimerService.sessionCompletedTaskId
        .collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(completedTaskId) {
        completedTaskId?.let { taskId ->
            navController.navigate(Screen.Timer.createRoute(taskId)) {
                launchSingleTop = true
            }
            TimerService.consumeSessionCompletedEvent()
        }
    }

    // Auto-navigate to Timer screen during strict lock mode
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            combine(
                FocusModeService.isStrictMode,
                TimerService.activeTimerState
            ) { strictMode, timerState ->
                strictMode to timerState
            }.collect { (strictMode, timerState) ->
                if (strictMode && timerState != null) {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (currentRoute != Screen.Timer.route) {
                        navController.navigate(Screen.Timer.createRoute(timerState.taskId)) {
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTimer = { taskId ->
                    navController.navigate(Screen.Timer.createRoute(taskId))
                },
                onNavigateToTasks = {
                    navController.navigate(Screen.Tasks.route)
                },
                onNavigateToReviewSchedule = {
                    navController.navigate(Screen.ReviewSchedule.route)
                },
                onNavigateToDeadlineList = {
                    navController.navigate(Screen.DeadlineList.route)
                }
            )
        }

        composable(Screen.Tasks.route) {
            TasksScreen(
                onStartTimer = { task ->
                    navController.navigate(Screen.Timer.createRoute(task.id))
                }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onStartTimer = { taskId ->
                    navController.navigate(Screen.Timer.createRoute(taskId))
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToPremium = {
                    navController.navigate(Screen.Premium.route)
                },
                onNavigateToBackup = {
                    navController.navigate(Screen.Backup.route)
                },
                onNavigateToAllowedApps = {
                    navController.navigate(Screen.AllowedApps.route)
                }
            )
        }

        composable(Screen.AllowedApps.route) {
            AllowedAppsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Premium.route) {
            PremiumScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ReviewSchedule.route) {
            ReviewScheduleScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.DeadlineList.route) {
            DeadlineListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartTimer = { taskId ->
                    navController.navigate(Screen.Timer.createRoute(taskId))
                }
            )
        }

        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPremium = {
                    navController.navigate(Screen.Premium.route)
                }
            )
        }

        composable(
            route = Screen.Timer.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            TimerScreen(
                taskId = taskId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPremium = {
                    navController.navigate(Screen.Premium.route)
                },
                onNavigateToNextTask = { nextTaskId ->
                    navController.popBackStack()
                    navController.navigate(Screen.Timer.createRoute(nextTaskId))
                }
            )
        }
    }
}
