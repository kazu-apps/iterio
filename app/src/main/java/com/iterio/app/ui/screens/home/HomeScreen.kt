package com.iterio.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iterio.app.R
import com.iterio.app.ui.components.LoadingIndicator
import com.iterio.app.ui.components.IterioCard
import com.iterio.app.ui.components.IterioTopBar
import com.iterio.app.ui.screens.home.components.ActiveTimerBar
import com.iterio.app.ui.screens.home.components.TodayReviewSection
import com.iterio.app.ui.screens.home.components.TodayTasksSection
import com.iterio.app.ui.screens.home.components.UpcomingDeadlinesSection
import com.iterio.app.ui.screens.home.components.WeeklyMiniChart

@Composable
fun HomeScreen(
    onNavigateToTimer: (Long) -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToReviewSchedule: () -> Unit = {},
    onNavigateToDeadlineList: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            IterioTopBar(title = "Iterio")
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Active Timer Bar
                ActiveTimerBar(
                    timerState = uiState.activeTimerState,
                    onNavigateToTimer = onNavigateToTimer
                )

                // Today's Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Study Time Card
                    IterioCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatMinutes(uiState.todayMinutes),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.home_today_study),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Cycles Card
                    IterioCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${uiState.todayCycles}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.home_cycles),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Streak Card
                    IterioCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${uiState.currentStreak}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.home_streak),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Today's Tasks Section
                TodayTasksSection(
                    tasks = uiState.todayScheduledTasks,
                    onStartTimer = onNavigateToTimer,
                    onNavigateToTasks = onNavigateToTasks
                )

                // Upcoming Deadlines Section
                UpcomingDeadlinesSection(
                    taskDeadlines = uiState.upcomingTaskDeadlines,
                    groupDeadlines = uiState.upcomingGroupDeadlines,
                    totalDeadlineCount = uiState.totalDeadlineCount,
                    onStartTimer = onNavigateToTimer,
                    onViewAll = onNavigateToDeadlineList
                )

                // Today's Review Section
                TodayReviewSection(
                    reviewTasks = uiState.todayReviewTasks,
                    onToggleComplete = viewModel::toggleReviewTaskComplete,
                    onViewAll = onNavigateToReviewSchedule
                )

                // Weekly Mini Chart
                if (uiState.weeklyData.isNotEmpty()) {
                    WeeklyMiniChart(weeklyData = uiState.weeklyData)
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    return if (minutes >= 60) {
        val hours = minutes / 60
        val mins = minutes % 60
        if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
    } else {
        "${minutes}m"
    }
}
