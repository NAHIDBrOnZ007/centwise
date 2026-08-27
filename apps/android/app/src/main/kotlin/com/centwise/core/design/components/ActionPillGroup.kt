package com.centwise.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors

@Composable
fun ActionPillGroup(
    onAddClick: () -> Unit,
    onSortClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val pillBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val accent = com.centwise.features.settings.AccentOptions.byName(com.centwise.features.settings.AppearancePrefs.accentName).color

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(pillBg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add",
            tint = accent,
            modifier = Modifier
                .size(18.dp)
                .clickable { onAddClick() }
        )
        Icon(
            imageVector = Icons.Default.SwapVert,
            contentDescription = "Sort",
            tint = accent,
            modifier = Modifier
                .size(18.dp)
                .clickable { onSortClick() }
        )
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Export",
            tint = accent,
            modifier = Modifier
                .size(18.dp)
                .clickable { onExportClick() }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ActionPillGroupPreview() {
    ActionPillGroup(
        onAddClick = {},
        onSortClick = {},
        onExportClick = {}
    )
}
