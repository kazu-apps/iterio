package com.zenith.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zenith.app.ui.screens.calendar.CalendarScreen
import com.zenith.app.ui.screens.home.HomeScreen
import com.zenith.app.ui.screens.backup.BackupScreen
import com.zenith.app.ui.screens.premium.PremiumScreen
import com.zenith.app.ui.screens.settings.SettingsScreen
import com.zenith.app.ui.screens.stats.StatsScreen
import com.zenith.app.ui.screens.tasks.TasksScreen
import com.zenith.app.ui.screens.timer.TimerScreen

@Composable
fun ZenithNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
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
            CalendarScreen()
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
                }
            )
        }
    }
}
