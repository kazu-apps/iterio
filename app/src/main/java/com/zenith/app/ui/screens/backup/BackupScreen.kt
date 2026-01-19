package com.zenith.app.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenith.app.data.cloud.CloudBackupInfo
import com.zenith.app.data.cloud.GoogleSignInState
import com.zenith.app.ui.components.PremiumBadge
import com.zenith.app.ui.components.PremiumUpgradeCard
import com.zenith.app.ui.components.ZenithCard
import com.zenith.app.ui.theme.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val cloudBackupState by viewModel.cloudBackupState.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val googleSignInState by viewModel.googleSignInState.collectAsStateWithLifecycle()
    val cloudBackupInfo by viewModel.cloudBackupInfo.collectAsStateWithLifecycle()

    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showCloudDownloadConfirmDialog by remember { mutableStateOf(false) }

    // ファイル作成用ランチャー（エクスポート）
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    // ファイル選択用ランチャー（インポート）
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
            showImportConfirmDialog = true
        }
    }

    // Google Sign-Inランチャー
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result.data)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "バックアップ",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
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
            // Premium未加入時のアップセル
            if (!isPremium) {
                PremiumUpgradeCard(
                    onStartTrial = { viewModel.startTrial() },
                    onUpgrade = onNavigateToPremium,
                    trialAvailable = subscriptionStatus.canStartTrial
                )
            }

            // ローカルバックアップセクション
            Text(
                text = "ローカルバックアップ",
                style = MaterialTheme.typography.titleSmall,
                color = Teal700,
                fontWeight = FontWeight.SemiBold
            )

            // エクスポートセクション
            BackupSection(
                title = "エクスポート",
                icon = Icons.Default.Upload,
                description = "学習データをファイルに保存します。\n端末変更やバックアップに便利です。",
                buttonText = "エクスポート",
                buttonEnabled = isPremium && backupState !is BackupState.Exporting,
                isLoading = backupState is BackupState.Exporting,
                onClick = {
                    val fileName = generateBackupFileName()
                    exportLauncher.launch(fileName)
                }
            )

            // インポートセクション
            BackupSection(
                title = "インポート",
                icon = Icons.Default.Download,
                description = "バックアップファイルからデータを復元します。\n既存のデータは上書きされます。",
                buttonText = "インポート",
                buttonEnabled = isPremium && backupState !is BackupState.Importing,
                isLoading = backupState is BackupState.Importing,
                onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // クラウドバックアップセクション
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "クラウドバックアップ",
                    style = MaterialTheme.typography.titleSmall,
                    color = Teal700,
                    fontWeight = FontWeight.SemiBold
                )
                PremiumBadge()
            }

            CloudBackupSection(
                isPremium = isPremium,
                googleSignInState = googleSignInState,
                cloudBackupInfo = cloudBackupInfo,
                isUploading = cloudBackupState is CloudBackupState.Uploading,
                isDownloading = cloudBackupState is CloudBackupState.Downloading,
                onSignIn = {
                    googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                },
                onSignOut = {
                    viewModel.signOutGoogle()
                },
                onUpload = {
                    viewModel.uploadToCloud()
                },
                onDownload = {
                    showCloudDownloadConfirmDialog = true
                }
            )

            // 注意事項
            ZenithCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Teal700,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "注意事項",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "• インポート時、既存のデータは完全に上書きされます\n• バックアップファイルは安全な場所に保管してください\n• 他のアプリのデータはバックアップに含まれません",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    // インポート確認ダイアログ
    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmDialog = false
                pendingImportUri = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("データを上書きしますか？") },
            text = {
                Text(
                    "インポートすると、現在のすべての学習データが削除され、バックアップファイルの内容に置き換えられます。\n\nこの操作は取り消せません。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri?.let { viewModel.importBackup(it) }
                        showImportConfirmDialog = false
                        pendingImportUri = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("上書きする")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirmDialog = false
                        pendingImportUri = null
                    }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }

    // クラウドダウンロード確認ダイアログ
    if (showCloudDownloadConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showCloudDownloadConfirmDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("クラウドから復元しますか？") },
            text = {
                Text(
                    "復元すると、現在のすべての学習データが削除され、クラウドのバックアップ内容に置き換えられます。\n\nこの操作は取り消せません。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.downloadFromCloud()
                        showCloudDownloadConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("復元する")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCloudDownloadConfirmDialog = false
                    }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }

    // ローカルバックアップ結果表示
    when (val state = backupState) {
        is BackupState.ExportSuccess -> {
            ResultDialog(
                icon = Icons.Default.CheckCircle,
                iconColor = Teal700,
                title = "エクスポート完了",
                message = state.message,
                onDismiss = { viewModel.resetState() }
            )
        }
        is BackupState.ImportSuccess -> {
            ResultDialog(
                icon = Icons.Default.CheckCircle,
                iconColor = Teal700,
                title = "インポート完了",
                message = buildString {
                    append("データを復元しました\n\n")
                    append("• 教科グループ: ${state.result.groupsImported}件\n")
                    append("• タスク: ${state.result.tasksImported}件\n")
                    append("• 学習セッション: ${state.result.sessionsImported}件\n")
                    append("• 復習タスク: ${state.result.reviewTasksImported}件\n")
                    append("• 統計: ${state.result.statsImported}件")
                },
                onDismiss = { viewModel.resetState() }
            )
        }
        is BackupState.Error -> {
            ResultDialog(
                icon = Icons.Default.Error,
                iconColor = MaterialTheme.colorScheme.error,
                title = "エラー",
                message = state.message,
                onDismiss = { viewModel.resetState() }
            )
        }
        else -> { /* Idle, Exporting, Importing - no dialog */ }
    }

    // クラウドバックアップ結果表示
    when (val state = cloudBackupState) {
        is CloudBackupState.UploadSuccess -> {
            ResultDialog(
                icon = Icons.Default.CloudDone,
                iconColor = Teal700,
                title = "アップロード完了",
                message = "クラウドにバックアップしました",
                onDismiss = { viewModel.resetCloudState() }
            )
        }
        is CloudBackupState.DownloadSuccess -> {
            ResultDialog(
                icon = Icons.Default.CloudDone,
                iconColor = Teal700,
                title = "復元完了",
                message = buildString {
                    append("クラウドからデータを復元しました\n\n")
                    append("• 教科グループ: ${state.result.groupsImported}件\n")
                    append("• タスク: ${state.result.tasksImported}件\n")
                    append("• 学習セッション: ${state.result.sessionsImported}件\n")
                    append("• 復習タスク: ${state.result.reviewTasksImported}件\n")
                    append("• 統計: ${state.result.statsImported}件")
                },
                onDismiss = { viewModel.resetCloudState() }
            )
        }
        is CloudBackupState.Error -> {
            ResultDialog(
                icon = Icons.Default.Error,
                iconColor = MaterialTheme.colorScheme.error,
                title = "エラー",
                message = state.message,
                onDismiss = { viewModel.resetCloudState() }
            )
        }
        else -> { /* Idle, Uploading, Downloading - no dialog */ }
    }
}

@Composable
private fun BackupSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    buttonText: String,
    buttonEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    ZenithCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Teal700,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Button(
                onClick = onClick,
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Teal700,
                    disabledContainerColor = SurfaceVariantDark
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("処理中...")
                } else {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
private fun CloudBackupSection(
    isPremium: Boolean,
    googleSignInState: GoogleSignInState,
    cloudBackupInfo: CloudBackupInfo?,
    isUploading: Boolean,
    isDownloading: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onUpload: () -> Unit,
    onDownload: () -> Unit
) {
    ZenithCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Teal700,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Google Drive",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            when (googleSignInState) {
                is GoogleSignInState.SignedOut -> {
                    Text(
                        text = "Googleアカウントに接続すると、クラウドにバックアップを保存できます。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Button(
                        onClick = onSignIn,
                        enabled = isPremium,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Teal700,
                            disabledContainerColor = SurfaceVariantDark
                        )
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Googleアカウントに接続")
                    }
                }

                is GoogleSignInState.SignedIn -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "接続済み",
                                style = MaterialTheme.typography.bodySmall,
                                color = Teal700
                            )
                            Text(
                                text = googleSignInState.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        TextButton(onClick = onSignOut) {
                            Text("接続解除", color = TextSecondary)
                        }
                    }

                    // 最終バックアップ情報
                    if (cloudBackupInfo != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "最終バックアップ: ${formatTimestamp(cloudBackupInfo.modifiedTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // アップロード・ダウンロードボタン
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onUpload,
                            enabled = isPremium && !isUploading && !isDownloading,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Teal700,
                                disabledContainerColor = SurfaceVariantDark
                            )
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = TextPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isUploading) "..." else "保存")
                        }

                        OutlinedButton(
                            onClick = onDownload,
                            enabled = isPremium && !isUploading && !isDownloading && cloudBackupInfo != null,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Teal700
                            )
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Teal700,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isDownloading) "..." else "復元")
                        }
                    }
                }

                is GoogleSignInState.Error -> {
                    Text(
                        text = googleSignInState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    Button(
                        onClick = onSignIn,
                        enabled = isPremium,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Teal700,
                            disabledContainerColor = SurfaceVariantDark
                        )
                    ) {
                        Text("再接続")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultDialog(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

private fun generateBackupFileName(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    val timestamp = LocalDateTime.now().format(formatter)
    return "zenith_backup_$timestamp.json"
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
