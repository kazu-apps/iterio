package com.zenith.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Premium機能のコンテンツにぼかし効果を適用し、
 * Premiumバッジをオーバーレイ表示するコンポーネント
 */
@Composable
fun BlurredPremiumContent(
    isPremium: Boolean,
    onPremiumClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // コンテンツ本体
        Box(
            modifier = Modifier
                .then(
                    if (!isPremium) {
                        Modifier.blur(8.dp)
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }

        // 非Premium時のオーバーレイ
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { onPremiumClick() },
                contentAlignment = Alignment.Center
            ) {
                PremiumBadge(
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
