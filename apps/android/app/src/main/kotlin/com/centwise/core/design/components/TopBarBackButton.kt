package com.centwise.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors

/**
 * Standardized circular iOS-style Top Bar Back Button with soft capsule backdrop,
 * centered ArrowBack icon, and tactile spring bounce physics.
 */
@Composable
fun TopBarBackButton(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 20.dp,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val buttonBg = if (isDark) Color(0x22FFFFFF) else Color(0x0E000000)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(buttonBg)
            .iosBounceClick { onBackClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = textPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}
