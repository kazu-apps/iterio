package com.zenith.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import com.zenith.app.ui.theme.Teal700

@Composable
fun NotificationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "通知を有効にしますか？",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "ZENITHはタイマーの進行状況や復習リマインダーを通知でお知らせします。\n\n通知を有効にすることで、学習を中断せずに進捗を確認できます。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Teal700
                )
            ) {
                Text("有効にする")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("後で")
            }
        }
    )
}
