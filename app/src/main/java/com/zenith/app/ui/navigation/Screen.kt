package com.zenith.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "ホーム",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Tasks : Screen(
        route = "tasks",
        title = "タスク",
        selectedIcon = Icons.Filled.Assignment,
        unselectedIcon = Icons.Outlined.Assignment
    )

    data object Calendar : Screen(
        route = "calendar",
        title = "カレンダー",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    data object Stats : Screen(
        route = "stats",
        title = "統計",
        selectedIcon = Icons.Filled.InsertChart,
        unselectedIcon = Icons.Outlined.InsertChart
    )

    data object Settings : Screen(
        route = "settings",
        title = "設定",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object Timer : Screen(
        route = "timer/{taskId}",
        title = "タイマー",
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer
    ) {
        fun createRoute(taskId: Long): String {
            return "timer/$taskId"
        }
    }

    data object Premium : Screen(
        route = "premium",
        title = "Premium",
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.Star
    )

    data object Backup : Screen(
        route = "backup",
        title = "バックアップ",
        selectedIcon = Icons.Filled.Backup,
        unselectedIcon = Icons.Outlined.Backup
    )

    companion object {
        val bottomNavItems = listOf(Home, Tasks, Calendar, Stats, Settings)
    }
}
