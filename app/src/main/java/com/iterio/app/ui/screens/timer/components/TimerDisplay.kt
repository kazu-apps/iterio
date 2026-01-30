package com.iterio.app.ui.screens.timer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iterio.app.service.TimerPhase

@Composable
internal fun CircularTimer(
    timeRemainingSeconds: Int,
    totalTimeSeconds: Int,
    phase: TimerPhase,
    timerSize: Dp = 280.dp
) {
    val progress = if (totalTimeSeconds > 0) {
        timeRemainingSeconds.toFloat() / totalTimeSeconds.toFloat()
    } else {
        1f
    }

    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60

    val primaryColor = when (phase) {
        TimerPhase.WORK -> MaterialTheme.colorScheme.primary
        TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
        TimerPhase.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    val scaleFactor = timerSize.value / 280f

    Box(
        modifier = Modifier.size(timerSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val topLeft = Offset(
                (size.width - radius * 2) / 2,
                (size.height - radius * 2) / 2
            )

            // Background circle
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress circle
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = (64 * scaleFactor).sp,
                fontWeight = FontWeight.Light
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
