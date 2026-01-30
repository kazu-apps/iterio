package com.iterio.app.ui.screens.timer

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iterio.app.domain.model.PremiumFeature
import com.iterio.app.service.FocusModeService
import com.iterio.app.ui.premium.PremiumUpsellDialog
import com.iterio.app.ui.screens.timer.components.AllowedAppsSelectorBottomSheet
import com.iterio.app.ui.screens.timer.components.BgmButton
import com.iterio.app.ui.screens.timer.components.BgmSelectorBottomSheet
import com.iterio.app.ui.screens.timer.components.CancelConfirmDialog
import com.iterio.app.ui.screens.timer.components.CircularTimer
import com.iterio.app.ui.screens.timer.components.FinishDialog
import com.iterio.app.ui.screens.timer.components.FocusModeWarningDialog
import com.iterio.app.ui.screens.timer.components.PhaseIndicator
import com.iterio.app.ui.screens.timer.components.StrictModeBlockedDialog
import com.iterio.app.ui.screens.timer.components.TimerControls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    taskId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToNextTask: ((Long) -> Unit)? = null,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val bgmState by viewModel.bgmState.collectAsStateWithLifecycle()
    val selectedBgmTrack by viewModel.selectedBgmTrack.collectAsStateWithLifecycle()
    val bgmVolume by viewModel.bgmVolume.collectAsStateWithLifecycle()
    val isAccessibilityServiceRunning by FocusModeService.isServiceRunning.collectAsStateWithLifecycle()

    var showFocusModeWarning by remember { mutableStateOf(false) }
    var showBgmSelector by remember { mutableStateOf(false) }
    var showBgmPremiumUpsell by remember { mutableStateOf(false) }
    var showStrictModeBlockedDialog by remember { mutableStateOf(false) }
    var showPremiumUpsellDialog by remember { mutableStateOf(false) }
    var showAutoLoopPremiumUpsell by remember { mutableStateOf(false) }
    var showAllowedAppsSelector by remember { mutableStateOf(false) }

    // Session-specific settings (initialized from default settings)
    var sessionLockModeEnabled by remember(uiState.settings.focusModeStrict) {
        mutableStateOf(uiState.settings.focusModeStrict)
    }
    var sessionCycleCount by remember(uiState.totalCycles) {
        mutableStateOf(uiState.totalCycles)
    }
    var sessionAutoLoopEnabled by remember(uiState.settings.autoLoopEnabled) {
        mutableStateOf(uiState.settings.autoLoopEnabled)
    }
    var sessionAllowedApps by remember(uiState.defaultAllowedApps) {
        mutableStateOf(uiState.defaultAllowedApps)
    }

    // Lock mode active check
    val isLockModeActive = uiState.settings.focusModeEnabled &&
            uiState.isSessionLockModeEnabled &&
            (uiState.isRunning || uiState.isPaused)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.task?.name ?: "タイマー") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isRunning || uiState.isPaused) {
                            if (isLockModeActive) {
                                showStrictModeBlockedDialog = true
                            } else {
                                viewModel.showCancelDialog()
                            }
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val onStartTimer: () -> Unit = {
            if (uiState.settings.focusModeEnabled) {
                showFocusModeWarning = true
            } else {
                viewModel.startTimer(lockModeEnabled = false)
            }
        }

        val onBgmClick: () -> Unit = {
            if (isPremium) {
                showBgmSelector = true
            } else {
                showBgmPremiumUpsell = true
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularTimer(
                    timeRemainingSeconds = uiState.timeRemainingSeconds,
                    totalTimeSeconds = uiState.totalTimeSeconds,
                    phase = uiState.phase,
                    timerSize = 200.dp
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PhaseIndicator(
                        phase = uiState.phase,
                        currentCycle = uiState.currentCycle,
                        totalCycles = uiState.totalCycles
                    )

                    TimerControls(
                        phase = uiState.phase,
                        isRunning = uiState.isRunning,
                        isPaused = uiState.isPaused,
                        focusModeEnabled = uiState.settings.focusModeEnabled,
                        isLockModeActive = isLockModeActive,
                        onStart = onStartTimer,
                        onPause = { viewModel.pauseTimer() },
                        onResume = { viewModel.resumeTimer() },
                        onSkip = { viewModel.skipPhase() },
                        onStop = { viewModel.showCancelDialog() }
                    )

                    if (uiState.totalWorkMinutes > 0) {
                        Text(
                            text = "今回の学習: ${uiState.totalWorkMinutes}分",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    BgmButton(
                        selectedTrack = selectedBgmTrack,
                        isPlaying = bgmState.isPlaying,
                        isPremium = isPremium,
                        onClick = onBgmClick,
                        onTogglePlay = { viewModel.toggleBgm() }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                PhaseIndicator(
                    phase = uiState.phase,
                    currentCycle = uiState.currentCycle,
                    totalCycles = uiState.totalCycles
                )

                CircularTimer(
                    timeRemainingSeconds = uiState.timeRemainingSeconds,
                    totalTimeSeconds = uiState.totalTimeSeconds,
                    phase = uiState.phase
                )

                TimerControls(
                    phase = uiState.phase,
                    isRunning = uiState.isRunning,
                    isPaused = uiState.isPaused,
                    focusModeEnabled = uiState.settings.focusModeEnabled,
                    isLockModeActive = isLockModeActive,
                    onStart = onStartTimer,
                    onPause = { viewModel.pauseTimer() },
                    onResume = { viewModel.resumeTimer() },
                    onSkip = { viewModel.skipPhase() },
                    onStop = { viewModel.showCancelDialog() }
                )

                if (uiState.totalWorkMinutes > 0) {
                    Text(
                        text = "今回の学習: ${uiState.totalWorkMinutes}分",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BgmButton(
                    selectedTrack = selectedBgmTrack,
                    isPlaying = bgmState.isPlaying,
                    isPremium = isPremium,
                    onClick = onBgmClick,
                    onTogglePlay = { viewModel.toggleBgm() }
                )
            }
        }
    }

    // Cancel confirmation dialog
    if (uiState.showCancelDialog) {
        CancelConfirmDialog(
            onDismiss = { viewModel.hideCancelDialog() },
            onConfirm = {
                viewModel.cancelTimer()
                onNavigateBack()
            }
        )
    }

    // Finish dialog
    if (uiState.showFinishDialog) {
        FinishDialog(
            completedCycles = uiState.currentCycle,
            totalWorkMinutes = uiState.totalWorkMinutes,
            nextTaskName = uiState.nextTaskName,
            allTasksCompleted = uiState.allTasksCompleted,
            onDismiss = {
                viewModel.hideFinishDialog()
                onNavigateBack()
            },
            onNavigateToNextTask = uiState.nextTaskId?.let { nextId ->
                {
                    viewModel.hideFinishDialog()
                    onNavigateToNextTask?.invoke(nextId)
                }
            }
        )
    }

    // Focus mode warning dialog
    if (showFocusModeWarning) {
        FocusModeWarningDialog(
            context = context,
            isPremium = isPremium,
            isAccessibilityServiceRunning = isAccessibilityServiceRunning,
            sessionLockModeEnabled = sessionLockModeEnabled,
            sessionCycleCount = sessionCycleCount,
            sessionAutoLoopEnabled = sessionAutoLoopEnabled,
            sessionAllowedApps = sessionAllowedApps,
            onLockModeToggle = { sessionLockModeEnabled = it },
            onCycleCountChange = { sessionCycleCount = it },
            onAutoLoopToggle = { sessionAutoLoopEnabled = it },
            onShowAllowedApps = { showAllowedAppsSelector = true },
            onShowPremiumUpsell = {
                showFocusModeWarning = false
                showPremiumUpsellDialog = true
            },
            onShowAutoLoopPremiumUpsell = {
                showFocusModeWarning = false
                showAutoLoopPremiumUpsell = true
            },
            onDismiss = { showFocusModeWarning = false },
            onConfirm = {
                showFocusModeWarning = false
                viewModel.startTimer(
                    lockModeEnabled = sessionLockModeEnabled,
                    cycleCount = sessionCycleCount,
                    autoLoopEnabled = sessionAutoLoopEnabled && isPremium,
                    allowedApps = sessionAllowedApps
                )
            }
        )
    }

    // Allowed apps selector
    if (showAllowedAppsSelector) {
        AllowedAppsSelectorBottomSheet(
            installedApps = uiState.installedApps,
            selectedPackages = sessionAllowedApps,
            isLoading = uiState.isLoadingApps,
            onSelectionChanged = { selectedPackages ->
                sessionAllowedApps = selectedPackages
                viewModel.updateAllowedApps(selectedPackages)
            },
            onDismiss = { showAllowedAppsSelector = false }
        )
    }

    // Strict mode blocked dialog
    if (showStrictModeBlockedDialog) {
        StrictModeBlockedDialog(
            onDismiss = { showStrictModeBlockedDialog = false }
        )
    }

    // Premium upsell dialogs
    if (showPremiumUpsellDialog) {
        PremiumUpsellDialog(
            feature = PremiumFeature.COMPLETE_LOCK_MODE,
            onDismiss = { showPremiumUpsellDialog = false },
            onStartTrial = {
                viewModel.startTrial()
                showPremiumUpsellDialog = false
            },
            onUpgrade = {
                showPremiumUpsellDialog = false
                onNavigateToPremium()
            },
            trialAvailable = subscriptionStatus.canStartTrial
        )
    }

    if (showBgmPremiumUpsell) {
        PremiumUpsellDialog(
            feature = PremiumFeature.BGM,
            onDismiss = { showBgmPremiumUpsell = false },
            onStartTrial = {
                viewModel.startTrial()
                showBgmPremiumUpsell = false
            },
            onUpgrade = {
                showBgmPremiumUpsell = false
                onNavigateToPremium()
            },
            trialAvailable = subscriptionStatus.canStartTrial
        )
    }

    if (showAutoLoopPremiumUpsell) {
        PremiumUpsellDialog(
            feature = PremiumFeature.TIMER_AUTO_LOOP,
            onDismiss = { showAutoLoopPremiumUpsell = false },
            onStartTrial = {
                viewModel.startTrial()
                showAutoLoopPremiumUpsell = false
            },
            onUpgrade = {
                showAutoLoopPremiumUpsell = false
                onNavigateToPremium()
            },
            trialAvailable = subscriptionStatus.canStartTrial
        )
    }

    // BGM selector
    if (showBgmSelector) {
        BgmSelectorBottomSheet(
            tracks = viewModel.getAvailableBgmTracks(),
            selectedTrack = selectedBgmTrack,
            volume = bgmVolume,
            onSelectTrack = { track -> viewModel.selectBgmTrack(track) },
            onVolumeChange = { volume -> viewModel.setBgmVolume(volume) },
            onDismiss = { showBgmSelector = false }
        )
    }
}
