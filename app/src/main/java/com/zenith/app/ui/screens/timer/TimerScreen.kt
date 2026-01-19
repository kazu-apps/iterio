package com.zenith.app.ui.screens.timer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenith.app.domain.model.BgmTrack
import com.zenith.app.domain.model.PremiumFeature
import com.zenith.app.service.LockOverlayService
import com.zenith.app.service.TimerPhase
import com.zenith.app.ui.components.PremiumBadge
import com.zenith.app.ui.premium.PremiumUpsellDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    taskId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val bgmState by viewModel.bgmState.collectAsStateWithLifecycle()
    val selectedBgmTrack by viewModel.selectedBgmTrack.collectAsStateWithLifecycle()
    val bgmVolume by viewModel.bgmVolume.collectAsStateWithLifecycle()

    var showFocusModeWarning by remember { mutableStateOf(false) }
    var showBgmSelector by remember { mutableStateOf(false) }
    var showBgmPremiumUpsell by remember { mutableStateOf(false) }
    var showStrictModeBlockedDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showPremiumUpsellDialog by remember { mutableStateOf(false) }
    // セッションごとの完全ロックモード選択（設定のデフォルト値で初期化）
    var sessionLockModeEnabled by remember(uiState.settings.focusModeStrict) {
        mutableStateOf(uiState.settings.focusModeStrict)
    }
    // セッションごとのサイクル数選択（設定のデフォルト値で初期化）
    var sessionCycleCount by remember(uiState.totalCycles) {
        mutableStateOf(uiState.totalCycles)
    }

    // 完全ロックモードが有効でタイマー実行中は停止できない
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // フェーズ表示
            PhaseIndicator(
                phase = uiState.phase,
                currentCycle = uiState.currentCycle,
                totalCycles = uiState.totalCycles
            )

            // 円形タイマー
            CircularTimer(
                timeRemainingSeconds = uiState.timeRemainingSeconds,
                totalTimeSeconds = uiState.totalTimeSeconds,
                phase = uiState.phase
            )

            // コントロールボタン
            TimerControls(
                phase = uiState.phase,
                isRunning = uiState.isRunning,
                isPaused = uiState.isPaused,
                focusModeEnabled = uiState.settings.focusModeEnabled,
                isLockModeActive = isLockModeActive,
                onStart = {
                    if (uiState.settings.focusModeEnabled) {
                        showFocusModeWarning = true
                    } else {
                        viewModel.startTimer(lockModeEnabled = false)
                    }
                },
                onPause = { viewModel.pauseTimer() },
                onResume = { viewModel.resumeTimer() },
                onSkip = { viewModel.skipPhase() },
                onStop = { viewModel.showCancelDialog() }
            )

            // 学習時間
            if (uiState.totalWorkMinutes > 0) {
                Text(
                    text = "今回の学習: ${uiState.totalWorkMinutes}分",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // BGMボタン
            BgmButton(
                selectedTrack = selectedBgmTrack,
                isPlaying = bgmState.isPlaying,
                isPremium = isPremium,
                onClick = {
                    if (isPremium) {
                        showBgmSelector = true
                    } else {
                        showBgmPremiumUpsell = true
                    }
                },
                onTogglePlay = { viewModel.toggleBgm() }
            )
        }
    }

    // キャンセル確認ダイアログ
    if (uiState.showCancelDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCancelDialog() },
            title = { Text("タイマーを終了しますか？") },
            text = { Text("現在の進捗は記録されます。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelTimer(interrupted = true)
                        onNavigateBack()
                    }
                ) {
                    Text("終了する")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideCancelDialog() }) {
                    Text("続ける")
                }
            }
        )
    }

    // 完了ダイアログ
    if (uiState.showFinishDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideFinishDialog() },
            title = { Text("お疲れ様でした！") },
            text = {
                Column {
                    Text("${uiState.totalCycles}サイクル完了しました。")
                    Text("学習時間: ${uiState.totalWorkMinutes}分")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hideFinishDialog()
                        onNavigateBack()
                    }
                ) {
                    Text("終了")
                }
            }
        )
    }

    // フォーカスモード警告ダイアログ
    if (showFocusModeWarning) {
        AlertDialog(
            onDismissRequest = { showFocusModeWarning = false },
            icon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("フォーカスモードを開始しますか？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("フォーカスモードを有効にすると、タイマー中は他のアプリを使用できなくなります。")

                    // サイクル数選択
                    Column {
                        Text(
                            text = "サイクル数",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (sessionCycleCount > 1) sessionCycleCount-- }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "減らす")
                            }
                            Text(
                                text = "${sessionCycleCount}サイクル",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(
                                onClick = { if (sessionCycleCount < 10) sessionCycleCount++ }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "増やす")
                            }
                        }
                    }

                    HorizontalDivider()

                    // 完全ロックモード選択（Premium機能）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isPremium) {
                                    sessionLockModeEnabled = !sessionLockModeEnabled
                                } else {
                                    showFocusModeWarning = false
                                    showPremiumUpsellDialog = true
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = sessionLockModeEnabled && isPremium,
                            onCheckedChange = { checked ->
                                if (isPremium) {
                                    sessionLockModeEnabled = checked
                                } else {
                                    showFocusModeWarning = false
                                    showPremiumUpsellDialog = true
                                }
                            },
                            enabled = isPremium
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "完全ロックモード",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isPremium) MaterialTheme.colorScheme.onSurface
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!isPremium) {
                                    PremiumBadge()
                                }
                            }
                            Text(
                                text = "タイマー終了まで中断できません",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // オーバーレイ権限が必要な場合の警告
                    if (sessionLockModeEnabled && !LockOverlayService.canDrawOverlays(context)) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "権限が必要です",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "他のアプリの上に重ねて表示",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        LockOverlayService.requestOverlayPermission(context)
                                    }
                                ) {
                                    Text("設定")
                                }
                            }
                        }
                    }

                    if (!sessionLockModeEnabled) {
                        Text(
                            text = "※ 緊急時は停止ボタンで解除できます",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFocusModeWarning = false
                        viewModel.startTimer(
                            lockModeEnabled = sessionLockModeEnabled,
                            cycleCount = sessionCycleCount
                        )
                    }
                ) {
                    Text("開始する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFocusModeWarning = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 完全ロックモード中の終了ブロックダイアログ
    if (showStrictModeBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showStrictModeBlockedDialog = false },
            icon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("終了できません") },
            text = {
                Text("完全ロックモードが有効のため、タイマーが終了するまで中断できません。\n\n集中して作業を続けましょう！")
            },
            confirmButton = {
                TextButton(onClick = { showStrictModeBlockedDialog = false }) {
                    Text("わかりました")
                }
            }
        )
    }

    // Premium機能アップグレードダイアログ
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

    // BGM用Premium機能アップグレードダイアログ
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

    // BGM選択ボトムシート
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

@Composable
private fun PhaseIndicator(
    phase: TimerPhase,
    currentCycle: Int,
    totalCycles: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = when (phase) {
                TimerPhase.WORK -> "作業中"
                TimerPhase.SHORT_BREAK -> "休憩中"
                TimerPhase.LONG_BREAK -> "長休憩中"
                TimerPhase.IDLE -> "準備完了"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = when (phase) {
                TimerPhase.WORK -> MaterialTheme.colorScheme.primary
                TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
                TimerPhase.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        if (phase != TimerPhase.IDLE) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalCycles) { index ->
                    val isCompleted = index < currentCycle - 1
                    val isCurrent = index == currentCycle - 1
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(2.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = when {
                                    isCompleted -> Color(0xFF00838F)
                                    isCurrent -> Color(0xFF4DD0E1)
                                    else -> Color.Gray.copy(alpha = 0.3f)
                                }
                            )
                        }
                    }
                }
            }
            Text(
                text = "サイクル $currentCycle / $totalCycles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CircularTimer(
    timeRemainingSeconds: Int,
    totalTimeSeconds: Int,
    phase: TimerPhase
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

    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val topLeft = Offset(
                (size.width - radius * 2) / 2,
                (size.height - radius * 2) / 2
            )

            // 背景の円
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 進捗の円
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
                fontSize = 64.sp,
                fontWeight = FontWeight.Light
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TimerControls(
    phase: TimerPhase,
    isRunning: Boolean,
    isPaused: Boolean,
    focusModeEnabled: Boolean,
    isLockModeActive: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit
) {
    // 完全ロックモードが有効でタイマー実行中は停止できない
    val canStop = !isLockModeActive

    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            phase == TimerPhase.IDLE -> {
                // 開始ボタン
                FilledIconButton(
                    onClick = onStart,
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "開始",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            isRunning -> {
                // 停止ボタン（完全ロックモードでは無効化）
                OutlinedIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(56.dp),
                    enabled = canStop
                ) {
                    Icon(
                        if (canStop) Icons.Default.Stop else Icons.Default.Lock,
                        contentDescription = if (canStop) "停止" else "完全ロックモード中",
                        tint = if (canStop) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // 一時停止ボタン
                FilledIconButton(
                    onClick = onPause,
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = "一時停止",
                        modifier = Modifier.size(48.dp)
                    )
                }

                // スキップボタン（フォーカスモード時は非表示）
                if (!focusModeEnabled) {
                    OutlinedIconButton(
                        onClick = onSkip,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "スキップ")
                    }
                }
            }

            isPaused -> {
                // 停止ボタン（完全ロックモードでは無効化）
                OutlinedIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(56.dp),
                    enabled = canStop
                ) {
                    Icon(
                        if (canStop) Icons.Default.Stop else Icons.Default.Lock,
                        contentDescription = if (canStop) "停止" else "完全ロックモード中",
                        tint = if (canStop) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // 再開ボタン
                FilledIconButton(
                    onClick = onResume,
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "再開",
                        modifier = Modifier.size(48.dp)
                    )
                }

                // スキップボタン（フォーカスモード時は非表示）
                if (!focusModeEnabled) {
                    OutlinedIconButton(
                        onClick = onSkip,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "スキップ")
                    }
                }
            }
        }
    }
}

@Composable
private fun BgmButton(
    selectedTrack: BgmTrack?,
    isPlaying: Boolean,
    isPremium: Boolean,
    onClick: () -> Unit,
    onTogglePlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isPremium) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "BGM",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (!isPremium) {
                            PremiumBadge()
                        }
                    }
                    Text(
                        text = selectedTrack?.nameJa ?: "選択してください",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 再生/一時停止ボタン（トラックが選択されている場合のみ）
        if (selectedTrack != null && isPremium) {
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = onTogglePlay
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "一時停止" else "再生"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BgmSelectorBottomSheet(
    tracks: List<BgmTrack>,
    selectedTrack: BgmTrack?,
    volume: Float,
    onSelectTrack: (BgmTrack?) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ヘッダー
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "BGMを選択",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            // トラック選択
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // なし（BGMオフ）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTrack(null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioButton(
                        selected = selectedTrack == null,
                        onClick = { onSelectTrack(null) }
                    )
                    Text(
                        text = "なし",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // トラック一覧
                tracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrack(track) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RadioButton(
                            selected = selectedTrack?.id == track.id,
                            onClick = { onSelectTrack(track) }
                        )
                        Column {
                            Text(
                                text = track.nameJa,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = track.category.nameJa,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 音量スライダー
            if (selectedTrack != null) {
                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "音量",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeDown,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
