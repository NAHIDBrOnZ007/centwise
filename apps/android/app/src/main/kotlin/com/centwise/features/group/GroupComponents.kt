package com.centwise.features.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

/**
 * Centwise-standard Group Type Icon Badge.
 * Replaces raw text emojis with a squircle-rounded container and dynamic theme accent vector icon.
 */
@Composable
fun GroupIconBadge(
    type: GroupType,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 20.dp,
    accent: Color = AccentOptions.byName(AppearancePrefs.accentName).color
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = type.vectorIcon,
            contentDescription = type.displayTitle,
            tint = accent,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Centwise-standard Expense Item Icon Badge.
 * Provides a mini squircle container with category-matched vector icon for each grocery/utility item.
 */
@Composable
fun ExpenseItemIconBadge(
    itemName: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    iconSize: Dp = 15.dp,
    accent: Color = AccentOptions.byName(AppearancePrefs.accentName).color
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = GroupItemIconHelper.iconForItem(itemName),
            contentDescription = itemName,
            tint = accent,
            modifier = Modifier.size(iconSize)
        )
    }
}
