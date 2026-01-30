package com.iterio.app.ui.screens.timer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.iterio.app.domain.model.AllowedApp
import com.iterio.app.ui.components.EmptyAppList
import com.iterio.app.ui.components.EmptySearchResult
import com.iterio.app.ui.theme.SurfaceDark
import com.iterio.app.ui.theme.SurfaceVariantDark
import com.iterio.app.ui.theme.Teal700
import com.iterio.app.ui.theme.TextPrimary
import com.iterio.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowedAppsSelectorBottomSheet(
    installedApps: List<AllowedApp>,
    selectedPackages: Set<String>,
    isLoading: Boolean,
    onSelectionChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var localSelectedPackages by remember(selectedPackages) { mutableStateOf(selectedPackages) }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onSelectionChanged(localSelectedPackages)
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // ヘッダー
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        tint = Teal700,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "許可アプリを選択",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Row {
                    TextButton(onClick = {
                        val newSet = localSelectedPackages + filteredApps.map { it.packageName }.toSet()
                        localSelectedPackages = newSet
                        onSelectionChanged(newSet)
                    }) {
                        Text("全選択", color = Teal700)
                    }
                    TextButton(onClick = {
                        val newSet = localSelectedPackages - filteredApps.map { it.packageName }.toSet()
                        localSelectedPackages = newSet
                        onSelectionChanged(newSet)
                    }) {
                        Text("全解除", color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 説明テキスト
            Text(
                text = "ロックモード中に使用を許可するアプリを選択",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "※ 完全ロックモード時は全てブロックされます",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 検索バー
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("アプリを検索", color = TextSecondary) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "クリア",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = Teal700,
                    unfocusedBorderColor = SurfaceVariantDark,
                    cursorColor = Teal700
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 選択状態サマリー
            Text(
                text = "${localSelectedPackages.size}個選択中",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // アプリリスト
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Teal700)
                }
            } else if (installedApps.isEmpty()) {
                // アプリ一覧が空の場合
                EmptyAppList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            } else if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
                // 検索結果が空の場合
                EmptySearchResult(
                    searchQuery = searchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppSelectorItem(
                            app = app,
                            isSelected = localSelectedPackages.contains(app.packageName),
                            onToggle = {
                                val newSet = if (localSelectedPackages.contains(app.packageName)) {
                                    localSelectedPackages - app.packageName
                                } else {
                                    localSelectedPackages + app.packageName
                                }
                                localSelectedPackages = newSet
                                onSelectionChanged(newSet)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSelectorItem(
    app: AllowedApp,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // アプリアイコン
        if (app.icon != null) {
            Image(
                bitmap = app.icon.toBitmap(40, 40).asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Icon(
                Icons.Default.Android,
                contentDescription = app.appName,
                modifier = Modifier.size(36.dp),
                tint = Teal700
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // アプリ名
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        // チェックボックス
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Teal700,
                uncheckedColor = TextSecondary,
                checkmarkColor = TextPrimary
            )
        )
    }
}
