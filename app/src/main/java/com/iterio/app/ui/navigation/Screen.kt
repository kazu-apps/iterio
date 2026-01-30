package com.iterio.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.iterio.app.R

sealed class Screen(
    val route: String,
    @StringRes val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        titleResId = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Tasks : Screen(
        route = "tasks",
        titleResId = R.string.nav_tasks,
        selectedIcon = Icons.Filled.Assignment,
        unselectedIcon = Icons.Outlined.Assignment
    )

    data object Calendar : Screen(
        route = "calendar",
        titleResId = R.string.nav_calendar,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    data object Stats : Screen(
        route = "stats",
        titleResId = R.string.nav_stats,
        selectedIcon = Icons.Filled.InsertChart,
        unselectedIcon = Icons.Outlined.InsertChart
    )

    data object Settings : Screen(
        route = "settings",
        titleResId = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object Timer : Screen(
        route = "timer/{taskId}",
        titleResId = R.string.timer_title,
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer
    ) {
        fun createRoute(taskId: Long): String {
            return "timer/$taskId"
        }
    }

    data object Premium : Screen(
        route = "premium",
        titleResId = R.string.settings_premium,
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.Star
    )

    data object Backup : Screen(
        route = "backup",
        titleResId = R.string.settings_backup,
        selectedIcon = Icons.Filled.Backup,
        unselectedIcon = Icons.Outlined.Backup
    )

    data object AllowedApps : Screen(
        route = "allowed_apps",
        titleResId = R.string.settings_allowed_apps,
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps
    )

    data object ReviewSchedule : Screen(
        route = "review_schedule",
        titleResId = R.string.review_schedule_title,
        selectedIcon = Icons.Filled.EventRepeat,
        unselectedIcon = Icons.Outlined.EventRepeat
    )

    data object DeadlineList : Screen(
        route = "deadline_list",
        titleResId = R.string.deadline_list_title,
        selectedIcon = Icons.Filled.DateRange,
        unselectedIcon = Icons.Outlined.DateRange
    )

    companion object {
        val bottomNavItems = listOf(Home, Tasks, Calendar, Stats, Settings)
    }
}
