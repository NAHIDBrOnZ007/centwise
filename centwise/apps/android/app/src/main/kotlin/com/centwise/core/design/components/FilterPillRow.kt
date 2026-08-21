package com.centwise.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography

@Composable
fun FilterPillRow(
    selectedPeriod: String = "All Time",
    selectedType: String = "Type",
    selectedCategory: String = "Category",
    onPeriodClick: () -> Unit = {},
    onTypeClick: () -> Unit = {},
    onCategoryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Period Pill (Active Mauve)
        FilterCapsulePill(
            icon = Icons.Default.CalendarToday,
            text = selectedPeriod,
            isActive = true,
            onClick = onPeriodClick,
            isDark = isDark
        )

        // 2. Type Pill
        FilterCapsulePill(
            icon = Icons.Default.Menu,
            text = selectedType,
            isActive = false,
            onClick = onTypeClick,
            isDark = isDark
        )

        // 3. Category Pill
        FilterCapsulePill(
            icon = Icons.Default.Tune,
            text = selectedCategory,
            isActive = false,
            onClick = onCategoryClick,
            isDark = isDark
        )
    }
}

@Composable
fun FilterCapsulePill(
    icon: ImageVector,
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val bg = if (isActive) {
        CentwiseColors.AccentMauve
    } else {
        if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg
    }
    val contentColor = if (isActive) {
        Color.White
    } else {
        if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = CentwiseTypography.Subheadline,
            color = contentColor
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FilterPillRowPreview() {
    FilterPillRow()
}
