package com.zenith.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenith.app.domain.model.PremiumFeature
import com.zenith.app.ui.components.BlurredPremiumContent
import com.zenith.app.ui.components.LoadingIndicator
import com.zenith.app.ui.components.ZenithCard
import com.zenith.app.ui.components.ZenithTopBar
import com.zenith.app.ui.premium.PremiumUpsellDialog
import com.zenith.app.ui.theme.AccentTeal
import com.zenith.app.ui.theme.AccentWarning
import com.zenith.app.ui.theme.BackgroundDark
import com.zenith.app.ui.theme.Teal700
import com.zenith.app.ui.theme.TextPrimary
import com.zenith.app.ui.theme.TextSecondary

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    var showPremiumUpsellDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ZenithTopBar(title = "統計")
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Today's Study Time Card (無料版でも表示・大きめ)
                TodayStudyCard(
                    minutes = uiState.todayMinutes,
                    sessions = uiState.todaySessions
                )

                // Streak Card (無料版でも表示)
                StatCard(
                    title = "連続学習",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = AccentWarning
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatValue(
                            value = "${uiState.currentStreak}日",
                            label = "現在の連続"
                        )
                        StatValue(
                            value = "${uiState.maxStreak}日",
                            label = "最高記録"
                        )
                    }
                }

                // Premium Stats with Blur Effect
                BlurredPremiumContent(
                    isPremium = isPremium,
                    onPremiumClick = { showPremiumUpsellDialog = true }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // 学習時間統計
                        StatCard(
                            title = "学習時間",
                            icon = Icons.Default.Schedule,
                            iconTint = AccentTeal
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatValue(
                                    value = formatMinutes(uiState.thisWeekMinutes),
                                    label = "今週"
                                )
                                StatValue(
                                    value = formatMinutes(uiState.thisMonthMinutes),
                                    label = "今月"
                                )
                                StatValue(
                                    value = formatMinutes(uiState.averageDailyMinutes),
                                    label = "1日平均"
                                )
                            }
                        }

                        // Sessions Card
                        StatCard(
                            title = "学習セッション",
                            icon = Icons.Default.TrendingUp,
                            iconTint = Teal700
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                StatValue(
                                    value = "${uiState.totalSessions}",
                                    label = "総セッション数"
                                )
                            }
                        }

                        // Weekly Chart
                        WeeklyChart(weeklyData = uiState.weeklyData)
                    }
                }
            }
        }
    }

    // Premium誘導ダイアログ
    if (showPremiumUpsellDialog) {
        PremiumUpsellDialog(
            feature = PremiumFeature.DETAILED_STATS,
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
private fun TodayStudyCard(
    minutes: Int,
    sessions: Int,
    modifier: Modifier = Modifier
) {
    ZenithCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = AccentTeal,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "今日の学習時間",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatMinutes(minutes),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = AccentTeal
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${sessions}セッション完了",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ZenithCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            content()
        }
    }
}

@Composable
private fun StatValue(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AccentTeal
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun WeeklyChart(
    weeklyData: List<DayStats>,
    modifier: Modifier = Modifier
) {
    ZenithCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "週間学習グラフ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val maxMinutes = weeklyData.maxOfOrNull { it.minutes }?.coerceAtLeast(60) ?: 60

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEach { dayData ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Minutes label
                        if (dayData.minutes > 0) {
                            Text(
                                text = "${dayData.minutes}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Bar
                        val barHeight = if (maxMinutes > 0) {
                            (dayData.minutes.toFloat() / maxMinutes * 100).coerceIn(4f, 100f)
                        } else {
                            4f
                        }

                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(if (dayData.minutes > 0) barHeight.dp else 4.dp)
                                .background(
                                    color = if (dayData.minutes > 0) AccentTeal else TextSecondary.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Day label
                        Text(
                            text = dayData.dayOfWeek,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}
