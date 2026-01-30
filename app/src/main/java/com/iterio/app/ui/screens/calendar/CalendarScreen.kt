package com.iterio.app.ui.screens.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iterio.app.R
import com.iterio.app.domain.model.PremiumFeature
import com.iterio.app.ui.components.IterioCard
import com.iterio.app.ui.components.IterioTopBar
import com.iterio.app.ui.components.LockedFeatureCard
import com.iterio.app.ui.premium.PremiumUpsellDialog
import com.iterio.app.ui.screens.calendar.components.CalendarGrid
import com.iterio.app.ui.screens.calendar.components.CalendarGridSimple
import com.iterio.app.ui.screens.calendar.components.HeatmapLegend
import com.iterio.app.ui.screens.calendar.components.MonthHeader
import com.iterio.app.ui.screens.calendar.components.SelectedDateInfo
import com.iterio.app.ui.screens.calendar.components.WeekdayHeader
import com.iterio.app.ui.theme.BackgroundDark

@Composable
fun CalendarScreen(
    onStartTimer: (Long) -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    var showPremiumUpsellDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            IterioTopBar(title = stringResource(R.string.calendar_title))
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Month navigation
            MonthHeader(
                yearMonth = uiState.currentMonth,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth
            )

            if (isPremium) {
                // Premium: ヒートマップ表示
                IterioCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WeekdayHeader()
                        CalendarGrid(
                            yearMonth = uiState.currentMonth,
                            dailyStats = uiState.dailyStats,
                            taskCountByDate = uiState.taskCountByDate,
                            groupColorsByDate = uiState.groupColorsByDate,
                            selectedDate = uiState.selectedDate,
                            onDateClick = viewModel::selectDate
                        )
                    }
                }

                HeatmapLegend()
            } else {
                // 無料版: リスト表示 + Premium誘導
                IterioCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WeekdayHeader()
                        // グレースケールのカレンダー表示
                        CalendarGridSimple(
                            yearMonth = uiState.currentMonth,
                            dailyStats = uiState.dailyStats,
                            taskCountByDate = uiState.taskCountByDate,
                            groupColorsByDate = uiState.groupColorsByDate,
                            selectedDate = uiState.selectedDate,
                            onDateClick = viewModel::selectDate
                        )
                    }
                }

                // Premium誘導カード
                LockedFeatureCard(
                    feature = PremiumFeature.CALENDAR_HEATMAP,
                    onClick = { showPremiumUpsellDialog = true },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Selected date info
            uiState.selectedDate?.let { date ->
                val stats = uiState.dailyStats[date]
                SelectedDateInfo(
                    date = date,
                    stats = stats,
                    tasks = uiState.selectedDateTasks,
                    reviewTasks = uiState.selectedDateReviewTasks,
                    onStartTimer = onStartTimer,
                    onToggleReviewTaskComplete = viewModel::toggleReviewTaskComplete
                )
            }
        }
    }

    // Premium誘導ダイアログ
    if (showPremiumUpsellDialog) {
        PremiumUpsellDialog(
            feature = PremiumFeature.CALENDAR_HEATMAP,
            onDismiss = { showPremiumUpsellDialog = false },
            onStartTrial = {
                viewModel.startTrial()
                showPremiumUpsellDialog = false
            },
            onUpgrade = {
                showPremiumUpsellDialog = false
            },
            trialAvailable = subscriptionStatus.canStartTrial
        )
    }
}
