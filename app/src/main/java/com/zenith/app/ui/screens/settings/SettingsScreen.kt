package com.zenith.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenith.app.ui.components.PremiumStatusCard
import com.zenith.app.ui.components.PremiumUpgradeCard
import com.zenith.app.ui.components.ZenithCard
import com.zenith.app.ui.components.ZenithTopBar
import com.zenith.app.ui.theme.BackgroundDark
import com.zenith.app.ui.theme.SurfaceVariantDark
import com.zenith.app.ui.theme.Teal700
import com.zenith.app.ui.theme.TextPrimary
import com.zenith.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onNavigateToPremium: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ZenithTopBar(title = "設定")
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notifications Section
            SettingsSection(title = "通知") {
                SettingsSwitchItem(
                    title = "リマインダー通知",
                    description = "学習リマインダーと復習通知を受け取る",
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = viewModel::toggleNotifications
                )
            }

            // Goals Section
            SettingsSection(title = "目標") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1日の学習目標",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${uiState.dailyGoalMinutes}分",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = uiState.dailyGoalMinutes.toFloat(),
                        onValueChange = { viewModel.updateDailyGoal(it.toInt()) },
                        valueRange = 15f..180f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = Teal700,
                            activeTrackColor = Teal700,
                            inactiveTrackColor = SurfaceVariantDark
                        )
                    )
                }
            }

            // Review Section
            SettingsSection(title = "復習") {
                SettingsSwitchItem(
                    title = "自動復習スケジュール",
                    description = "エビングハウス忘却曲線に基づく復習タスクを自動生成",
                    checked = uiState.reviewIntervalsEnabled,
                    onCheckedChange = viewModel::toggleReviewIntervals
                )
            }

            // Pomodoro Timer Section
            SettingsSection(title = "ポモドーロタイマー") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "作業時間",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (uiState.workDurationMinutes > 1) viewModel.updateWorkDuration(uiState.workDurationMinutes - 1) }
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "1分減らす",
                                tint = Teal700
                            )
                        }
                        Text(
                            text = "${uiState.workDurationMinutes}分",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(
                            onClick = { if (uiState.workDurationMinutes < 180) viewModel.updateWorkDuration(uiState.workDurationMinutes + 1) }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "1分増やす",
                                tint = Teal700
                            )
                        }
                    }
                    Slider(
                        value = uiState.workDurationMinutes.toFloat(),
                        onValueChange = { viewModel.updateWorkDuration(it.toInt()) },
                        valueRange = 1f..180f,
                        steps = 178,
                        colors = SliderDefaults.colors(
                            thumbColor = Teal700,
                            activeTrackColor = Teal700,
                            inactiveTrackColor = SurfaceVariantDark
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1分", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("180分", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }

                    Text(
                        text = "短休憩",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "${uiState.shortBreakMinutes}分",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = uiState.shortBreakMinutes.toFloat(),
                        onValueChange = { viewModel.updateShortBreak(it.toInt()) },
                        valueRange = 3f..15f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = Teal700,
                            activeTrackColor = Teal700,
                            inactiveTrackColor = SurfaceVariantDark
                        )
                    )

                    Text(
                        text = "長休憩",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "${uiState.longBreakMinutes}分",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = uiState.longBreakMinutes.toFloat(),
                        onValueChange = { viewModel.updateLongBreak(it.toInt()) },
                        valueRange = 10f..30f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = Teal700,
                            activeTrackColor = Teal700,
                            inactiveTrackColor = SurfaceVariantDark
                        )
                    )

                    Text(
                        text = "長休憩までのサイクル数",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "${uiState.cyclesBeforeLongBreak}サイクル",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = uiState.cyclesBeforeLongBreak.toFloat(),
                        onValueChange = { viewModel.updateCycles(it.toInt()) },
                        valueRange = 2f..6f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = Teal700,
                            activeTrackColor = Teal700,
                            inactiveTrackColor = SurfaceVariantDark
                        )
                    )
                }
            }

            // Focus Mode Section
            SettingsSection(title = "フォーカスモード") {
                SettingsSwitchItem(
                    title = "フォーカスモードを有効化",
                    description = "タイマー中はスマートフォンの使用を制限",
                    checked = uiState.focusModeEnabled,
                    onCheckedChange = viewModel::toggleFocusMode
                )
                if (uiState.focusModeEnabled) {
                    SettingsSwitchItem(
                        title = "完全ロックモード（デフォルト）",
                        description = "タイマー開始時に完全ロックをデフォルトで有効にする",
                        checked = uiState.focusModeStrict,
                        onCheckedChange = viewModel::toggleFocusModeStrict
                    )
                }
            }

            // Premium Section
            SettingsSection(title = "Premium") {
                if (isPremium) {
                    PremiumStatusCard(
                        isPremium = true,
                        isInTrialPeriod = subscriptionStatus.isInTrialPeriod,
                        daysRemainingInTrial = subscriptionStatus.daysRemainingInTrial,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    PremiumUpgradeCard(
                        onStartTrial = { viewModel.startTrial() },
                        onUpgrade = onNavigateToPremium,
                        trialAvailable = subscriptionStatus.canStartTrial,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Backup Section
            SettingsSection(title = "データ管理") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToBackup() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Backup,
                            contentDescription = null,
                            tint = Teal700,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "バックアップ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "データのエクスポート・インポート",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "詳細",
                        tint = TextSecondary
                    )
                }
            }

            // About Section
            SettingsSection(title = "アプリについて") {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsInfoItem(label = "バージョン", value = "1.0.0")
                    SettingsInfoItem(label = "ビルド", value = "Phase 1")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Teal700,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ZenithCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Teal700,
                checkedTrackColor = Teal700.copy(alpha = 0.5f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceVariantDark
            )
        )
    }
}

@Composable
private fun SettingsInfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}
