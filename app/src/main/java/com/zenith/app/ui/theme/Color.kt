package com.zenith.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors - Teal
val Teal700 = Color(0xFF00838F)
val Teal800 = Color(0xFF00695C)
val AccentTeal = Color(0xFF4DD0E1)

// Background Colors
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val SurfaceVariantDark = Color(0xFF2D2D2D)

// Text Colors
val TextPrimary = Color(0xDEFFFFFF) // 87% white
val TextSecondary = Color(0x99FFFFFF) // 60% white
val TextDisabled = Color(0x61FFFFFF) // 38% white

// Semantic Colors
val AccentSuccess = Color(0xFF4CAF50)
val AccentWarning = Color(0xFFFF9800)
val AccentError = Color(0xFFE53935)

// Heatmap Colors (5 levels)
val HeatmapLevel0 = Color(0xFF2D2D2D) // No activity
val HeatmapLevel1 = Color(0xFF004D40) // Low
val HeatmapLevel2 = Color(0xFF00695C) // Medium-Low
val HeatmapLevel3 = Color(0xFF00838F) // Medium-High
val HeatmapLevel4 = Color(0xFF4DD0E1) // High

val HeatmapColors = listOf(
    HeatmapLevel0,
    HeatmapLevel1,
    HeatmapLevel2,
    HeatmapLevel3,
    HeatmapLevel4
)

// Subject Default Colors
val SubjectColors = listOf(
    Color(0xFF00838F), // Teal
    Color(0xFF1565C0), // Blue
    Color(0xFF7B1FA2), // Purple
    Color(0xFFC62828), // Red
    Color(0xFFEF6C00), // Orange
    Color(0xFF2E7D32), // Green
    Color(0xFF6D4C41), // Brown
    Color(0xFF455A64)  // Blue Grey
)
