package com.iterio.app.ui.screens.timer.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.stringResource
import com.iterio.app.R
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iterio.app.service.LockOverlayService
import com.iterio.app.service.TimerDefaults
import com.iterio.app.ui.components.PremiumBadge

@Composable
internal fun FocusModeWarningDialog(
    context: Context,
    isPremium: Boolean,
    sessionLockModeEnabled: Boolean,
    sessionCycleCount: Int,
    sessionAutoLoopEnabled: Boolean,
    sessionAllowedApps: Set<String>,
    onLockModeToggle: (Boolean) -> Unit,
    onCycleCountChange: (Int) -> Unit,
    onAutoLoopToggle: (Boolean) -> Unit,
    onShowAllowedApps: () -> Unit,
    onShowPremiumUpsell: () -> Unit,
    onShowAutoLoopPremiumUpsell: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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

                // Cycle count selection
                CycleCountSelector(
                    cycleCount = sessionCycleCount,
                    onCycleCountChange = onCycleCountChange
                )

                HorizontalDivider()

                // Complete lock mode (Premium feature)
                LockModeOption(
                    isPremium = isPremium,
                    isEnabled = sessionLockModeEnabled,
                    onToggle = { checked ->
                        if (isPremium) {
                            onLockModeToggle(checked)
                        } else {
                            onShowPremiumUpsell()
                        }
                    }
                )

                // Auto loop (Premium feature)
                AutoLoopOption(
                    isPremium = isPremium,
                    isEnabled = sessionAutoLoopEnabled,
                    onToggle = { checked ->
                        if (isPremium) {
                            onAutoLoopToggle(checked)
                        } else {
                            onShowAutoLoopPremiumUpsell()
                        }
                    }
                )

                // Allowed apps selection (only when not in lock mode)
                if (!sessionLockModeEnabled) {
                    HorizontalDivider()
                    AllowedAppsSelector(
                        selectedCount = sessionAllowedApps.size,
                        onClick = onShowAllowedApps
                    )
                }

                // Overlay permission warning
                if (sessionLockModeEnabled && !LockOverlayService.canDrawOverlays(context)) {
                    OverlayPermissionWarning(
                        onRequestPermission = {
                            LockOverlayService.requestOverlayPermission(context)
                        }
                    )
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
            TextButton(onClick = onConfirm) {
                Text("開始する")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
private fun CycleCountSelector(
    cycleCount: Int,
    onCycleCountChange: (Int) -> Unit
) {
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
                onClick = { if (cycleCount > TimerDefaults.MIN_CYCLES) onCycleCountChange(cycleCount - 1) }
            ) {
                Icon(Icons.Default.Remove, contentDescription = "減らす")
            }
            Text(
                text = "${cycleCount}サイクル",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            IconButton(
                onClick = { if (cycleCount < TimerDefaults.MAX_CYCLES) onCycleCountChange(cycleCount + 1) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "増やす")
            }
        }
    }
}

@Composable
private fun LockModeOption(
    isPremium: Boolean,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isEnabled) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = isEnabled && isPremium,
            onCheckedChange = onToggle,
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
}

@Composable
private fun AutoLoopOption(
    isPremium: Boolean,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isEnabled) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = isEnabled && isPremium,
            onCheckedChange = onToggle,
            enabled = isPremium
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "自動ループ",
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
                text = "サイクル完了後も自動で次のサイクルを開始",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AllowedAppsSelector(
    selectedCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = "許可アプリ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (selectedCount == 0) {
                        "ロックモード中に使用を許可するアプリ"
                    } else {
                        "${selectedCount}個選択中"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "選択",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OverlayPermissionWarning(
    onRequestPermission: () -> Unit
) {
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
            TextButton(onClick = onRequestPermission) {
                Text("設定")
            }
        }
    }
}

@Composable
internal fun StrictModeBlockedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = onDismiss) {
                Text("わかりました")
            }
        }
    )
}

@Composable
internal fun CancelConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("タイマーを終了しますか？") },
        text = { Text("現在の進捗は記録されます。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("終了する")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("続ける")
            }
        }
    )
}

@Composable
internal fun FinishDialog(
    totalCycles: Int,
    totalWorkMinutes: Int,
    nextTaskName: String? = null,
    allTasksCompleted: Boolean = false,
    onDismiss: () -> Unit,
    onNavigateToNextTask: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("お疲れ様でした！") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${totalCycles}サイクル完了しました。")
                Text("学習時間: ${totalWorkMinutes}分")
                if (allTasksCompleted) {
                    Text(
                        text = stringResource(R.string.timer_all_tasks_completed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (nextTaskName != null) {
                    Text(
                        text = "次のタスク: $nextTaskName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            if (nextTaskName != null && onNavigateToNextTask != null) {
                TextButton(onClick = onNavigateToNextTask) {
                    Text("次のタスクへ")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("終了")
                }
            }
        },
        dismissButton = if (nextTaskName != null && onNavigateToNextTask != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text("終了")
                }
            }
        } else {
            null
        }
    )
}
