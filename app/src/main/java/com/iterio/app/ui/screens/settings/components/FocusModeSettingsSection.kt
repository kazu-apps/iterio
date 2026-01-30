package com.iterio.app.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iterio.app.R
import com.iterio.app.ui.components.PremiumBadge
import com.iterio.app.ui.theme.Teal700
import com.iterio.app.ui.theme.TextPrimary
import com.iterio.app.ui.theme.TextSecondary

@Composable
internal fun FocusModeSettingsContent(
    focusModeEnabled: Boolean,
    focusModeStrict: Boolean,
    allowedAppsCount: Int,
    isPremium: Boolean,
    onFocusModeToggle: (Boolean) -> Unit,
    onStrictModeToggle: (Boolean) -> Unit,
    onNavigateToAllowedApps: () -> Unit,
    onPremiumClick: () -> Unit
) {
    SettingsSwitchItem(
        title = stringResource(R.string.settings_focus_mode_enable),
        description = stringResource(R.string.settings_focus_mode_enable_desc),
        checked = focusModeEnabled,
        onCheckedChange = onFocusModeToggle
    )

    if (focusModeEnabled) {
        FocusModeLevelSelector(
            isStrictMode = focusModeStrict,
            isPremium = isPremium,
            onModeChange = onStrictModeToggle,
            onPremiumClick = onPremiumClick
        )

        // Allowed apps settings
        AllowedAppsNavigationItem(
            allowedAppsCount = allowedAppsCount,
            onClick = onNavigateToAllowedApps
        )
    }
}

@Composable
private fun FocusModeLevelSelector(
    isStrictMode: Boolean,
    isPremium: Boolean,
    onModeChange: (Boolean) -> Unit,
    onPremiumClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.focus_mode_level_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Teal700,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Soft mode (App Restriction Mode)
        FocusModeLevelOption(
            title = stringResource(R.string.focus_mode_soft),
            description = stringResource(R.string.focus_mode_soft_desc),
            selected = !isStrictMode,
            enabled = true,
            showPremiumBadge = false,
            onClick = { onModeChange(false) }
        )

        // Hard mode (Complete Lock Mode)
        FocusModeLevelOption(
            title = stringResource(R.string.focus_mode_hard),
            description = stringResource(R.string.focus_mode_hard_desc),
            selected = isStrictMode && isPremium,
            enabled = isPremium,
            showPremiumBadge = !isPremium,
            onClick = {
                if (isPremium) {
                    onModeChange(true)
                } else {
                    onPremiumClick()
                }
            }
        )
    }
}

@Composable
private fun FocusModeLevelOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    showPremiumBadge: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Teal700,
                unselectedColor = TextSecondary,
                disabledSelectedColor = TextSecondary,
                disabledUnselectedColor = TextSecondary
            ),
            enabled = enabled
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) TextPrimary else TextSecondary
                )
                if (showPremiumBadge) {
                    PremiumBadge()
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AllowedAppsNavigationItem(
    allowedAppsCount: Int,
    onClick: () -> Unit
) {
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
                Icons.Default.Apps,
                contentDescription = null,
                tint = Teal700,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = stringResource(R.string.settings_allowed_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = if (allowedAppsCount > 0) {
                        stringResource(R.string.settings_allowed_apps_selected, allowedAppsCount)
                    } else {
                        stringResource(R.string.settings_allowed_apps_desc)
                    },
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
