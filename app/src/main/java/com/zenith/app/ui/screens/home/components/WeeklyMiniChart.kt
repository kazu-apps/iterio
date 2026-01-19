package com.zenith.app.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenith.app.domain.repository.DayStats
import com.zenith.app.ui.components.ZenithCard
import com.zenith.app.ui.theme.AccentTeal
import com.zenith.app.ui.theme.SurfaceVariantDark
import java.time.LocalDate

/**
 * 週間学習時間のミニ棒グラフ（月〜日）
 */
@Composable
fun WeeklyMiniChart(
    weeklyData: List<DayStats>,
    modifier: Modifier = Modifier
) {
    val maxMinutes = weeklyData.maxOfOrNull { it.minutes } ?: 1
    val today = LocalDate.now()

    ZenithCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "今週の学習",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyData.forEach { dayStats ->
                    DayBar(
                        dayStats = dayStats,
                        maxMinutes = maxMinutes,
                        isToday = dayStats.date == today,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBar(
    dayStats: DayStats,
    maxMinutes: Int,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val barHeight = 80.dp
    val fillRatio = if (maxMinutes > 0) dayStats.minutes.toFloat() / maxMinutes else 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        // 学習時間（分）
        Text(
            text = if (dayStats.minutes > 0) "${dayStats.minutes}" else "",
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) AccentTeal else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 棒グラフ
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceVariantDark),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(barHeight * fillRatio)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isToday) AccentTeal else AccentTeal.copy(alpha = 0.6f))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 曜日ラベル
        Text(
            text = dayStats.dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) AccentTeal else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
