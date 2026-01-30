package com.iterio.app.ui.screens.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iterio.app.R
import com.iterio.app.domain.model.DailyStats
import com.iterio.app.ui.theme.HeatmapColors
import com.iterio.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun WeekdayHeader(modifier: Modifier = Modifier) {
    val weekdays = listOf(
        stringResource(R.string.calendar_day_sun),
        stringResource(R.string.calendar_day_mon),
        stringResource(R.string.calendar_day_tue),
        stringResource(R.string.calendar_day_wed),
        stringResource(R.string.calendar_day_thu),
        stringResource(R.string.calendar_day_fri),
        stringResource(R.string.calendar_day_sat)
    )

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
fun CalendarGrid(
    yearMonth: YearMonth,
    dailyStats: Map<LocalDate, DailyStats>,
    taskCountByDate: Map<LocalDate, Int>,
    groupColorsByDate: Map<LocalDate, List<String>>,
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
                        val taskCount = taskCountByDate[date] ?: 0
                        val taskHeatmapLevel = calculateTaskHeatmapLevel(taskCount)

                        DayCell(
                            date = date,
                            heatmapColor = HeatmapColors[taskHeatmapLevel],
                            taskCount = taskCount,
                            groupColors = groupColorsByDate[date].orEmpty(),
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

/**
 * 無料版用：グレースケールのシンプルなカレンダーグリッド
 */
@Composable
fun CalendarGridSimple(
    yearMonth: YearMonth,
    dailyStats: Map<LocalDate, DailyStats>,
    taskCountByDate: Map<LocalDate, Int>,
    groupColorsByDate: Map<LocalDate, List<String>>,
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
                        val taskCount = taskCountByDate[date] ?: 0

                        DayCellSimple(
                            date = date,
                            hasStudied = hasStudied,
                            taskCount = taskCount,
                            groupColors = groupColorsByDate[date].orEmpty(),
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

internal fun calculateHeatmapLevel(minutes: Int): Int {
    return when {
        minutes == 0 -> 0
        minutes < 30 -> 1
        minutes < 60 -> 2
        minutes < 120 -> 3
        else -> 4
    }
}

internal fun calculateTaskHeatmapLevel(taskCount: Int): Int {
    return when {
        taskCount == 0 -> 0
        taskCount == 1 -> 1
        taskCount == 2 -> 2
        taskCount <= 4 -> 3
        else -> 4
    }
}
