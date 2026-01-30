package com.iterio.app.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iterio.app.R
import com.iterio.app.service.TimerPhase
import com.iterio.app.service.TimerState
import com.iterio.app.ui.theme.AccentTeal
import com.iterio.app.ui.theme.AccentWarning

@Composable
fun ActiveTimerBar(
    timerState: TimerState?,
    onNavigateToTimer: (Long) -> Unit
) {
    val visible = timerState != null && timerState.phase != TimerPhase.IDLE

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        if (timerState != null && timerState.phase != TimerPhase.IDLE) {
            ActiveTimerBarContent(
                timerState = timerState,
                onClick = { onNavigateToTimer(timerState.taskId) }
            )
        }
    }
}

@Composable
private fun ActiveTimerBarContent(
    timerState: TimerState,
    onClick: () -> Unit
) {
    val phaseColor = when (timerState.phase) {
        TimerPhase.WORK -> MaterialTheme.colorScheme.primary
        TimerPhase.SHORT_BREAK -> AccentTeal
        TimerPhase.LONG_BREAK -> AccentWarning
        TimerPhase.IDLE -> MaterialTheme.colorScheme.primary
    }

    val phaseText = when (timerState.phase) {
        TimerPhase.WORK -> stringResource(R.string.home_timer_phase_work)
        TimerPhase.SHORT_BREAK -> stringResource(R.string.home_timer_phase_short_break)
        TimerPhase.LONG_BREAK -> stringResource(R.string.home_timer_phase_long_break)
        TimerPhase.IDLE -> ""
    }

    val minutes = timerState.timeRemainingSeconds / 60
    val seconds = timerState.timeRemainingSeconds % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)

    val progress = if (timerState.totalTimeSeconds > 0) {
        1f - (timerState.timeRemainingSeconds.toFloat() / timerState.totalTimeSeconds)
    } else {
        0f
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phase icon
                Icon(
                    imageVector = if (timerState.isPaused) Icons.Default.Pause else Icons.Default.Timer,
                    contentDescription = null,
                    tint = phaseColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Task name + phase info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (timerState.taskName.isNotEmpty()) {
                        Text(
                            text = timerState.taskName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = phaseText,
                            style = MaterialTheme.typography.bodySmall,
                            color = phaseColor
                        )
                        if (timerState.isPaused) {
                            Text(
                                text = stringResource(R.string.home_timer_paused),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.home_timer_cycle_format,
                                timerState.currentCycle,
                                timerState.totalCycles
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Remaining time
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.home_timer_tap_to_open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = phaseColor,
                trackColor = phaseColor.copy(alpha = 0.2f)
            )
        }
    }
}
