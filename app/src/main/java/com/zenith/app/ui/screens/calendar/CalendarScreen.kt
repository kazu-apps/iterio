package com.zenith.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenith.app.domain.model.DailyStats
import com.zenith.app.domain.model.PremiumFeature
import com.zenith.app.ui.components.LockedFeatureCard
import com.zenith.app.ui.components.ZenithCard
import com.zenith.app.ui.components.ZenithTopBar
import com.zenith.app.ui.premium.PremiumUpsellDialog
import com.zenith.app.ui.theme.AccentTeal
import com.zenith.app.ui.theme.BackgroundDark
import com.zenith.app.ui.theme.HeatmapColors
import com.zenith.app.ui.theme.TextPrimary
import com.zenith.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    var showPremiumUpsellDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ZenithTopBar(title = "カレンダー")
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                ZenithCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WeekdayHeader()
                        CalendarGrid(
                            yearMonth = uiState.currentMonth,
                            dailyStats = uiState.dailyStats,
                            selectedDate = uiState.selectedDate,
                            onDateClick = viewModel::selectDate
                        )
                    }
                }

                HeatmapLegend()
            } else {
                // 無料版: リスト表示 + Premium誘導
                ZenithCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WeekdayHeader()
                        // グレースケールのカレンダー表示
                        CalendarGridSimple(
                            yearMonth = uiState.currentMonth,
                            dailyStats = uiState.dailyStats,
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
                SelectedDateInfo(date = date, stats = stats)
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

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "前月",
                tint = TextPrimary
            )
        }

        Text(
            text = "${yearMonth.year}年${yearMonth.monthValue}月",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "翌月",
                tint = TextPrimary
            )
        }
    }
}

@Composable
private fun WeekdayHeader(modifier: Modifier = Modifier) {
    val weekdays = listOf("日", "月", "火", "水", "木", "金", "土")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    dailyStats: Map<LocalDate, DailyStats>,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0

    val days = mutableListOf<LocalDate?>()

    // Add empty slots for days before the first day of month
    repeat(firstDayOfWeek) { days.add(null) }

    // Add all days of the month
    var currentDay = firstDayOfMonth
    while (!currentDay.isAfter(lastDayOfMonth)) {
        days.add(currentDay)
        currentDay = currentDay.plusDays(1)
    }

    // Create rows of 7 days
    val weeks = days.chunked(7)

    Column(modifier = modifier) {
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { date ->
                    if (date != null) {
                        val stats = dailyStats[date]
                        val heatmapLevel = calculateHeatmapLevel(stats?.totalStudyMinutes ?: 0)

                        DayCell(
                            date = date,
                            heatmapColor = HeatmapColors[heatmapLevel],
                            isSelected = date == selectedDate,
                            isToday = date == LocalDate.now(),
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
                // Fill remaining cells if week is incomplete
                repeat(7 - week.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    heatmapColor: Color,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(heatmapColor)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, AccentTeal, RoundedCornerShape(4.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (heatmapColor == HeatmapColors[0]) TextSecondary else TextPrimary,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun HeatmapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "少",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        HeatmapColors.forEach { color ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }

        Text(
            text = "多",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun SelectedDateInfo(
    date: LocalDate,
    stats: DailyStats?,
    modifier: Modifier = Modifier
) {
    ZenithCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${date.monthValue}/${date.dayOfMonth}の学習",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (stats != null && stats.hasStudied) {
                Text(
                    text = "学習時間: ${stats.formattedTotalTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "セッション数: ${stats.sessionCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "この日は学習記録がありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private fun calculateHeatmapLevel(minutes: Int): Int {
    return when {
        minutes == 0 -> 0
        minutes < 30 -> 1
        minutes < 60 -> 2
        minutes < 120 -> 3
        else -> 4
    }
}

// 無料版用：グレースケールのシンプルなカレンダーグリッド
@Composable
private fun CalendarGridSimple(
    yearMonth: YearMonth,
    dailyStats: Map<LocalDate, DailyStats>,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

    val days = mutableListOf<LocalDate?>()
    repeat(firstDayOfWeek) { days.add(null) }

    var currentDay = firstDayOfMonth
    while (!currentDay.isAfter(lastDayOfMonth)) {
        days.add(currentDay)
        currentDay = currentDay.plusDays(1)
    }

    val weeks = days.chunked(7)

    Column(modifier = modifier) {
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { date ->
                    if (date != null) {
                        val stats = dailyStats[date]
                        val hasStudied = stats?.hasStudied == true

                        DayCellSimple(
                            date = date,
                            hasStudied = hasStudied,
                            isSelected = date == selectedDate,
                            isToday = date == LocalDate.now(),
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
                repeat(7 - week.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayCellSimple(
    date: LocalDate,
    hasStudied: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (hasStudied) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, AccentTeal, RoundedCornerShape(4.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (hasStudied) TextPrimary else TextSecondary,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}
