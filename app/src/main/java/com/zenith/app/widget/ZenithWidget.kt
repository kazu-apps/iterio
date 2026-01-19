package com.zenith.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zenith.app.R
import com.zenith.app.service.TimerPhase
import com.zenith.app.ui.MainActivity

class ZenithWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ZenithWidgetStateHelper.getWidgetState(context)

        provideContent {
            GlanceTheme {
                ZenithWidgetContent(context = context, state = state)
            }
        }
    }
}

@Composable
private fun ZenithWidgetContent(context: Context, state: WidgetState) {
    val backgroundColor = ColorProvider(R.color.widget_background)
    val primaryTextColor = ColorProvider(R.color.widget_text_primary)
    val secondaryTextColor = ColorProvider(R.color.widget_text_secondary)
    val accentColor = ColorProvider(R.color.teal_700)

    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(launchIntent))
            .padding(16.dp)
    ) {
        if (state.isPremium) {
            // Premium版: 通常表示
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // App title
                Text(
                    text = "ZENITH",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Study time
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = formatStudyTime(state.todayStudyMinutes),
                        style = TextStyle(
                            color = primaryTextColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "今日",
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Streak
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = "${state.currentStreak}日連続",
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 12.sp
                        )
                    )
                }

                // Timer status (if running)
                if (state.isTimerRunning || state.timerPhase != TimerPhase.IDLE) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    TimerStatusRow(state)
                }
            }
        } else {
            // 無料版: Premium誘導表示
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "ZENITH",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = "Premium機能",
                    style = TextStyle(
                        color = primaryTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = "タップしてアップグレード",
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun TimerStatusRow(state: WidgetState) {
    val statusText = when (state.timerPhase) {
        TimerPhase.WORK -> "作業中"
        TimerPhase.SHORT_BREAK -> "休憩中"
        TimerPhase.LONG_BREAK -> "長休憩"
        TimerPhase.IDLE -> ""
    }

    val statusColor = when (state.timerPhase) {
        TimerPhase.WORK -> ColorProvider(R.color.timer_work)
        TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> ColorProvider(R.color.timer_break)
        TimerPhase.IDLE -> ColorProvider(R.color.widget_text_secondary)
    }

    if (statusText.isNotEmpty()) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .cornerRadius(4.dp)
                    .background(statusColor)
            ) {}
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "$statusText ${formatTime(state.timeRemainingSeconds)}",
                style = TextStyle(
                    color = statusColor,
                    fontSize = 12.sp
                )
            )
        }
    }
}

private fun formatStudyTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
