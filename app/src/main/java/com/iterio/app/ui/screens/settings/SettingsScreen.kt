package com.iterio.app.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iterio.app.R
import com.iterio.app.domain.model.BgmTracks
import com.iterio.app.ui.components.IterioTopBar
import com.iterio.app.ui.components.PremiumStatusCard
import com.iterio.app.ui.components.PremiumUpgradeCard
import com.iterio.app.ui.screens.settings.components.BgmSettingsContent
import com.iterio.app.ui.screens.settings.components.FocusModeSettingsContent
import com.iterio.app.ui.screens.settings.components.LanguageOption
import com.iterio.app.ui.screens.timer.components.BgmSelectorBottomSheet
import com.iterio.app.ui.screens.settings.components.PomodoroSettingsContent
import com.iterio.app.ui.screens.settings.components.ReviewSettingsContent
import com.iterio.app.ui.screens.settings.components.ReviewTaskManagementSection
import com.iterio.app.ui.screens.settings.components.ReviewTasksListDialog
import com.iterio.app.ui.screens.settings.components.SettingsInfoItem
import com.iterio.app.ui.screens.settings.components.SettingsPremiumSwitchItem
import com.iterio.app.ui.screens.settings.components.SettingsSection
import com.iterio.app.ui.theme.BackgroundDark
import com.iterio.app.ui.theme.SurfaceDark
import com.iterio.app.ui.theme.Teal700
import com.iterio.app.ui.theme.TextPrimary
import com.iterio.app.ui.theme.TextSecondary
import com.iterio.app.util.LocaleManager

@Composable
fun SettingsScreen(
    onNavigateToPremium: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToAllowedApps: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    val activity = LocalContext.current as? Activity

    Scaffold(
        topBar = {
            IterioTopBar(title = stringResource(R.string.settings_title))
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
            // Language Section
            SettingsSection(title = stringResource(R.string.settings_language)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .padding(8.dp)
                ) {
                    LanguageOption(
                        label = stringResource(R.string.settings_language_japanese),
                        selected = uiState.language == LocaleManager.LANGUAGE_JAPANESE,
                        onClick = {
                            viewModel.updateLanguage(LocaleManager.LANGUAGE_JAPANESE) {
                                activity?.recreate()
                            }
                        }
                    )
                    LanguageOption(
                        label = stringResource(R.string.settings_language_english),
                        selected = uiState.language == LocaleManager.LANGUAGE_ENGLISH,
                        onClick = {
                            viewModel.updateLanguage(LocaleManager.LANGUAGE_ENGLISH) {
                                activity?.recreate()
                            }
                        }
                    )
                }
            }

            // Notifications Section
            SettingsSection(title = stringResource(R.string.settings_notifications)) {
                com.iterio.app.ui.screens.settings.components.SettingsSwitchItem(
                    title = stringResource(R.string.settings_notifications_enabled),
                    description = stringResource(R.string.settings_notifications_desc),
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = viewModel::toggleNotifications
                )
            }

            // Review Section
            SettingsSection(title = stringResource(R.string.settings_review)) {
                ReviewSettingsContent(
                    reviewIntervalsEnabled = uiState.reviewIntervalsEnabled,
                    defaultReviewCount = uiState.defaultReviewCount,
                    isPremium = isPremium,
                    onReviewIntervalsToggle = viewModel::toggleReviewIntervals,
                    onReviewCountChange = viewModel::updateDefaultReviewCount,
                    onPremiumClick = onNavigateToPremium
                )
            }

            // Review Task Management Section
            SettingsSection(title = stringResource(R.string.settings_review_task_management)) {
                ReviewTaskManagementSection(
                    totalCount = uiState.reviewTaskTotalCount,
                    incompleteCount = uiState.reviewTaskIncompleteCount,
                    onShowList = { viewModel.showReviewTasksDialog() },
                    onDeleteAll = { viewModel.showDeleteAllReviewTasksDialog() }
                )
            }

            // Pomodoro Timer Section
            SettingsSection(title = stringResource(R.string.settings_pomodoro)) {
                PomodoroSettingsContent(
                    workDurationMinutes = uiState.workDurationMinutes,
                    shortBreakMinutes = uiState.shortBreakMinutes,
                    longBreakMinutes = uiState.longBreakMinutes,
                    cyclesBeforeLongBreak = uiState.cyclesBeforeLongBreak,
                    onWorkDurationChange = viewModel::updateWorkDuration,
                    onShortBreakChange = viewModel::updateShortBreak,
                    onLongBreakChange = viewModel::updateLongBreak,
                    onCyclesChange = viewModel::updateCycles
                )
            }

            // Focus Mode Section
            SettingsSection(title = stringResource(R.string.settings_focus_mode)) {
                FocusModeSettingsContent(
                    focusModeEnabled = uiState.focusModeEnabled,
                    focusModeStrict = uiState.focusModeStrict,
                    allowedAppsCount = uiState.allowedAppsCount,
                    isPremium = isPremium,
                    onFocusModeToggle = viewModel::toggleFocusMode,
                    onStrictModeToggle = viewModel::toggleFocusModeStrict,
                    onNavigateToAllowedApps = onNavigateToAllowedApps,
                    onPremiumClick = onNavigateToPremium
                )
            }

            // Timer Auto-Loop Section
            SettingsSection(title = stringResource(R.string.settings_timer)) {
                SettingsPremiumSwitchItem(
                    title = stringResource(R.string.settings_auto_loop),
                    description = stringResource(R.string.settings_auto_loop_desc),
                    checked = uiState.autoLoopEnabled,
                    isPremium = isPremium,
                    onCheckedChange = { checked ->
                        if (isPremium) {
                            viewModel.toggleAutoLoop(checked)
                        }
                    },
                    onPremiumClick = onNavigateToPremium
                )
            }

            // BGM Section
            SettingsSection(title = stringResource(R.string.settings_bgm)) {
                BgmSettingsContent(
                    selectedTrackId = uiState.bgmTrackId,
                    volume = uiState.bgmVolume,
                    autoPlayEnabled = uiState.bgmAutoPlayEnabled,
                    isPremium = isPremium,
                    onTrackSelect = { viewModel.showBgmSelector() },
                    onVolumeChange = { viewModel.updateBgmVolume(it) },
                    onAutoPlayToggle = { viewModel.toggleBgmAutoPlay(it) },
                    onPremiumClick = onNavigateToPremium
                )
            }

            // Premium Section
            SettingsSection(title = stringResource(R.string.settings_premium)) {
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
            SettingsSection(title = stringResource(R.string.settings_data_management)) {
                BackupNavigationItem(onClick = onNavigateToBackup)
            }

            // About Section
            SettingsSection(title = stringResource(R.string.settings_about)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsInfoItem(
                        label = stringResource(R.string.settings_version),
                        value = "1.0.0"
                    )
                    SettingsInfoItem(
                        label = stringResource(R.string.settings_build),
                        value = "Phase 1"
                    )
                }
            }
        }
    }

    // Review Tasks List Dialog
    if (uiState.showReviewTasksDialog) {
        ReviewTasksListDialog(
            reviewTasks = uiState.reviewTasks,
            selectedTaskIds = uiState.selectedReviewTaskIds,
            onToggleSelection = { viewModel.toggleReviewTaskSelection(it) },
            onSelectAll = { viewModel.selectAllReviewTasks() },
            onClearSelection = { viewModel.clearReviewTaskSelection() },
            onDeleteSelected = { viewModel.showDeleteSelectedReviewTasksDialog() },
            onDismiss = { viewModel.hideReviewTasksDialog() }
        )
    }

    // Delete Selected Confirmation Dialog
    if (uiState.showDeleteSelectedReviewTasksDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteSelectedReviewTasksDialog() },
            title = {
                Text(
                    text = stringResource(R.string.settings_review_task_delete_selected_confirm),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.settings_review_task_delete_selected_message,
                        uiState.selectedReviewTaskIds.size
                    ),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteSelectedReviewTasks() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteSelectedReviewTasksDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Delete All Confirmation Dialog
    if (uiState.showDeleteAllReviewTasksDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteAllReviewTasksDialog() },
            title = {
                Text(
                    text = stringResource(R.string.settings_review_task_delete_all_confirm),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.settings_review_task_delete_all_message,
                        uiState.reviewTaskTotalCount
                    ),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteAllReviewTasks() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteAllReviewTasksDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = SurfaceDark
        )
    }

    // BGM Selector BottomSheet
    if (uiState.showBgmSelectorDialog) {
        BgmSelectorBottomSheet(
            tracks = BgmTracks.all,
            selectedTrack = uiState.bgmTrackId?.let { BgmTracks.getById(it) },
            volume = uiState.bgmVolume,
            onSelectTrack = { viewModel.selectBgmTrack(it) },
            onVolumeChange = { viewModel.updateBgmVolume(it) },
            onDismiss = { viewModel.hideBgmSelector() }
        )
    }
}

@Composable
private fun BackupNavigationItem(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                    text = stringResource(R.string.settings_backup),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.settings_backup_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = stringResource(R.string.details),
            tint = TextSecondary
        )
    }
}
