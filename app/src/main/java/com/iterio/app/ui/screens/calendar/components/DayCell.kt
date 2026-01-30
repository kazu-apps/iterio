package com.iterio.app.ui.screens.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iterio.app.ui.theme.AccentTeal
import com.iterio.app.ui.theme.HeatmapColors
import com.iterio.app.ui.theme.TextPrimary
import com.iterio.app.ui.theme.TextSecondary
import java.time.LocalDate

@Composable
fun DayCell(
    date: LocalDate,
    heatmapColor: Color,
    taskCount: Int,
    groupColors: List<String>,
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (heatmapColor == HeatmapColors[0]) TextSecondary else TextPrimary,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            // Task dots (max 3) with group colors
            if (taskCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    val dotCount = minOf(taskCount, 3)
                    repeat(dotCount) { index ->
                        val dotColor = groupColors.getOrNull(index)
                            ?.let { parseColorSafe(it) }
                            ?: AccentTeal
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayCellSimple(
    date: LocalDate,
    hasStudied: Boolean,
    taskCount: Int,
    groupColors: List<String>,
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (hasStudied) TextPrimary else TextSecondary,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            // Task dots (max 3) with group colors
            if (taskCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    val dotCount = minOf(taskCount, 3)
                    repeat(dotCount) { index ->
                        val dotColor = groupColors.getOrNull(index)
                            ?.let { parseColorSafe(it) }
                            ?: AccentTeal
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

/**
 * カラーコード文字列を Color に安全にパース
 */
private fun parseColorSafe(colorHex: String): Color? {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        null
    }
}
